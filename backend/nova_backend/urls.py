from django.contrib import admin
from django.http import JsonResponse
from django.urls import include, path
from rest_framework.permissions import AllowAny
from rest_framework.views import APIView


class HealthView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def get(self, request):
        return JsonResponse({"status": "ok", "service": "nova-api"})


urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/v1/health/", HealthView.as_view(), name="health"),
    path("api/v1/", include("accounts.urls")),
]
