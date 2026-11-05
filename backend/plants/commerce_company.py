"""Draft-only Company Commerce workspace. No public Store/checkout endpoints."""
import uuid

from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.db.models import Q, Sum
from django.shortcuts import get_object_or_404, redirect

from . import company_services as company, commerce_services as commerce
from .commerce_models import CommerceProduct, CommerceSKU, CommerceStockLocation, CommerceStockMovement
from .company_views import page, paginated, portal
from .media import sanitize_public_plant_photo


class CommerceProductForm(forms.ModelForm):
    class Meta:
        model = CommerceProduct
        fields = ("name", "description", "kind", "reference_species", "cover_photo", "image_description")
        help_texts = {
            "reference_species": "Catalogue type only; never verification of a real delivered plant.",
            "cover_photo": "An authentic authorized photograph only. Draft products never appear in customer Shop.",
            "image_description": "Describe the actual photograph for accessibility.",
        }
        widgets = {"description": forms.Textarea(attrs={"rows": 4})}

    def clean_cover_photo(self):
        photo = self.cleaned_data.get("cover_photo")
        if not photo or not hasattr(photo, "content_type"):
            return photo
        try:
            result = sanitize_public_plant_photo(photo)
            if result.size > 2 * 1024 * 1024:
                raise ValueError("Use a photo smaller than 2 MiB after normalization.")
            return result
        except ValueError as exc:
            raise forms.ValidationError(str(exc)) from exc


class CommerceSKUForm(forms.ModelForm):
    class Meta:
        model = CommerceSKU
        fields = ("code", "option_label", "price", "currency")
        help_texts = {
            "code": "Unique exact variant SKU. A SKU is not a verified physical plant identity.",
            "price": "Draft price only; no checkout or payment is enabled.",
        }

    def clean_code(self):
        code = self.cleaned_data["code"].strip().upper()
        if not code:
            raise forms.ValidationError("An exact SKU code is required.")
        return code


class CommerceStockLocationForm(forms.ModelForm):
    class Meta:
        model = CommerceStockLocation
        fields = ("code", "name", "active")
        help_texts = {"code": "A warehouse or sales stock location, not a customer installation."}


class StockCountForm(forms.Form):
    location = forms.ModelChoiceField(queryset=CommerceStockLocation.objects.none())
    quantity_delta = forms.IntegerField(min_value=-100000, max_value=100000,
        label="Count change (+ received / - correction)")
    reason = forms.ChoiceField(choices=CommerceStockMovement.Reason.choices)
    reference_note = forms.CharField(max_length=200,
        help_text="A physical count or receiving reference. This does not reserve or sell stock.")
    client_id = forms.UUIDField(widget=forms.HiddenInput)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["location"].queryset = CommerceStockLocation.objects.filter(active=True)


@portal("plants.view_commerceproduct", methods=("GET",))
def products(request):
    qs = CommerceProduct.objects.select_related("reference_species", "created_by").prefetch_related("variants")
    q = request.GET.get("q", "").strip()[:120]
    if q:
        qs = qs.filter(Q(name__icontains=q) | Q(variants__code__icontains=q)).distinct()
    return page(request, "commerce_catalog", {
        "title": "Commercial product drafts", "section": "commerce_products",
        "records": paginated(request, qs.order_by("name", "id")), "query": q,
    })


@portal("plants.add_commerceproduct")
def product_create(request):
    form = CommerceProductForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        product = form.save(commit=False)
        product.created_by = request.user
        product.full_clean()
        product.save()
        company.audit(request.user, product, "Created a draft commercial product; no customer publication")
        messages.success(request, "Draft product saved. It is not visible in the customer app.")
        return redirect("company:commerce-product", product_id=product.pk)
    return page(request, "form", {
        "title": "New commercial product draft", "section": "commerce_products", "form": form,
        "multipart": True, "submit": "Save internal draft",
        "description": "Create an internal product record without charging customers or publishing stock.",
    })


@portal("plants.view_commerceproduct", methods=("GET",))
def product_detail(request, product_id):
    product = get_object_or_404(CommerceProduct.objects.select_related("created_by", "reference_species"), pk=product_id)
    variants = list(product.variants.all())
    for sku in variants:
        sku.counted_balance = commerce.stock_balance(sku.pk)
    return page(request, "commerce_product", {
        "title": product.name, "section": "commerce_products", "product": product, "variants": variants,
    })


@portal("plants.add_commercesku")
def sku_create(request, product_id):
    company.require(request.user, "plants.view_commerceproduct")
    product = get_object_or_404(CommerceProduct, pk=product_id)
    form = CommerceSKUForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        sku = form.save(commit=False)
        sku.product = product
        sku.full_clean()
        sku.save()
        company.audit(request.user, sku, "Created draft sellable SKU; no checkout/publication")
        messages.success(request, "Variant saved in the internal draft catalog.")
        return redirect("company:commerce-product", product_id=product.pk)
    return page(request, "form", {
        "title": "Add SKU / variant", "section": "commerce_products", "form": form,
        "submit": "Save draft SKU",
        "description": f"{product.name} · Internal draft SKU, not available for customer orders.",
    })


@portal("plants.view_commercestockmovement", methods=("GET",))
def stock_ledger(request):
    stock = (CommerceSKU.objects.select_related("product")
             .annotate(counted_balance=Sum("stock_movements__quantity_delta")).order_by("code"))
    history = CommerceStockMovement.objects.select_related("sku", "location", "recorded_by")
    return page(request, "commerce_stock", {
        "title": "Counted commercial stock", "section": "commerce_stock",
        "stock": paginated(request, stock), "recent": history.order_by("-created_at")[:20],
        "locations": CommerceStockLocation.objects.order_by("code")[:30],
    })


@portal("plants.add_commercestocklocation")
def location_create(request):
    company.require(request.user, "plants.view_commercestockmovement")
    form = CommerceStockLocationForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        location = form.save()
        company.audit(request.user, location, "Created separate commercial stock-count location")
        messages.success(request, "Stock location saved. This is not an installed Plantupia space.")
        return redirect("company:commerce-stock")
    return page(request, "form", {
        "title": "New stock-count location", "section": "commerce_stock", "form": form,
        "submit": "Create stock location",
        "description": "Warehouse/sales stock only. Does not change any customer installation.",
    })


@portal("plants.add_commercestockmovement")
def stock_count(request, sku_id):
    company.require(request.user, "plants.view_commercestockmovement")
    sku = get_object_or_404(CommerceSKU.objects.select_related("product"), pk=sku_id)
    form = StockCountForm(request.POST or None, initial={"client_id": uuid.uuid4()})
    if request.method == "POST" and form.is_valid():
        try:
            commerce.record_counted_stock(
                staff=request.user, sku_id=sku.pk, location_id=form.cleaned_data["location"].pk,
                quantity_delta=form.cleaned_data["quantity_delta"], reason=form.cleaned_data["reason"],
                reference_note=form.cleaned_data["reference_note"], client_id=form.cleaned_data["client_id"],
            )
        except ValidationError as exc:
            form.add_error(None, exc)
        else:
            messages.success(request, "Counted stock recorded. No sale or payment was created.")
            return redirect("company:commerce-stock")
    return page(request, "form", {
        "title": "Record counted stock", "section": "commerce_stock", "form": form,
        "submit": "Record counted change",
        "description": f"{sku.code} · Current total across locations: {commerce.stock_balance(sku.pk)}. "
                       "No checkout, reservation or allocation occurs here.",
    })
