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
    cover_photo = models.ImageField(upload_to="plantupia/units/%Y/%m/", blank=True)
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
    # A deliberate human check-in, not a watering prediction or notification.
    follow_up_on = models.DateField(null=True, blank=True)
    follow_up_completed_at = models.DateTimeField(null=True, blank=True)
    follow_up_for = models.ForeignKey(
        "self", null=True, blank=True, on_delete=models.SET_NULL,
        related_name="documented_outcomes",
    )
    observed_at = models.DateTimeField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-observed_at", "-created_at"]


class PlantDiscussion(models.Model):
    """A consented public Nova post attached privately to its source plant.

    Public responses never serialize space, owner, plant UUID, or private history.
    Deleting the public post also deletes this link; plant history survives.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    plant = models.ForeignKey(PlantUnit, on_delete=models.PROTECT, related_name="discussions")
    post = models.OneToOneField("accounts.Post", on_delete=models.CASCADE, related_name="plantupia_discussion")
    public_topic = models.CharField(max_length=80)
    created_at = models.DateTimeField(auto_now_add=True)
    resolved_at = models.DateTimeField(null=True, blank=True)
    resolution_event = models.ForeignKey(
        PlantEvent, null=True, blank=True, on_delete=models.SET_NULL, related_name="resolved_discussions"
    )

    class Meta:
        ordering = ("-created_at",)

    def __str__(self):
        return f"Plant discussion for post {self.post_id}"


class PlantSubmission(models.Model):
    """Account-scoped immutable receipt; never serialized in public Circle feeds."""
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="plantupia_submissions")
    client_submission_id = models.UUIDField()
    operation = models.CharField(max_length=12)
    plant_id = models.UUIDField()
    fingerprint = models.CharField(max_length=64)
    response = models.JSONField(default=dict)
    status_code = models.PositiveSmallIntegerField(default=201)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("actor", "client_submission_id"), name="plant_submission_actor_identity")]
