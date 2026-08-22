from django.urls import include, path


urlpatterns = [
    path("", include("accounts.auth.urls")),
    path("", include("accounts.trust_safety_urls")),
    path("", include("accounts.privacy_urls")),
    path("", include("accounts.social.urls")),
    path("", include("accounts.stories_urls")),
    path("", include("accounts.posts.urls")),
    path("", include("accounts.sharing.urls")),
    path("", include("accounts.notifications.urls")),
    path("", include("accounts.calls_urls")),
    path("", include("accounts.messaging.urls")),
]
