from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView

from .serializers import (
    NovaTokenObtainPairSerializer,
    RegisterSerializer,
    UserSerializer,
)


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
