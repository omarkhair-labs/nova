from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from .models import Comment, Follow, Like, Notification, Post
from .privacy import can_view_user_content, is_private_account, pending_follow_request
from .sharing_models import Repost

User = get_user_model()
REEL_NOTIFICATION_KINDS = {"reel_like", "reel_comment", "reel_repost", "reel_reply"}


class AvatarUrlMixin:
    def get_avatar_url(self, obj):
        if not obj.avatar:
            return ""

        request = self.context.get("request")
        url = obj.avatar.url
        return request.build_absolute_uri(url) if request else url


class UserSerializer(AvatarUrlMixin, serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()
    avatar = serializers.ImageField(write_only=True, required=False)
    followers_count = serializers.SerializerMethodField()
    following_count = serializers.SerializerMethodField()
    posts_count = serializers.SerializerMethodField()
    is_private = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = (
            "id",
            "email",
            "username",
            "name",
            "bio",
            "location",
            "link",
            "interests",
            "profile_theme",
            "show_orbit",
            "is_verified",
            "avatar_url",
            "avatar",
            "followers_count",
            "following_count",
            "posts_count",
            "is_private",
            "date_joined",
        )
        read_only_fields = (
            "id",
            "email",
            "avatar_url",
            "followers_count",
            "following_count",
            "posts_count",
            "is_private",
            "is_verified",
            "date_joined",
        )

    def get_followers_count(self, obj):
        return obj.follower_relationships.count()

    def get_following_count(self, obj):
        return obj.following_relationships.count()

    def get_posts_count(self, obj):
        return obj.posts.count()

    def get_is_private(self, obj):
        return is_private_account(obj)

    def validate_username(self, value):
        return value.strip().lower()

    def validate_avatar(self, value):
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError("Profile photo must be 5 MB or smaller.")
        return value

    def validate_interests(self, value):
        if not isinstance(value, list):
            raise serializers.ValidationError("Interests must be a list.")
        cleaned = []
        for raw in value:
            interest = str(raw).strip()
            if not interest or interest in cleaned:
                continue
            if len(interest) > 24:
                raise serializers.ValidationError("Interests must be 24 characters or fewer.")
            cleaned.append(interest)
        if len(cleaned) > 8:
            raise serializers.ValidationError("Choose up to 8 interests.")
        return cleaned

    def validate_profile_theme(self, value):
        value = str(value).strip().lower()
        allowed = {"violet", "cyan", "orange", "pink", "slate", "ink", "black"}
        if value not in allowed:
            raise serializers.ValidationError("Choose a supported profile theme.")
        return value


class PersonSerializer(AvatarUrlMixin, serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()
    followers_count = serializers.SerializerMethodField()
    following_count = serializers.SerializerMethodField()
    posts_count = serializers.SerializerMethodField()
    is_following = serializers.SerializerMethodField()
    is_private = serializers.SerializerMethodField()
    follow_requested = serializers.SerializerMethodField()
    can_view_content = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = (
            "id",
            "username",
            "name",
            "bio",
            "location",
            "link",
            "interests",
            "profile_theme",
            "show_orbit",
            "is_verified",
            "avatar_url",
            "followers_count",
            "following_count",
            "posts_count",
            "is_following",
            "is_private",
            "follow_requested",
            "can_view_content",
        )

    def get_followers_count(self, obj):
        return obj.follower_relationships.count()

    def get_following_count(self, obj):
        return obj.following_relationships.count()

    def get_posts_count(self, obj):
        return obj.posts.count()

    def get_is_following(self, obj):
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return Follow.objects.filter(follower=request.user, following=obj).exists()

    def get_is_private(self, obj):
        return is_private_account(obj)

    def get_follow_requested(self, obj):
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return pending_follow_request(request.user, obj)

    def get_can_view_content(self, obj):
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return not is_private_account(obj)
        return can_view_user_content(request.user, obj)


class PostAuthorSerializer(AvatarUrlMixin, serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ("id", "username", "name", "avatar_url")

    def to_representation(self, instance):
        data = super().to_representation(instance)
        if not instance.is_active:
            data["username"] = "deleted"
            data["name"] = "Deleted user"
            data["avatar_url"] = ""
        return data


class PostSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)
    image = serializers.ImageField(write_only=True, required=True)
    image_url = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()
    likes_count = serializers.SerializerMethodField()
    comments_count = serializers.SerializerMethodField()
    is_liked = serializers.SerializerMethodField()
    reposts_count = serializers.SerializerMethodField()
    is_reposted = serializers.SerializerMethodField()
    reposted_by = serializers.SerializerMethodField()

    class Meta:
        model = Post
        fields = (
            "id",
            "author",
            "image",
            "image_url",
            "caption",
            "created_at",
            "is_mine",
            "likes_count",
            "comments_count",
            "is_liked",
            "reposts_count",
            "is_reposted",
            "reposted_by",
        )
        read_only_fields = (
            "id",
            "author",
            "image_url",
            "created_at",
            "is_mine",
            "likes_count",
            "comments_count",
            "is_liked",
            "reposts_count",
            "is_reposted",
            "reposted_by",
        )

    def get_image_url(self, obj):
        if not obj.image:
            return ""

        request = self.context.get("request")
        url = obj.image.url
        return request.build_absolute_uri(url) if request else url

    def get_is_mine(self, obj):
        request = self.context.get("request")
        return bool(request and request.user.is_authenticated and obj.author_id == request.user.id)

    def get_likes_count(self, obj):
        annotated = getattr(obj, "likes_count_value", None)
        return annotated if annotated is not None else obj.likes.count()

    def get_comments_count(self, obj):
        annotated = getattr(obj, "comments_count_value", None)
        return annotated if annotated is not None else obj.comments.count()

    def get_is_liked(self, obj):
        annotated = getattr(obj, "is_liked_value", None)
        if annotated is not None:
            return bool(annotated)

        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return Like.objects.filter(post=obj, user=request.user).exists()

    def get_reposts_count(self, obj):
        annotated = getattr(obj, "reposts_count_value", None)
        return annotated if annotated is not None else obj.reposts.count()

    def get_is_reposted(self, obj):
        annotated = getattr(obj, "is_reposted_value", None)
        if annotated is not None:
            return bool(annotated)
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return Repost.objects.filter(post=obj, user=request.user).exists()

    def get_reposted_by(self, obj):
        reposter = getattr(obj, "feed_reposted_by_value", None)
        if reposter is None:
            return None
        return PostAuthorSerializer(reposter, context=self.context).data

    def validate_image(self, value):
        if value.size > 10 * 1024 * 1024:
            raise serializers.ValidationError("Post photo must be 10 MB or smaller.")
        return value

    def validate_caption(self, value):
        return value.strip()


class CommentSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)
    is_mine = serializers.SerializerMethodField()

    class Meta:
        model = Comment
        fields = (
            "id",
            "author",
            "body",
            "created_at",
            "is_mine",
        )
        read_only_fields = ("id", "author", "created_at", "is_mine")

    def get_is_mine(self, obj):
        request = self.context.get("request")
        return bool(request and request.user.is_authenticated and obj.author_id == request.user.id)

    def validate_body(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("Comment can't be empty.")
        return value


class NotificationSerializer(serializers.ModelSerializer):
    actor = PostAuthorSerializer(read_only=True)
    post_id = serializers.IntegerField(read_only=True, allow_null=True)
    reel_id = serializers.SerializerMethodField()
    reel_author_username = serializers.SerializerMethodField()
    comment_preview = serializers.SerializerMethodField()
    is_read = serializers.SerializerMethodField()

    class Meta:
        model = Notification
        fields = (
            "id",
            "kind",
            "actor",
            "post_id",
            "reel_id",
            "reel_author_username",
            "comment_preview",
            "created_at",
            "is_read",
        )
        read_only_fields = fields

    def get_reel_id(self, obj):
        if obj.kind not in REEL_NOTIFICATION_KINDS:
            return None
        try:
            reel_id = int(str(obj.dedupe_key).rsplit(":", 1)[-1])
        except (TypeError, ValueError):
            return None
        return reel_id if reel_id > 0 else None

    def get_reel_author_username(self, obj):
        reel_id = self.get_reel_id(obj)
        if reel_id is None:
            return ""
        from .reels_models import Reel

        return (
            Reel.objects.filter(pk=reel_id, author__is_active=True)
            .values_list("author__username", flat=True)
            .first()
            or ""
        )

    def get_comment_preview(self, obj):
        return obj.comment.body if obj.comment_id and obj.comment else ""

    def get_is_read(self, obj):
        return obj.read_at is not None


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8, trim_whitespace=False)

    class Meta:
        model = User
        fields = ("email", "password", "username", "name")

    def validate_email(self, value):
        return User.objects.normalize_email(value).lower()

    def validate_username(self, value):
        return value.strip().lower()

    def validate_password(self, value):
        validate_password(value)
        return value

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class NovaTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        data = super().validate(attrs)
        data["user"] = UserSerializer(self.user, context=self.context).data
        return data
