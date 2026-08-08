from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from .models import Follow, Post

User = get_user_model()


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

    class Meta:
        model = User
        fields = (
            "id",
            "email",
            "username",
            "name",
            "avatar_url",
            "avatar",
            "followers_count",
            "following_count",
            "posts_count",
            "date_joined",
        )
        read_only_fields = (
            "id",
            "email",
            "avatar_url",
            "followers_count",
            "following_count",
            "posts_count",
            "date_joined",
        )

    def get_followers_count(self, obj):
        return obj.follower_relationships.count()

    def get_following_count(self, obj):
        return obj.following_relationships.count()

    def get_posts_count(self, obj):
        return obj.posts.count()

    def validate_username(self, value):
        return value.strip().lower()

    def validate_avatar(self, value):
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError("Profile photo must be 5 MB or smaller.")
        return value


class PersonSerializer(AvatarUrlMixin, serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()
    followers_count = serializers.SerializerMethodField()
    following_count = serializers.SerializerMethodField()
    posts_count = serializers.SerializerMethodField()
    is_following = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = (
            "id",
            "username",
            "name",
            "avatar_url",
            "followers_count",
            "following_count",
            "posts_count",
            "is_following",
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


class PostAuthorSerializer(AvatarUrlMixin, serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ("id", "username", "name", "avatar_url")


class PostSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)
    image = serializers.ImageField(write_only=True, required=True)
    image_url = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()

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
        )
        read_only_fields = ("id", "author", "image_url", "created_at", "is_mine")

    def get_image_url(self, obj):
        if not obj.image:
            return ""

        request = self.context.get("request")
        url = obj.image.url
        return request.build_absolute_uri(url) if request else url

    def get_is_mine(self, obj):
        request = self.context.get("request")
        return bool(request and request.user.is_authenticated and obj.author_id == request.user.id)

    def validate_image(self, value):
        if value.size > 10 * 1024 * 1024:
            raise serializers.ValidationError("Post photo must be 10 MB or smaller.")
        return value

    def validate_caption(self, value):
        return value.strip()


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
