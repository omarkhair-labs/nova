from django.contrib.auth import get_user_model
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

User = get_user_model()


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ("id", "email", "username", "name", "avatar_url", "date_joined")
        read_only_fields = ("id", "email", "date_joined")


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8, trim_whitespace=False)

    class Meta:
        model = User
        fields = ("email", "password", "username", "name")

    def validate_email(self, value):
        return User.objects.normalize_email(value).lower()

    def validate_username(self, value):
        return value.strip().lower()

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class NovaTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        data = super().validate(attrs)
        data["user"] = UserSerializer(self.user).data
        return data
