from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Follow, Notification
from .privacy import is_private_account
from .privacy_models import FollowRequest
from .serializers import PersonSerializer
from .trust_safety import active_person_for
from ..views import create_notification, public_post_queryset


class PersonView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = active_person_for(request.user, username)
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )


class FollowView(APIView):
    permission_classes = [IsAuthenticated]

    def _person(self, request, username):
        return active_person_for(request.user, username)

    def post(self, request, username):
        person = self._person(request, username)
        if person.pk == request.user.pk:
            return Response(
                {"detail": "You can't follow yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        existing = Follow.objects.filter(follower=request.user, following=person).exists()
        if not existing and is_private_account(person):
            FollowRequest.objects.get_or_create(requester=request.user, target=person)
            return Response(
                PersonSerializer(person, context={"request": request}).data,
                status=status.HTTP_202_ACCEPTED,
            )

        FollowRequest.objects.filter(requester=request.user, target=person).delete()
        _, created = Follow.objects.get_or_create(
            follower=request.user,
            following=person,
        )
        if created:
            create_notification(
                recipient=person,
                actor=request.user,
                kind=Notification.Kind.FOLLOW,
                dedupe_key=f"follow:{request.user.pk}:{person.pk}",
            )

        return Response(
            PersonSerializer(person, context={"request": request}).data
        )

    def delete(self, request, username):
        person = self._person(request, username)
        Follow.objects.filter(follower=request.user, following=person).delete()
        FollowRequest.objects.filter(requester=request.user, target=person).delete()
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )
