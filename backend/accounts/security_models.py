from django.db import models

from .models import User


class UserSecurityState(models.Model):
    """Small revocation counter for JWT sessions.

    Users created before V10 intentionally have no row and therefore use
    version 0. The first password/security invalidation creates the row and
    increments it, which revokes every token issued with the previous version.
    """

    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name="security_state",
    )
    token_version = models.PositiveBigIntegerField(default=0)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"

    def __str__(self):
        return f"Security state for @{self.user.username}: v{self.token_version}"


class PasswordResetChallenge(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="password_reset_challenges",
    )
    code_hash = models.CharField(max_length=128)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()
    consumed_at = models.DateTimeField(null=True, blank=True)
    attempts = models.PositiveSmallIntegerField(default=0)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        indexes = [
            models.Index(
                fields=("user", "-created_at"),
                name="pwd_reset_user_created_idx",
            ),
            models.Index(
                fields=("expires_at", "consumed_at"),
                name="pwd_reset_expiry_idx",
            ),
        ]

    def __str__(self):
        return f"Password reset challenge {self.pk} for @{self.user.username}"
