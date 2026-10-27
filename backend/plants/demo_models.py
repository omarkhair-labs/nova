"""Additive, isolated CEO QA domain. No billing or fulfillment engine."""
import uuid
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import MinValueValidator, MaxValueValidator
from django.db import models, transaction


class DemoMutation(models.Model):
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    client_id = models.UUIDField()
    operation = models.CharField(max_length=90)
    fingerprint = models.CharField(max_length=64)
    response = models.JSONField(default=dict)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("actor", "client_id"), name="plant_demo_mutation_actor_id")]


class CareInspectionPlan(models.Model):
    class Action(models.TextChoices):
        SOIL = "soil", "Inspect soil condition"
        LEAVES = "leaves", "Inspect leaves"
        DRAINAGE = "drainage", "Inspect drainage"
        ROTATION = "rotation", "Inspect light before considering rotation"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    plant = models.ForeignKey("plants.PlantUnit", on_delete=models.PROTECT, related_name="inspection_plans")
    guide = models.ForeignKey("plants.CareGuideVersion", on_delete=models.PROTECT)
    action = models.CharField(max_length=16, choices=Action.choices)
    every_days = models.PositiveSmallIntegerField(validators=[MinValueValidator(1), MaxValueValidator(365)])
    start_on = models.DateField()
    timezone = models.CharField(max_length=64, default="Africa/Cairo")
    placement_context = models.CharField(max_length=180, blank=True)
    instructions = models.CharField(max_length=1200)
    prepared_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="prepared_inspection_plans")
    approved_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="approved_inspection_plans", null=True, blank=True)
    approved_at = models.DateTimeField(null=True, blank=True)
    active = models.BooleanField(default=True)

    class Meta:
        permissions = [("approve_careinspectionplan", "Approve source-backed inspection plans")]
        constraints = [models.CheckConstraint(condition=models.Q(every_days__gte=1, every_days__lte=365), name="plant_inspection_cadence_bound")]

    def clean(self):
        super().clean()
        try:
            ZoneInfo(self.timezone)
        except (ZoneInfoNotFoundError, ValueError):
            raise ValidationError({"timezone": "Choose an IANA timezone, for example Africa/Cairo."})
        if self.active and self.plant_id and self.guide_id and (not self.guide.published or self.plant.verified_species_id != self.guide.species_id):
            raise ValidationError("A plan needs this plant's verified identity and a published guide for that species.")
        if bool(self.approved_by_id) != bool(self.approved_at):
            raise ValidationError("Approval needs the actual staff actor and time.")
        old = type(self).objects.filter(pk=self.pk).first() if self.pk else None
        if old and old.approved_at:
            immutable = ("plant_id", "guide_id", "action", "every_days", "start_on", "timezone", "placement_context", "instructions", "prepared_by_id", "approved_by_id", "approved_at")
            if any(getattr(old, name) != getattr(self, name) for name in immutable):
                raise ValidationError("Approved plans are immutable. Retire this version and prepare a new one.")

    def save(self, *args, **kwargs):
        with transaction.atomic():
            if self.pk:
                list(type(self).objects.select_for_update().filter(pk=self.pk))
            self.full_clean()
            super().save(*args, **kwargs)


class CarePlanEnrollment(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    plan = models.ForeignKey(CareInspectionPlan, on_delete=models.PROTECT, related_name="enrollments")
    responsible = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    enabled = models.BooleanField(default=False)
    next_due_on = models.DateField()
    revision = models.PositiveIntegerField(default=0)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("plan", "responsible"), name="plant_plan_responsible_once")]


class CarePlanDecision(models.Model):
    enrollment = models.ForeignKey(CarePlanEnrollment, on_delete=models.PROTECT, related_name="decisions")
    action = models.CharField(max_length=16)
    due_on = models.DateField()
    next_due_on = models.DateField()
    event = models.OneToOneField("plants.PlantEvent", on_delete=models.PROTECT, null=True)
    created_at = models.DateTimeField(auto_now_add=True)


class DemoOffer(models.Model):
    key = models.SlugField(primary_key=True, max_length=40)
    name = models.CharField(max_length=80)
    proposed_monthly_price = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(0)])
    currency = models.CharField(max_length=3, default="EGP", choices=[("EGP", "EGP")])
    ai_daily_limit = models.PositiveSmallIntegerField(default=20, validators=[MinValueValidator(1), MaxValueValidator(35)])
    human_requests_per_30_days = models.PositiveSmallIntegerField(default=0, validators=[MaxValueValidator(10)])
    qa_hours = models.PositiveSmallIntegerField(default=24, validators=[MinValueValidator(1), MaxValueValidator(72)])
    active = models.BooleanField(default=True)


class DemoEntitlement(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="plantupia_demo_access")
    offer = models.ForeignKey(DemoOffer, on_delete=models.PROTECT)
    granted_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()
    revoked_at = models.DateTimeField(null=True, blank=True)


class ServiceRequest(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    customer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="plantupia_service_requests")
    plant = models.ForeignKey("plants.PlantUnit", on_delete=models.PROTECT)
    # Reuse Nova's actual message storage, with no generic DM/group memberships.
    # Only the ticket endpoints authorize this conversation, never Inbox/DM URLs.
    conversation = models.OneToOneField("accounts.Conversation", on_delete=models.PROTECT, related_name="plantupia_service_request")
    subject = models.CharField(max_length=140)
    shared_plant_label = models.CharField(max_length=180)
    shared_context = models.CharField(max_length=500, blank=True)
    assigned_to = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, null=True, blank=True, related_name="assigned_plantupia_requests")
    status = models.CharField(max_length=16, choices=[("open", "Awaiting staff"), ("replied", "Staff replied"), ("closed", "Closed")], default="open")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        permissions = [("manage_service_requests", "Handle explicitly shared Plantupia support requests")]


class CatalogProduct(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=120)
    description = models.CharField(max_length=1500)
    kind = models.CharField(max_length=16, choices=[("plant", "Plant"), ("planter", "Planter"), ("accessory", "Care accessory")])
    image = models.ImageField(upload_to="plantupia/catalog/%Y/%m/", blank=True)
    proposed_price = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(0)])
    currency = models.CharField(max_length=3, default="EGP", choices=[("EGP", "EGP")])
    qa_stock = models.PositiveIntegerField(default=10, validators=[MaxValueValidator(10000)])
    active = models.BooleanField(default=True)


class DemoCartLine(models.Model):
    customer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    product = models.ForeignKey(CatalogProduct, on_delete=models.PROTECT)
    quantity = models.PositiveSmallIntegerField(validators=[MinValueValidator(1), MaxValueValidator(10)])

    class Meta:
        constraints = [models.UniqueConstraint(fields=("customer", "product"), name="plant_demo_cart_unique"),
                       models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10), name="plant_demo_cart_quantity")]


class DemoOrder(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    customer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="plantupia_demo_orders")
    created_at = models.DateTimeField(auto_now_add=True)
    total_provisional = models.DecimalField(max_digits=10, decimal_places=2)
    status = models.CharField(max_length=16, default="qa_requested", choices=[("qa_requested", "QA request received"), ("draft_prepared", "Draft installation prepared")])
    installation = models.OneToOneField("plants.InstallationBatch", on_delete=models.PROTECT, null=True, blank=True)


class DemoOrderLine(models.Model):
    order = models.ForeignKey(DemoOrder, on_delete=models.PROTECT, related_name="lines")
    product = models.ForeignKey(CatalogProduct, on_delete=models.PROTECT)
    name_snapshot = models.CharField(max_length=120)
    kind_snapshot = models.CharField(max_length=16)
    unit_price_provisional = models.DecimalField(max_digits=8, decimal_places=2)
    quantity = models.PositiveSmallIntegerField(validators=[MinValueValidator(1), MaxValueValidator(10)])

    class Meta:
        constraints = [models.UniqueConstraint(fields=("order", "product"), name="plant_demo_order_product_once"),
                       models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10), name="plant_demo_order_quantity")]
