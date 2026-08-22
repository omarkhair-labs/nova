from rest_framework import generics
from rest_framework.permissions import IsAuthenticated

from .serializers import UserSerializer


class MeView(generics.RetrieveUpdateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = UserSerializer

    def get_object(self):
        return self.request.user
