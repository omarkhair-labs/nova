from django.contrib.auth.base_user import BaseUserManager
from django.contrib.auth.models import AbstractUser
from django.core.validators import RegexValidator
from django.db import models


username_validator = RegexValidator(
    regex=r"^[a-z0-9_.]+$",
    message="Username can only contain lowercase letters, numbers, underscores, and dots.",
)


class UserManager(BaseUserManager):
    use_in_migrations = True

    def create_user(self, email, username, password=None, **extra_fields):
        if not email:
            raise ValueError("Email is required")
        if not username:
            raise ValueError("Username is required")

        email = self.normalize_email(email)
        username = username.strip().lower()
        user = self.model(email=email, username=username, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, username, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        extra_fields.setdefault("is_active", True)

        if extra_fields.get("is_staff") is not True:
            raise ValueError("Superuser must have is_staff=True")
        if extra_fields.get("is_superuser") is not True:
            raise ValueError("Superuser must have is_superuser=True")

        return self.create_user(email, username, password, **extra_fields)


class User(AbstractUser):
    email = models.EmailField(unique=True)
    username = models.CharField(
        max_length=30,
        unique=True,
        validators=[username_validator],
    )
    name = models.CharField(max_length=80, blank=True)
    avatar = models.ImageField(upload_to="avatars/%Y/%m/", blank=True)

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = ["username"]

    objects = UserManager()

    def save(self, *args, **kwargs):
        self.email = self.__class__.objects.normalize_email(self.email)
        self.username = self.username.strip().lower()
        super().save(*args, **kwargs)

    def __str__(self):
        return self.email


class Follow(models.Model):
    follower = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="following_relationships",
    )
    following = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="follower_relationships",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at",)
        constraints = [
            models.UniqueConstraint(
                fields=("follower", "following"),
                name="unique_follow_relationship",
            ),
            models.CheckConstraint(
                condition=~models.Q(follower=models.F("following")),
                name="prevent_self_follow",
            ),
        ]

    def __str__(self):
        return f"{self.follower.username} -> {self.following.username}"


class Post(models.Model):
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="posts",
    )
    image = models.ImageField(upload_to="posts/%Y/%m/")
    caption = models.CharField(max_length=500, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at", "-id")
        indexes = [
            models.Index(fields=("-created_at",), name="post_created_idx"),
            models.Index(fields=("author", "-created_at"), name="post_author_created_idx"),
        ]

    def delete(self, *args, **kwargs):
        image_name = self.image.name
        storage = self.image.storage
        result = super().delete(*args, **kwargs)
        if image_name:
            storage.delete(image_name)
        return result

    def __str__(self):
        return f"Post {self.pk} by @{self.author.username}"


class Like(models.Model):
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="likes",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_likes",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("post", "user"),
                name="unique_post_like",
            )
        ]
        indexes = [
            models.Index(fields=("post", "-created_at"), name="like_post_created_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} likes post {self.post_id}"


class Comment(models.Model):
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="comments",
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_comments",
    )
    body = models.CharField(max_length=300)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("created_at", "id")
        indexes = [
            models.Index(fields=("post", "created_at"), name="comment_post_created_idx"),
        ]

    def __str__(self):
        return f"Comment {self.pk} by @{self.author.username}"


class Notification(models.Model):
    class Kind(models.TextChoices):
        FOLLOW = "follow", "Follow"
        LIKE = "like", "Like"
        COMMENT = "comment", "Comment"

    recipient = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="notifications",
    )
    actor = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="notification_actions",
    )
    kind = models.CharField(max_length=16, choices=Kind.choices)
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="notifications",
        null=True,
        blank=True,
    )
    comment = models.ForeignKey(
        Comment,
        on_delete=models.CASCADE,
        related_name="notifications",
        null=True,
        blank=True,
    )
    dedupe_key = models.CharField(max_length=100, unique=True)
    created_at = models.DateTimeField(auto_now_add=True)
    read_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ("-created_at", "-id")
        indexes = [
            models.Index(fields=("recipient", "-created_at"), name="notif_recipient_created_idx"),
            models.Index(fields=("recipient", "read_at"), name="notif_recipient_read_idx"),
        ]

    def __str__(self):
        return f"{self.kind} from @{self.actor.username} to @{self.recipient.username}"


class DevicePushToken(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="push_tokens",
    )
    token = models.CharField(max_length=512, unique=True)
    platform = models.CharField(max_length=16, default="android")
    active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ("-updated_at", "-id")
        indexes = [
            models.Index(fields=("user", "active"), name="push_user_active_idx"),
        ]

    def __str__(self):
        return f"{self.platform} push token for @{self.user.username}"
