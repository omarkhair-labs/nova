"""Public website enquiries are not authenticated customer orders or care records."""
import uuid
from django.conf import settings
from django.db import models


class WebsiteEnquiry(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    submission_id = models.UUIDField(unique=True, editable=False)
    fingerprint = models.CharField(max_length=64, editable=False)
    topic = models.CharField(max_length=12, choices=[("project", "Project"), ("care", "Care & support"), ("general", "General")])
    name = models.CharField(max_length=100)
    email = models.EmailField(max_length=160)
    organization = models.CharField(max_length=120, blank=True)
    environment = models.CharField(max_length=20, blank=True)
    location = models.CharField(max_length=100, blank=True)
    message = models.TextField(max_length=2000)
    status = models.CharField(max_length=12, choices=[("new", "New"), ("reviewed", "Reviewed"), ("closed", "Closed")], default="new", db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    reviewed_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)

    class Meta:
        ordering = ("-created_at", "id")
