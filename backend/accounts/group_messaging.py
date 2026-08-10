from django.db import transaction
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_models import ConversationPreference, GroupMembership, GroupReadState
from .messaging_serializers import ConversationSerializer
from .models import Conversation, User
from .serializers import PostAuthorSerializer
from .trust_safety import blocked_user_ids, users_blocked

MAX_GROUP_MEMBERS = 50
MIN_GROUP_MEMBERS = 3


def group_membership(conversation, user):
    return GroupMembership.objects.filter(
        conversation=conversation,
        user=user,
    ).select_related("user").first()


def group_conversation_for(user, conversation_id):
    return get_object_or_404(
        Conversation.objects.filter(
            pk=conversation_id,
            kind=Conversation.Kind.GROUP,
            group_memberships__user=user,
        ).distinct(),
    )


def serialize_group_detail(request, conversation):
    hidden_ids = blocked_user_ids(request.user)
    memberships = list(
        conversation.group_memberships.select_related("user")
        .filter(user__is_active=True)
        .exclude(user_id__in=hidden_ids)
        .order_by("joined_at", "id")
    )
    return {
        "conversation": ConversationSerializer(
            conversation,
            context={"request": request},
        ).data,
        "members": [
            {
                "user": PostAuthorSerializer(
                    membership.user,
                    context={"request": request},
                ).data,
                "role": membership.role,
                "joined_at": membership.joined_at.isoformat(),
            }
            for membership in memberships
        ],
    }


def remove_user_from_all_groups(user):
    """Remove a departing account from groups without leaving ownerless rooms."""
    memberships = list(
        GroupMembership.objects.select_related("conversation")
        .filter(user=user)
        .order_by("conversation_id")
    )
    for membership in memberships:
        conversation = membership.conversation
        if conversation.kind != Conversation.Kind.GROUP:
            continue

        was_owner = membership.role == GroupMembership.Role.OWNER
        membership.delete()
        GroupReadState.objects.filter(conversation=conversation, user=user).delete()
        ConversationPreference.objects.filter(conversation=conversation, user=user).delete()

        remaining = list(
            GroupMembership.objects.select_for_update()
            .filter(conversation=conversation)
            .order_by("joined_at", "id")
        )
        if not remaining:
            conversation.delete()
            continue

        if was_owner:
            successor = next(
                (
                    item
                    for item in remaining
                    if item.role == GroupMembership.Role.ADMIN
                ),
                remaining[0],
            )
            successor.role = GroupMembership.Role.OWNER
            successor.save(update_fields=("role",))

    Conversation.objects.filter(
        kind=Conversation.Kind.GROUP,
        created_by=user,
    ).update(created_by=None)


def _clean_title(raw):
    return str(raw or "").strip()


def _clean_usernames(raw):
    if not isinstance(raw, list):
        return None
    cleaned = []
    seen = set()
    for value in raw:
        username = str(value or "").strip().lower()
        if not username or username in seen:
            continue
        seen.add(username)
        cleaned.append(username)
    return cleaned


class GroupConversationCreateView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        title = _clean_title(request.data.get("title"))
        usernames = _clean_usernames(request.data.get("usernames"))
        if not title:
            return Response(
                {"detail": "Group name is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(title) > 80:
            return Response(
                {"detail": "Group name must be 80 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if usernames is None:
            return Response(
                {"detail": "Choose people for the group."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        usernames = [username for username in usernames if username != request.user.username]
        if len(usernames) < MIN_GROUP_MEMBERS - 1:
            return Response(
                {"detail": "A group needs at least 3 people including you."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(usernames) > MAX_GROUP_MEMBERS - 1:
            return Response(
                {"detail": f"Groups can have up to {MAX_GROUP_MEMBERS} members."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        people = list(
            User.objects.filter(is_active=True, username__in=usernames).order_by("username")
        )
        found = {person.username for person in people}
        missing = [username for username in usernames if username not in found]
        if missing:
            return Response(
                {"detail": f"Nova couldn't find @{missing[0]}."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        for person in people:
            if users_blocked(request.user, person):
                return Response(
                    {"detail": f"You can't add @{person.username} to this group."},
                    status=status.HTTP_403_FORBIDDEN,
                )

        with transaction.atomic():
            conversation = Conversation.objects.create(
                kind=Conversation.Kind.GROUP,
                title=title,
                created_by=request.user,
            )
            memberships = [
                GroupMembership(
                    conversation=conversation,
                    user=request.user,
                    role=GroupMembership.Role.OWNER,
                ),
                *[
                    GroupMembership(
                        conversation=conversation,
                        user=person,
                        role=GroupMembership.Role.MEMBER,
                    )
                    for person in people
                ],
            ]
            GroupMembership.objects.bulk_create(memberships)
            GroupReadState.objects.bulk_create(
                [
                    GroupReadState(conversation=conversation, user=membership.user)
                    for membership in memberships
                ]
            )

        return Response(
            ConversationSerializer(conversation, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class GroupConversationDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        return Response(serialize_group_detail(request, conversation))

    def patch(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        membership = group_membership(conversation, request.user)
        if membership is None or membership.role not in {
            GroupMembership.Role.OWNER,
            GroupMembership.Role.ADMIN,
        }:
            return Response(
                {"detail": "Only group admins can edit the group."},
                status=status.HTTP_403_FORBIDDEN,
            )
        title = _clean_title(request.data.get("title"))
        if not title:
            return Response(
                {"detail": "Group name is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(title) > 80:
            return Response(
                {"detail": "Group name must be 80 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        conversation.title = title
        conversation.save(update_fields=("title", "updated_at"))
        return Response(serialize_group_detail(request, conversation))

    def delete(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        membership = group_membership(conversation, request.user)
        if membership is None or membership.role != GroupMembership.Role.OWNER:
            return Response(
                {"detail": "Only the group owner can delete the group."},
                status=status.HTTP_403_FORBIDDEN,
            )
        conversation.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class GroupMembersView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        membership = group_membership(conversation, request.user)
        if membership is None or membership.role not in {
            GroupMembership.Role.OWNER,
            GroupMembership.Role.ADMIN,
        }:
            return Response(
                {"detail": "Only group admins can add people."},
                status=status.HTTP_403_FORBIDDEN,
            )

        usernames = _clean_usernames(request.data.get("usernames"))
        if usernames is None or not usernames:
            return Response(
                {"detail": "Choose at least one person to add."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        existing_ids = set(
            conversation.group_memberships.values_list("user_id", flat=True)
        )
        current_count = len(existing_ids)
        people = list(User.objects.filter(is_active=True, username__in=usernames))
        found = {person.username for person in people}
        missing = [username for username in usernames if username not in found]
        if missing:
            return Response(
                {"detail": f"Nova couldn't find @{missing[0]}."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        people = [person for person in people if person.pk not in existing_ids]
        if current_count + len(people) > MAX_GROUP_MEMBERS:
            return Response(
                {"detail": f"Groups can have up to {MAX_GROUP_MEMBERS} members."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        for person in people:
            if users_blocked(request.user, person):
                return Response(
                    {"detail": f"You can't add @{person.username} to this group."},
                    status=status.HTTP_403_FORBIDDEN,
                )

        latest_message = conversation.messages.order_by("-id").first()
        with transaction.atomic():
            for person in people:
                GroupMembership.objects.create(
                    conversation=conversation,
                    user=person,
                    role=GroupMembership.Role.MEMBER,
                )
                GroupReadState.objects.create(
                    conversation=conversation,
                    user=person,
                    last_read_message=latest_message,
                )

        return Response(serialize_group_detail(request, conversation))


class GroupMemberDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, conversation_id, username):
        conversation = group_conversation_for(request.user, conversation_id)
        actor_membership = group_membership(conversation, request.user)
        target = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.strip().lower(),
        )
        target_membership = group_membership(conversation, target)
        if target_membership is None:
            return Response(status=status.HTTP_404_NOT_FOUND)

        removing_self = target.pk == request.user.pk
        if not removing_self:
            if actor_membership is None or actor_membership.role not in {
                GroupMembership.Role.OWNER,
                GroupMembership.Role.ADMIN,
            }:
                return Response(
                    {"detail": "Only group admins can remove people."},
                    status=status.HTTP_403_FORBIDDEN,
                )
            if target_membership.role == GroupMembership.Role.OWNER:
                return Response(
                    {"detail": "The group owner can't be removed."},
                    status=status.HTTP_403_FORBIDDEN,
                )
            if (
                actor_membership.role == GroupMembership.Role.ADMIN
                and target_membership.role == GroupMembership.Role.ADMIN
            ):
                return Response(
                    {"detail": "Only the owner can remove another admin."},
                    status=status.HTTP_403_FORBIDDEN,
                )

        with transaction.atomic():
            owner_leaving = (
                removing_self
                and target_membership.role == GroupMembership.Role.OWNER
            )
            target_membership.delete()
            GroupReadState.objects.filter(
                conversation=conversation,
                user=target,
            ).delete()
            ConversationPreference.objects.filter(
                conversation=conversation,
                user=target,
            ).delete()

            remaining = list(
                conversation.group_memberships.select_for_update()
                .order_by("joined_at", "id")
            )
            if not remaining:
                conversation.delete()
                return Response({"deleted": True, "left": removing_self})

            if owner_leaving:
                successor = next(
                    (
                        item
                        for item in remaining
                        if item.role == GroupMembership.Role.ADMIN
                    ),
                    remaining[0],
                )
                successor.role = GroupMembership.Role.OWNER
                successor.save(update_fields=("role",))

        return Response(
            {
                "deleted": False,
                "left": removing_self,
                **serialize_group_detail(request, conversation),
            }
        )
