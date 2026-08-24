from django.db import models

from .models import User


class AuthSessionRecord(models.Model):
    session_key = models.CharField(max_length=64, unique=True)
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="auth_sessions",
    )
    device_name = models.CharField(max_length=120, blank=True)
    platform = models.CharField(max_length=24, blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    user_agent = models.CharField(max_length=300, blank=True)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    last_seen_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-last_seen_at", "-id")
        indexes = [
            models.Index(fields=("user", "is_active", "-last_seen_at"), name="auth_session_user_active_idx"),
        ]
