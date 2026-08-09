from django.apps import AppConfig


class AccountsConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "accounts"

    def ready(self):
        # Register feature-specific models that intentionally live outside
        # the already-large accounts/models.py module.
        from . import messaging_models  # noqa: F401
        from . import security_models  # noqa: F401
