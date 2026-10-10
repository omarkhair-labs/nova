"""The Plantupia domain. Nova's social models stay intact and can link later.

A planted unit may be a single individual or a deliberately tracked group.
The shared label, each member's personal nickname, and the botanical identity
are different things. No public serial or QR code is required.
"""
import uuid

from django.conf import settings
from django.db import models


class Space(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="owned_plantupia_spaces"
    )
    name = models.CharField(max_length=140)
    description = models.TextField(blank=True)
    location_note = models.CharField(max_length=200, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name


class SpaceMember(models.Model):
    class Role(models.TextChoices):
        VIEWER = "viewer", "Viewer"
        CARER = "carer", "Carer"
        MANAGER = "manager", "Manager"

    space = models.ForeignKey(Space, on_delete=models.CASCADE, related_name="memberships")
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="plantupia_memberships"
    )
    role = models.CharField(max_length=10, choices=Role.choices, default=Role.VIEWER)
    joined_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=["space", "user"], name="plant_space_unique_member")]


class PlantUnit(models.Model):
    class UnitType(models.TextChoices):
        INDIVIDUAL = "individual", "Individual plant"
        GROUP = "group", "Planted group"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    space = models.ForeignKey(Space, on_delete=models.PROTECT, related_name="plants")
    unit_type = models.CharField(max_length=12, choices=UnitType.choices, default=UnitType.INDIVIDUAL)
    label = models.CharField(max_length=120)
    species_label = models.CharField(max_length=180, blank=True)
    placement_note = models.CharField(max_length=180, blank=True)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.label


class PlantNickname(models.Model):
    plant = models.ForeignKey(PlantUnit, on_delete=models.CASCADE, related_name="nicknames")
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="plantupia_nicknames"
    )
    nickname = models.CharField(max_length=80)

    class Meta:
        constraints = [models.UniqueConstraint(fields=["plant", "user"], name="plant_unique_personal_nickname")]


class PlantEvent(models.Model):
    class Kind(models.TextChoices):
        OBSERVATION = "observation", "Observation"
        CARE = "care", "Care"
        ISSUE = "issue", "Issue"
        OUTCOME = "outcome", "Outcome"

    class Evidence(models.TextChoices):
        OBSERVED = "observed", "Personally observed"
        MEASURED = "measured", "Measured"
        REPORTED = "reported", "Reported by someone else"
        INFERRED = "inferred", "Inferred"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    plant = models.ForeignKey(PlantUnit, on_delete=models.PROTECT, related_name="events")
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="plantupia_events")
    kind = models.CharField(max_length=12, choices=Kind.choices)
    evidence_type = models.CharField(max_length=10, choices=Evidence.choices, default=Evidence.OBSERVED)
    note = models.TextField(blank=True)
    photo = models.ImageField(upload_to="plantupia/events/%Y/%m/", blank=True)
    observed_at = models.DateTimeField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-observed_at", "-created_at"]
