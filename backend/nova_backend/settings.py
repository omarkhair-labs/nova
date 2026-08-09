import os
from datetime import timedelta
from pathlib import Path

import dj_database_url
from django.core.exceptions import ImproperlyConfigured

BASE_DIR = Path(__file__).resolve().parent.parent

ENVIRONMENT = os.getenv("DJANGO_ENV", "development").strip().lower()
IS_PRODUCTION = ENVIRONMENT == "production"

SECRET_KEY = os.getenv("DJANGO_SECRET_KEY", "nova-dev-only-secret")
DEBUG = os.getenv("DJANGO_DEBUG", "0") == "1"

if IS_PRODUCTION and SECRET_KEY == "nova-dev-only-secret":
    raise ImproperlyConfigured("DJANGO_SECRET_KEY must be set in production.")

allowed_hosts = [
    host.strip()
    for host in os.getenv("DJANGO_ALLOWED_HOSTS", "localhost,127.0.0.1").split(",")
    if host.strip()
]
railway_public_domain = os.getenv("RAILWAY_PUBLIC_DOMAIN", "").strip()
if railway_public_domain and railway_public_domain not in allowed_hosts:
    allowed_hosts.append(railway_public_domain)
ALLOWED_HOSTS = allowed_hosts

INSTALLED_APPS = [
    "daphne",
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "storages",
    "channels",
    "accounts",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "whitenoise.middleware.WhiteNoiseMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "nova_backend.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "nova_backend.wsgi.application"
ASGI_APPLICATION = "nova_backend.asgi.application"

# Railway Redis is optional during rollout so a merge cannot take production
# down before the Redis service/reference variable is attached. Once REDIS_URL
# exists, all HTTP/ASGI workers share the same channel layer automatically.
REDIS_URL = os.getenv("REDIS_URL", "").strip()
if REDIS_URL:
    CHANNEL_LAYERS = {
        "default": {
            "BACKEND": "channels_redis.core.RedisChannelLayer",
            "CONFIG": {
                "hosts": [REDIS_URL],
                "prefix": os.getenv("NOVA_CHANNEL_PREFIX", "nova-asgi"),
                "expiry": 60,
                "group_expiry": 3600,
                "capacity": 200,
            },
        }
    }
else:
    CHANNEL_LAYERS = {
        "default": {
            "BACKEND": "channels.layers.InMemoryChannelLayer",
        }
    }

database_url = os.getenv("DATABASE_URL", "").strip()
if database_url:
    DATABASES = {
        "default": dj_database_url.parse(
            database_url,
            conn_max_age=600,
            conn_health_checks=True,
        )
    }
else:
    if IS_PRODUCTION:
        raise ImproperlyConfigured("DATABASE_URL must be set in production.")

    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": os.getenv("POSTGRES_DB", "nova"),
            "USER": os.getenv("POSTGRES_USER", "nova"),
            "PASSWORD": os.getenv("POSTGRES_PASSWORD", "nova_dev"),
            "HOST": os.getenv("POSTGRES_HOST", "localhost"),
            "PORT": os.getenv("POSTGRES_PORT", "5432"),
        }
    }

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True

STATIC_URL = "/static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

STORAGES = {
    "default": {
        "BACKEND": "django.core.files.storage.FileSystemStorage",
    },
    "staticfiles": {
        "BACKEND": "whitenoise.storage.CompressedManifestStaticFilesStorage",
    },
}

bucket_env = {
    "endpoint_url": os.getenv("AWS_ENDPOINT_URL", "").strip(),
    "access_key": os.getenv("AWS_ACCESS_KEY_ID", "").strip(),
    "secret_key": os.getenv("AWS_SECRET_ACCESS_KEY", "").strip(),
    "bucket_name": os.getenv("AWS_S3_BUCKET_NAME", "").strip(),
}
use_object_storage = all(bucket_env.values())

if IS_PRODUCTION and not use_object_storage:
    raise ImproperlyConfigured(
        "Railway bucket credentials are required in production. "
        "Set AWS_ENDPOINT_URL, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_S3_BUCKET_NAME."
    )

if use_object_storage:
    STORAGES["default"] = {
        "BACKEND": "storages.backends.s3.S3Storage",
        "OPTIONS": {
            **bucket_env,
            "region_name": os.getenv("AWS_DEFAULT_REGION", "auto"),
            "addressing_style": os.getenv("AWS_S3_URL_STYLE", "virtual"),
            "signature_version": "s3v4",
            "querystring_auth": True,
            "querystring_expire": 60 * 60 * 24,
            "default_acl": None,
            "file_overwrite": False,
        },
    }

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
AUTH_USER_MODEL = "accounts.User"

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ),
    "DEFAULT_PERMISSION_CLASSES": (
        "rest_framework.permissions.IsAuthenticated",
    ),
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=15),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=30),
    "ROTATE_REFRESH_TOKENS": False,
    "UPDATE_LAST_LOGIN": True,
}

if IS_PRODUCTION:
    SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
    USE_X_FORWARDED_HOST = True
    SESSION_COOKIE_SECURE = True
    CSRF_COOKIE_SECURE = True

    trusted_origins = [
        origin.strip()
        for origin in os.getenv("DJANGO_CSRF_TRUSTED_ORIGINS", "").split(",")
        if origin.strip()
    ]
    if railway_public_domain:
        railway_origin = f"https://{railway_public_domain}"
        if railway_origin not in trusted_origins:
            trusted_origins.append(railway_origin)
    CSRF_TRUSTED_ORIGINS = trusted_origins
