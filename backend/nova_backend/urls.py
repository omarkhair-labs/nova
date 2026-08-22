from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.http import JsonResponse
from django.urls import include, path
from django.views.generic import TemplateView
from rest_framework.permissions import AllowAny
from rest_framework.views import APIView


class HealthView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def get(self, request):
        return JsonResponse({"status": "ok", "service": "nova-api"})


urlpatterns = [
    path("admin/", admin.site.urls),
    path(
        "privacy/",
        TemplateView.as_view(template_name="accounts/privacy_policy.html"),
        name="privacy-policy",
    ),
    path(
        "account-deletion/",
        TemplateView.as_view(template_name="accounts/account_deletion.html"),
        name="account-deletion",
    ),
    path(
        "child-safety/",
        TemplateView.as_view(template_name="accounts/child_safety.html"),
        name="child-safety",
    ),
    path("api/v1/health/", HealthView.as_view(), name="health"),
    path("api/v1/", include("accounts.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
