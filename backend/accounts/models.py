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
