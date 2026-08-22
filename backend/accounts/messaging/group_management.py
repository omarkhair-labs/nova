from PIL import Image, UnidentifiedImageError
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .group_messaging import (
    group_conversation_for,
    group_membership,
    serialize_group_detail,
)
from .messaging_models import (
    GroupConversationProfile,
    GroupMembership,
    group_avatar_url,
)
from .models import User

MAX_GROUP_AVATAR_BYTES = 10 * 1024 * 1024


def serialize_managed_group_detail(request, conversation):
    data = serialize_group_detail(request, conversation)
    conversation_data = dict(data["conversation"])
    conversation_data["group_avatar_url"] = group_avatar_url(request, conversation)
    return {
        **data,
        "conversation": conversation_data,
    }


def _clean_title(raw):
    return str(raw or "").strip()


def _truthy(raw):
    return str(raw or "").strip().lower() in {"1", "true", "yes", "on"}


def _validate_avatar(upload):
    if upload.size > MAX_GROUP_AVATAR_BYTES:
        return "Group photo must be 10 MB or smaller."
    content_type = str(getattr(upload, "content_type", "") or "").lower()
    if content_type and not content_type.startswith("image/"):
        return "Choose an image for the group photo."
    try:
        Image.open(upload).verify()
        upload.seek(0)
    except (UnidentifiedImageError, OSError, ValueError):
        return "Nova couldn't read that image. Choose another photo."
    return None


class GroupManagementDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        return Response(serialize_managed_group_detail(request, conversation))

    def post(self, request, conversation_id):
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

        changed = False
        if "title" in request.data:
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
            if title != conversation.title:
                conversation.title = title
                conversation.save(update_fields=("title", "updated_at"))
                changed = True

        avatar = request.FILES.get("avatar")
        remove_avatar = _truthy(request.data.get("remove_avatar"))
        if avatar is not None and remove_avatar:
            return Response(
                {"detail": "Choose a new group photo or remove the current one, not both."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if avatar is not None:
            avatar_error = _validate_avatar(avatar)
            if avatar_error:
                return Response(
                    {"detail": avatar_error},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            profile, _ = GroupConversationProfile.objects.get_or_create(
                conversation=conversation,
            )
            profile.avatar = avatar
            profile.save(update_fields=("avatar", "updated_at"))
            changed = True
        elif remove_avatar:
            profile = GroupConversationProfile.objects.filter(
                conversation=conversation,
            ).first()
            if profile is not None and profile.avatar:
                profile.avatar = ""
                profile.save(update_fields=("avatar", "updated_at"))
                changed = True

        if not changed and "title" not in request.data and avatar is None and not remove_avatar:
            return Response(
                {"detail": "Nothing to update."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response(serialize_managed_group_detail(request, conversation))


class GroupMemberRoleView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id, username):
        conversation = group_conversation_for(request.user, conversation_id)
        actor_membership = group_membership(conversation, request.user)
        if actor_membership is None or actor_membership.role != GroupMembership.Role.OWNER:
            return Response(
                {"detail": "Only the group owner can manage admins."},
                status=status.HTTP_403_FORBIDDEN,
            )

        target = User.objects.filter(
            is_active=True,
            username=str(username or "").strip().lower(),
        ).first()
        if target is None:
            return Response(status=status.HTTP_404_NOT_FOUND)
        target_membership = group_membership(conversation, target)
        if target_membership is None:
            return Response(status=status.HTTP_404_NOT_FOUND)
        if target_membership.role == GroupMembership.Role.OWNER:
            return Response(
                {"detail": "The group owner role can't be changed here."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        requested_role = str(request.data.get("role") or "").strip().lower()
        if requested_role not in {
            GroupMembership.Role.ADMIN,
            GroupMembership.Role.MEMBER,
        }:
            return Response(
                {"detail": "Role must be admin or member."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if target_membership.role != requested_role:
            target_membership.role = requested_role
            target_membership.save(update_fields=("role",))

        return Response(serialize_managed_group_detail(request, conversation))
