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
    catalog_key = models.SlugField(max_length=64, unique=True, null=True, blank=True,
        help_text="Stable identity for reviewed catalogue imports; not a retail SKU.")
    qa_listed = models.BooleanField(default=False,
        help_text="Explicitly available in the isolated QA app only; never commercial publication.")
    class CatalogReviewStatus(models.TextChoices):
        DRAFT = "draft", "Needs product review"
        APPROVED = "approved", "Product data approved for future storefront"

    catalog_review_status = models.CharField(
        max_length=12, choices=CatalogReviewStatus.choices, default=CatalogReviewStatus.DRAFT,
        help_text="Staff content approval only. Does not publish to Android or authorize taking orders.")
    catalog_reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.PROTECT,
        related_name="plantupia_catalog_products_reviewed",
    )
    catalog_reviewed_at = models.DateTimeField(null=True, blank=True)

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


class CommerceProductPhoto(models.Model):
    """Optional ordered commercial draft gallery; no implicit publication."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    product = models.ForeignKey(CommerceProduct, on_delete=models.CASCADE, related_name="gallery")
    image = models.ImageField(upload_to="plantupia/commerce/gallery/%Y/%m/")
    alt_text = models.CharField(max_length=180)
    position = models.PositiveSmallIntegerField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("position", "id")
        constraints = [
            models.UniqueConstraint(fields=("product", "position"),
                name="plant_commerce_gallery_position_unique"),
        ]

    def __str__(self):
        return f"{self.product.name} · image {self.position}"


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


class CommerceOrderRequest(models.Model):
    """A noncommercial request for company review; not a paid or dispatched sale."""
    class PaymentPreference(models.TextChoices):
        COD = "cod", "Cash on delivery — preference only"
        MANUAL_TRANSFER = "manual_transfer", "Manual InstaPay/bank transfer — preference only"

    class Status(models.TextChoices):
        SUBMITTED = "qa_submitted", "QA request submitted"
        REVIEWED = "qa_reviewed", "QA reviewed"
        PREPARING = "qa_preparing", "QA preparing"
        OUT_FOR_DELIVERY = "qa_out_for_delivery", "QA out for delivery"
        DELIVERED = "qa_delivered", "QA delivered"
        CANCELLED = "qa_cancelled", "QA cancelled"
        RETURN_REQUESTED = "qa_return_requested", "QA return requested"
        RETURNED = "qa_returned", "QA returned"
        DECLINED = "qa_declined", "QA request declined"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    customer = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="plantupia_commerce_requests",
    )
    client_id = models.UUIDField(help_text="Stable customer retry identifier.")
    fingerprint = models.CharField(max_length=64, editable=False)
    contact_name = models.CharField(max_length=160)
    contact_phone = models.CharField(max_length=32)
    city = models.CharField(max_length=120)
    delivery_address = models.CharField(max_length=320)
    # Customer-confirmed pin. Optional for compatibility with earlier QA requests;
    # both coordinates must be present together, and are never inferred from free text.
    delivery_latitude = models.DecimalField(max_digits=10, decimal_places=7, null=True, blank=True)
    delivery_longitude = models.DecimalField(max_digits=10, decimal_places=7, null=True, blank=True)
    delivery_pin_confirmed = models.BooleanField(default=False)
    building_details = models.CharField(max_length=180, blank=True)
    landmark = models.CharField(max_length=180, blank=True)
    customer_note = models.CharField(max_length=500, blank=True)
    payment_preference = models.CharField(max_length=20, choices=PaymentPreference.choices)
    provisional_subtotal = models.DecimalField(max_digits=12, decimal_places=2)
    status = models.CharField(max_length=22, choices=Status.choices, default=Status.SUBMITTED)
    review_note = models.CharField(max_length=500, blank=True)
    # Staff-entered QA shipping quote; NULL always means "not yet quoted", never free.
    qa_shipping_fee = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True,
        validators=[MinValueValidator(Decimal("0.00"))])
    qa_tracking_reference = models.CharField(max_length=120, blank=True)
    staff_first_seen_at = models.DateTimeField(null=True, blank=True)
    customer_cancel_requested_at = models.DateTimeField(null=True, blank=True)
    customer_return_requested_at = models.DateTimeField(null=True, blank=True)
    last_status_at = models.DateTimeField(null=True, blank=True)
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, null=True, blank=True,
        related_name="plantupia_commerce_requests_reviewed",
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at",)
        constraints = [
            models.UniqueConstraint(
                fields=("customer", "client_id"),
                name="plant_commerce_request_idempotency",
            ),
        ]

    def clean(self):
        super().clean()
        has_lat = self.delivery_latitude is not None
        has_lon = self.delivery_longitude is not None
        if has_lat != has_lon:
            raise ValidationError("Both pin coordinates are required together.")
        if has_lat:
            if not (-90 <= self.delivery_latitude <= 90 and -180 <= self.delivery_longitude <= 180):
                raise ValidationError("The selected map location is outside valid coordinates.")
            if not self.delivery_pin_confirmed:
                raise ValidationError("Confirm the exact pin before submitting it.")
        elif self.delivery_pin_confirmed:
            raise ValidationError("A confirmed pin requires both map coordinates.")
        if self.qa_shipping_fee is not None and self.qa_shipping_fee < 0:
            raise ValidationError({"qa_shipping_fee": "Shipping cannot be negative."})
        if self.provisional_subtotal is not None and self.provisional_subtotal < 0:
            raise ValidationError({"provisional_subtotal": "Subtotal cannot be negative."})
        if self.status != self.Status.SUBMITTED and (
            not self.reviewed_by_id or not self.reviewed_at
        ):
            raise ValidationError({"status": "A QA review decision requires reviewer and timestamp."})

    def __str__(self):
        return f"QA commerce request {self.pk}"


class CommerceOrderStatusEvent(models.Model):
    """Customer-visible in-app timeline, never a payment or delivery proof."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    order = models.ForeignKey(CommerceOrderRequest, on_delete=models.PROTECT, related_name="status_events")
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="plantupia_commerce_order_status_events")
    kind = models.CharField(max_length=40)
    label = models.CharField(max_length=120)
    note = models.CharField(max_length=320, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("created_at", "pk")

    def __str__(self):
        return f"{self.order_id}: {self.kind}"


class CommerceOrderRequestLine(models.Model):
    """Immutable-at-service-boundary price/SKU snapshot of request intent."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    order = models.ForeignKey(
        CommerceOrderRequest, on_delete=models.PROTECT, related_name="lines",
    )
    sku = models.ForeignKey(CommerceSKU, on_delete=models.PROTECT)
    product_name_snapshot = models.CharField(max_length=160)
    sku_code_snapshot = models.CharField(max_length=64)
    option_snapshot = models.CharField(max_length=120, blank=True)
    unit_price_snapshot = models.DecimalField(max_digits=11, decimal_places=2)
    quantity = models.PositiveSmallIntegerField()
    line_subtotal_snapshot = models.DecimalField(max_digits=12, decimal_places=2)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=("order", "sku"), name="plant_commerce_request_line_unique",
            ),
            models.CheckConstraint(
                condition=models.Q(quantity__gte=1, quantity__lte=10),
                name="plant_commerce_request_line_qty",
            ),
        ]

    def __str__(self):
        return f"{self.sku_code_snapshot} × {self.quantity}"


class CommerceBusinessRFQ(models.Model):
    """Short nonbinding B2B quote enquiry, not a purchase order."""
    class Status(models.TextChoices):
        SUBMITTED = "qa_submitted", "Request received"
        QUOTED = "qa_quoted", "Quote sent (QA)"
        ACCEPTED = "qa_accepted", "Quote accepted for follow-up (QA)"
        DECLINED = "qa_declined", "Declined"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    requester = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT,
        related_name="plantupia_business_rfqs")
    client_id = models.UUIDField()
    fingerprint = models.CharField(max_length=64, editable=False)
    company_name = models.CharField(max_length=180)
    contact_name = models.CharField(max_length=160)
    phone = models.CharField(max_length=32)
    email = models.EmailField(blank=True)
    city = models.CharField(max_length=120)
    sku = models.ForeignKey(CommerceSKU, null=True, blank=True, on_delete=models.PROTECT)
    sku_snapshot = models.CharField(max_length=64, blank=True)
    quantity = models.PositiveIntegerField(default=1)
    scope = models.TextField(max_length=1500)
    status = models.CharField(max_length=15, choices=Status.choices, default=Status.SUBMITTED)
    quoted_amount = models.DecimalField(max_digits=12, decimal_places=2,
        null=True, blank=True, validators=[MinValueValidator(Decimal("0.01"))])
    quote_note = models.CharField(max_length=800, blank=True)
    replied_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True,
        on_delete=models.PROTECT, related_name="plantupia_business_rfqs_replied")
    replied_at = models.DateTimeField(null=True, blank=True)
    accepted_at = models.DateTimeField(null=True, blank=True)
    staff_first_seen_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at",)
        constraints = [
            models.UniqueConstraint(fields=("requester", "client_id"),
                name="plant_b2b_rfq_idempotency"),
            models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10000),
                name="plant_b2b_rfq_quantity"),
        ]

    def clean(self):
        super().clean()
        if self.quoted_amount is not None and self.quoted_amount <= 0:
            raise ValidationError({"quoted_amount": "Quote must be positive."})
        if self.status == self.Status.QUOTED and (
            self.quoted_amount is None or not self.replied_by_id or not self.replied_at):
            raise ValidationError({"status": "A quote requires amount, reviewer and timestamp."})
        if self.status == self.Status.ACCEPTED and not self.accepted_at:
            raise ValidationError({"status": "A QA acceptance requires a timestamp."})

    def __str__(self):
        return f"Business RFQ {self.id}"
