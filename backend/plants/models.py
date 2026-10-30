"""The Plantupia domain. Nova's social models stay intact and can link later.

A planted unit may be a single individual or a deliberately tracked group.
The shared label, each member's personal nickname, and the botanical identity
are different things. No public serial or QR code is required.
"""
import uuid

from django.conf import settings
from django.db import models, transaction
from django.core.exceptions import ValidationError


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


class BotanicalSpecies(models.Model):
    """Curated identity; a user-entered species_label is never verification."""
    key = models.SlugField(max_length=100, unique=True)
    scientific_name = models.CharField(max_length=180)

    def __str__(self):
        return self.scientific_name


class CareGuideVersion(models.Model):
    species = models.ForeignKey(BotanicalSpecies, on_delete=models.PROTECT, related_name="guides")
    version = models.PositiveIntegerField()
    source_url = models.URLField(max_length=500)
    source_reference = models.CharField(max_length=240)
    instructions = models.TextField()
    context_notes = models.TextField(blank=True)
    reviewer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, null=True, blank=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    published = models.BooleanField(default=False)
    ever_published = models.BooleanField(default=False, editable=False)
    assigned_reviewer = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.PROTECT,
        related_name="assigned_plantupia_guides",
        help_text="Review assignment only; not botanical approval or publication.",
    )

    class Meta:
        permissions = [("publish_careguideversion", "Review and publish care guide versions")]
        constraints = [
            models.UniqueConstraint(fields=("species", "version"), name="plant_guide_species_version"),
            models.CheckConstraint(condition=models.Q(published=False) | models.Q(
                reviewer__isnull=False, reviewed_at__isnull=False), name="plant_published_guide_review_required"),
        ]

    def clean(self):
        super().clean()
        if self.published and (not self.reviewer_id or not self.reviewed_at):
            raise ValidationError("Publication requires an actual staff review.")
        if not self.source_url.startswith("https://"):
            raise ValidationError({"source_url": "An HTTPS source is required."})
        if self.reviewer_id and not self.reviewer.is_staff:
            raise ValidationError({"reviewer": "An authorized botanical reviewer is required."})
        if self.version is not None and self.version < 1:
            raise ValidationError({"version": "Start guide versions at 1."})
        from django.utils import timezone
        if self.reviewed_at and self.reviewed_at > timezone.now():
            raise ValidationError({"reviewed_at": "A review must have actually occurred."})
        if self.pk:
            previous = type(self).objects.filter(pk=self.pk).filter(
                models.Q(published=True) | models.Q(ever_published=True)
            ).values().first()
            if previous and previous["ever_published"] and not self.ever_published:
                raise ValidationError("A published version cannot lose its immutable history.")
            if previous and any(previous[field.attname] != getattr(self, field.attname)
                                for field in self._meta.fields if field.name not in ("published", "ever_published")):
                raise ValidationError("Published content is immutable; create a new version.")

    def save(self, *args, **kwargs):
        # Validation and the write share the publication/import lock order.
        # A stale draft editor must never overwrite a concurrently published guide.
        with transaction.atomic():
            list(BotanicalSpecies.objects.select_for_update().filter(pk=self.species_id))
            if self.pk:
                list(type(self).objects.select_for_update().filter(pk=self.pk))
            self.full_clean()
            if self.published:
                self.ever_published = True
                if kwargs.get("update_fields") is not None:
                    kwargs["update_fields"] = set(kwargs["update_fields"]) | {"ever_published"}
            super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.species} v{self.version}"


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
    verified_species = models.ForeignKey(
        BotanicalSpecies, null=True, blank=True, on_delete=models.PROTECT,
        help_text="Assign only after botanical identity verification; free-text species is not verification.",
    )
    placement_note = models.CharField(max_length=180, blank=True)
    identity_reference = models.CharField(max_length=500, blank=True)
    identity_verified_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.PROTECT,
        related_name="verified_plant_identities", editable=False,
    )
    identity_verified_at = models.DateTimeField(null=True, blank=True, editable=False)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.label



class InstallationBatch(models.Model):
    """Staff-prepared real inventory; never a claim token or customer entitlement.

    An installation has exactly one existing staff-owned Space. Customer account
    rights are deliberately NOT changed until a separately reviewed claim flow.
    """

    class Status(models.TextChoices):
        DRAFT = "draft", "Draft"
        READY = "ready", "Inventory reviewed; ready for handover design"
        CLAIMED = "claimed", "Claimed by verified recipient"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    reference_code = models.CharField(max_length=80, unique=True)
    space = models.OneToOneField(
        Space, on_delete=models.PROTECT, related_name="installation_batch",
    )
    prepared_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="prepared_plantupia_installations",
    )
    status = models.CharField(max_length=12, choices=Status.choices, default=Status.DRAFT)
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        null=True, blank=True, related_name="reviewed_plantupia_installations",
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        permissions = [("import_inventory", "Preview and approve installation inventory imports"),
                       ("access_company_dashboard", "Access the Plantupia company dashboard")]

    def clean(self):
        super().clean()
        errors = {}
        if (self.space_id and self.status != self.Status.CLAIMED
                and not self.space.owner.is_staff):
            errors["space"] = "Prepare installations inside a staff-owned space only."
        if (self.status == self.Status.CLAIMED and self.pk
                and not self.invitations.filter(claimed_at__isnull=False).exists()):
            errors["status"] = "A claimed installation needs a recorded authenticated handover."
        if self.prepared_by_id and not self.prepared_by.is_staff:
            errors["prepared_by"] = "Only authorized staff may prepare an installation."
        if self.status == self.Status.READY:
            if not self.reviewed_by_id or not self.reviewed_by.is_staff or not self.reviewed_at:
                errors["status"] = "Ready inventory needs a staff reviewer and review timestamp."
            if not self.pk or not self.manifest_rows.exists():
                errors["status"] = "At least one real manifest row is required before review."
        if errors:
            raise ValidationError(errors)

    def __str__(self):
        return self.reference_code


class InstallationManifestRow(models.Model):
    """One real plant identity per source row, not a duplicated copy of PlantUnit.

    A source key is an internal inventory reference; never a public QR credential.
    """

    batch = models.ForeignKey(
        InstallationBatch, on_delete=models.PROTECT, related_name="manifest_rows",
    )
    plant = models.OneToOneField(
        PlantUnit, on_delete=models.PROTECT, related_name="installation_manifest_row",
    )
    source_key = models.CharField(max_length=80)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=("batch", "source_key"),
                name="plant_installation_source_key_unique",
            ),
        ]

    def clean(self):
        super().clean()
        errors = {}
        if self.batch_id and self.plant_id and self.plant.space_id != self.batch.space_id:
            errors["plant"] = "Manifest plant must belong to this installation's space."
        if self.batch_id and self.batch.status == InstallationBatch.Status.READY:
            errors["batch"] = "Reviewed inventory is locked. Reopen before changing entries."
        if errors:
            raise ValidationError(errors)

    def __str__(self):
        return f"{self.source_key} — {self.plant_id}"


class HandoverInvitation(models.Model):
    """Recipient-bound one-use activation. Never store or expose raw QR secrets."""

    class Mode(models.TextChoices):
        CUSTOMER_OWNED = "customer_owned", "Customer-owned sale"
        MANAGED = "managed", "Company-maintained site"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    batch = models.ForeignKey(
        InstallationBatch, on_delete=models.PROTECT, related_name="invitations",
    )
    intended_recipient = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="incoming_plantupia_handovers",
    )
    issued_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="issued_plantupia_handovers",
    )
    token_digest = models.CharField(max_length=64, unique=True, editable=False)
    mode = models.CharField(max_length=20, choices=Mode.choices)
    expires_at = models.DateTimeField()
    revoked_at = models.DateTimeField(null=True, blank=True)
    claimed_at = models.DateTimeField(null=True, blank=True)
    claimed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True,
        on_delete=models.PROTECT, related_name="claimed_plantupia_handovers",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    def clean(self):
        super().clean()
        errors = {}
        if self.issued_by_id and not self.issued_by.is_staff:
            errors["issued_by"] = "An authorized staff issuer is required."
        if self.intended_recipient_id and self.intended_recipient.is_staff:
            errors["intended_recipient"] = "Customer invitations cannot target a staff account."
        if self.claimed_by_id and self.claimed_by_id != self.intended_recipient_id:
            errors["claimed_by"] = "Only the intended account may claim the handover."
        if errors:
            raise ValidationError(errors)

    def __str__(self):
        return f"{self.batch.reference_code} ({self.mode}, secret withheld)"


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


class InventoryImportReceipt(models.Model):
    """One atomic application of normalized content; no spreadsheet/customer payload."""
    batch = models.ForeignKey(InstallationBatch, on_delete=models.PROTECT, related_name="import_receipts")
    digest = models.CharField(max_length=64)
    approved_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    approved_at = models.DateTimeField(auto_now_add=True)
    summary = models.JSONField(default=dict)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("batch", "digest"), name="plant_inventory_import_identity")]


# Django discovers these additive domain models through this app's model entrypoint.
from .demo_models import (CareInspectionPlan, CarePlanEnrollment, CarePlanDecision,
                         DemoOffer, DemoEntitlement, DemoMutation, ServiceRequest,
                         CatalogProduct, CatalogProductPhoto, DemoCartLine, DemoOrder, DemoOrderLine)  # noqa: E402,F401
