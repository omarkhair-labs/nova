"""Draft commercial catalogue. Kept strictly separate from the fictional QA shop and installed PlantUnit inventory."""
import uuid
from decimal import Decimal

from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import MinValueValidator
from django.db import models


class CommerceProduct(models.Model):
    class Kind(models.TextChoices):
        PLANT = "plant", "Plant"
        PLANTER = "planter", "Pot / Planter"
        CARE = "care", "Care supply"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=160)
    description = models.TextField(max_length=3000, blank=True)
    kind = models.CharField(max_length=12, choices=Kind.choices)
    reference_species = models.ForeignKey(
        "plants.BotanicalSpecies", on_delete=models.PROTECT, null=True, blank=True,
        related_name="commerce_product_references",
        help_text="A catalogue reference, not verification of an individual customer's plant.",
    )
    cover_photo = models.ImageField(upload_to="plantupia/commerce/drafts/%Y/%m/", blank=True)
    image_description = models.CharField(max_length=180, blank=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="commerce_products_created",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ("name", "id")

    def clean(self):
        super().clean()
        if self.reference_species_id and self.kind != self.Kind.PLANT:
            raise ValidationError({"reference_species": "Only plant products may reference a botanical species."})
        if self.cover_photo and not self.image_description.strip():
            raise ValidationError({"image_description": "Describe the real photo for accessibility."})

    def __str__(self):
        return self.name


class CommerceSKU(models.Model):
    """One priced option; not yet published or available for customer checkout."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    product = models.ForeignKey(CommerceProduct, on_delete=models.PROTECT, related_name="variants")
    code = models.CharField(max_length=64, unique=True)
    option_label = models.CharField(max_length=120, blank=True)
    price = models.DecimalField(max_digits=11, decimal_places=2, validators=[MinValueValidator(Decimal("0.01"))])
    currency = models.CharField(max_length=3, default="EGP", choices=(("EGP", "EGP"),))
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ("code",)

    def clean(self):
        super().clean()
        self.code = (self.code or "").strip().upper()
        if not self.code:
            raise ValidationError({"code": "An exact SKU code is required."})

    def __str__(self):
        return self.code


class CommerceStockLocation(models.Model):
    """Sellable SKU stock location, never an installed-plant Space or manifest."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    code = models.SlugField(max_length=60, unique=True)
    name = models.CharField(max_length=140)
    active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("code",)

    def __str__(self):
        return self.name


class CommerceStockMovement(models.Model):
    """Append-only through company service. This records counts, not cash settlement or reservations."""
    class Reason(models.TextChoices):
        OPENING = "opening", "Initial counted stock"
        CORRECTION = "correction", "Stock count correction"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    sku = models.ForeignKey(CommerceSKU, on_delete=models.PROTECT, related_name="stock_movements")
    location = models.ForeignKey(CommerceStockLocation, on_delete=models.PROTECT, related_name="stock_movements")
    quantity_delta = models.IntegerField()
    reason = models.CharField(max_length=12, choices=Reason.choices)
    reference_note = models.CharField(max_length=200)
    client_id = models.UUIDField(help_text="Client-generated retry identity; reuse on a network retry.")
    fingerprint = models.CharField(max_length=64, editable=False)
    recorded_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="commerce_stock_movements_recorded",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at",)
        constraints = [
            models.CheckConstraint(condition=~models.Q(quantity_delta=0), name="plant_commerce_stock_nonzero"),
            models.UniqueConstraint(fields=("recorded_by", "client_id"), name="plant_commerce_stock_idempotency"),
        ]

    def clean(self):
        super().clean()
        if self.quantity_delta == 0:
            raise ValidationError({"quantity_delta": "A stock movement cannot be zero."})

    def __str__(self):
        return f"{self.sku.code}: {self.quantity_delta:+d}"
