from django.contrib.auth import get_user_model
from django.db.models import Q
from django.shortcuts import get_object_or_404
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView

from .models import Follow
from .serializers import (
    NovaTokenObtainPairSerializer,
    PersonSerializer,
    RegisterSerializer,
    UserSerializer,
)

User = get_user_model()


class RegisterView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        refresh = RefreshToken.for_user(user)
        return Response(
            {
                "access": str(refresh.access_token),
                "refresh": str(refresh),
                "user": UserSerializer(user, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class NovaTokenObtainPairView(TokenObtainPairView):
    permission_classes = [AllowAny]
    authentication_classes = []
    serializer_class = NovaTokenObtainPairSerializer


class MeView(generics.RetrieveUpdateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = UserSerializer

    def get_object(self):
        return self.request.user


class PeopleView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.query_params.get("q", "").strip()
        people = User.objects.filter(is_active=True).exclude(pk=request.user.pk)

        if query:
            people = people.filter(
                Q(username__icontains=query) | Q(name__icontains=query)
            )

        people = people.order_by("username")[:50]
        serializer = PersonSerializer(
            people,
            many=True,
            context={"request": request},
        )
        return Response({"results": serializer.data})


class PersonView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.lower(),
        )
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )


class FollowView(APIView):
    permission_classes = [IsAuthenticated]

    def _person(self, username):
        return get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.lower(),
        )

    def post(self, request, username):
        person = self._person(username)
        if person.pk == request.user.pk:
            return Response(
                {"detail": "You can't follow yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        Follow.objects.get_or_create(follower=request.user, following=person)
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )

    def delete(self, request, username):
        person = self._person(username)
        Follow.objects.filter(follower=request.user, following=person).delete()
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )
