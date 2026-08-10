from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as DjangoUserAdmin

from .models import User, UserBlock, UserReport
from .story_models import Story, StoryReaction, StoryView


@admin.register(User)
class UserAdmin(DjangoUserAdmin):
    model = User
    ordering = ("email",)
    list_display = ("email", "username", "name", "is_staff", "is_active")
    search_fields = ("email", "username", "name")

    fieldsets = (
        (None, {"fields": ("email", "password")}),
        ("Profile", {"fields": ("username", "name", "avatar")}),
        ("Personal info", {"fields": ("first_name", "last_name")}),
        (
            "Permissions",
            {
                "fields": (
                    "is_active",
                    "is_staff",
                    "is_superuser",
                    "groups",
                    "user_permissions",
                )
            },
        ),
        ("Important dates", {"fields": ("last_login", "date_joined")}),
    )

    add_fieldsets = (
        (
            None,
            {
                "classes": ("wide",),
                "fields": ("email", "username", "name", "password1", "password2"),
            },
        ),
    )


@admin.register(UserReport)
class UserReportAdmin(admin.ModelAdmin):
    list_display = ("reported", "reporter", "reason", "status", "created_at")
    list_filter = ("status", "reason")
    search_fields = ("reported__username", "reported__email", "reporter__username", "reporter__email")
    readonly_fields = ("created_at", "updated_at")


@admin.register(UserBlock)
class UserBlockAdmin(admin.ModelAdmin):
    list_display = ("blocker", "blocked", "created_at")
    search_fields = ("blocker__username", "blocked__username")
    readonly_fields = ("created_at",)


@admin.register(Story)
class StoryAdmin(admin.ModelAdmin):
    list_display = ("id", "author", "media_type", "created_at", "expires_at")
    list_filter = ("media_type", "created_at", "expires_at")
    search_fields = ("author__username", "author__email", "caption")
    readonly_fields = ("created_at",)


@admin.register(StoryView)
class StoryViewAdmin(admin.ModelAdmin):
    list_display = ("story", "viewer", "viewed_at")
    search_fields = ("story__author__username", "viewer__username")
    readonly_fields = ("viewed_at",)


@admin.register(StoryReaction)
class StoryReactionAdmin(admin.ModelAdmin):
    list_display = ("story", "user", "emoji", "updated_at")
    search_fields = ("story__author__username", "user__username", "emoji")
    readonly_fields = ("created_at", "updated_at")
