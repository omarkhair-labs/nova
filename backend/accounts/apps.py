from django.apps import AppConfig


class AccountsConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "accounts"

    def ready(self):
        # Register messaging-specific models that intentionally live outside
        # the already-large accounts/models.py module.
        from . import messaging_models  # noqa: F401
        # Persist terminal voice/video calls as durable conversation history.
        from .calls import call_history  # noqa: F401
        # Stories evolve independently from the core social models while still
        # registering under the accounts Django app.
        from . import story_models  # noqa: F401
        # Sharing/reposts stay modular while participating in the same app.
        from . import sharing_models  # noqa: F401
        # Privacy, follow requests and Close Friends are modular social policy.
        from . import privacy_models  # noqa: F401
        # Reels are a V3 media surface with their own durable interaction graph.
        from . import reels_models  # noqa: F401
        # Comment replies remain modular so legacy comments stay migration-safe.
        from . import comment_reply_models  # noqa: F401
        # Pulse is the live social layer; its durable model remains in accounts.
        from . import pulse_models  # noqa: F401
