from django.db import transaction
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Follow, User
from .privacy import privacy_payload, visible_close_friend_ids
from .privacy_models import AccountPrivacy, CloseFriend, FollowRequest
from .serializers import PersonSerializer
from .trust_safety import active_person_for, blocked_user_ids, users_blocked


class AccountPrivacyView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(privacy_payload(request.user))

    def post(self, request):
        raw = request.data.get("is_private")
        if not isinstance(raw, bool):
            return Response(
                {"detail": "is_private must be true or false."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        with transaction.atomic():
            privacy, _ = AccountPrivacy.objects.select_for_update().get_or_create(
                user=request.user,
            )
            was_private = privacy.is_private
            privacy.is_private = raw
            privacy.save(update_fields=("is_private", "updated_at"))

            accepted = 0
            if was_private and not raw:
                blocked_ids = blocked_user_ids(request.user)
                pending = list(
                    FollowRequest.objects.select_related("requester")
                    .filter(target=request.user, requester__is_active=True)
                    .exclude(requester_id__in=blocked_ids)
                )
                Follow.objects.bulk_create(
                    [
                        Follow(follower=item.requester, following=request.user)
                        for item in pending
                    ],
                    ignore_conflicts=True,
                )
                accepted = len(pending)
                FollowRequest.objects.filter(target=request.user).delete()

        payload = privacy_payload(request.user)
        payload["accepted_pending_requests"] = accepted
        return Response(payload)


class FollowRequestsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        blocked_ids = blocked_user_ids(request.user)
        requests = list(
            FollowRequest.objects.select_related("requester", "requester__account_privacy")
            .filter(target=request.user, requester__is_active=True)
            .exclude(requester_id__in=blocked_ids)
            .order_by("-created_at", "-id")[:250]
        )
        return Response(
            {
                "results": [
                    {
                        "id": item.pk,
                        "requester": PersonSerializer(
                            item.requester,
                            context={"request": request},
                        ).data,
                        "created_at": item.created_at.isoformat(),
                    }
                    for item in requests
                ],
                "count": len(requests),
            }
        )


class FollowRequestAcceptView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, request_id):
        follow_request = get_object_or_404(
            FollowRequest.objects.select_related("requester"),
            pk=request_id,
            target=request.user,
        )
        requester = follow_request.requester
        if not requester.is_active or users_blocked(request.user, requester):
            follow_request.delete()
            return Response(
                {"detail": "That follow request is no longer available."},
                status=status.HTTP_404_NOT_FOUND,
            )

        with transaction.atomic():
            Follow.objects.get_or_create(
                follower=requester,
                following=request.user,
            )
            FollowRequest.objects.filter(pk=follow_request.pk).delete()

        return Response(
            {
                "accepted": True,
                "requester": PersonSerializer(
                    requester,
                    context={"request": request},
                ).data,
            }
        )


class FollowRequestDeclineView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, request_id):
        follow_request = get_object_or_404(
            FollowRequest,
            pk=request_id,
            target=request.user,
        )
        follow_request.delete()
        return Response({"declined": True})


class CloseFriendsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        member_ids = list(visible_close_friend_ids(request.user))
        people = list(
            User.objects.filter(pk__in=member_ids, is_active=True)
            .select_related("account_privacy")
            .order_by("username", "id")
        )
        return Response(
            {
                "results": PersonSerializer(
                    people,
                    many=True,
                    context={"request": request},
                ).data,
                "count": len(people),
            }
        )

    def post(self, request):
        username = str(request.data.get("username") or "").strip().lower()
        if not username:
            return Response(
                {"detail": "Choose a follower to add."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        member = active_person_for(request.user, username)
        if member.pk == request.user.pk:
            return Response(
                {"detail": "You are already part of your own Stories."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not Follow.objects.filter(follower=member, following=request.user).exists():
            return Response(
                {"detail": "Only your followers can be added to Close Friends."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        _, created = CloseFriend.objects.get_or_create(owner=request.user, member=member)
        return Response(
            {
                "added": True,
                "person": PersonSerializer(member, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )


class CloseFriendDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, username):
        member = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.strip().lower(),
        )
        removed, _ = CloseFriend.objects.filter(owner=request.user, member=member).delete()
        return Response({"removed": bool(removed)})
