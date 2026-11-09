"""Staff-driven XLSX + ZIP import for genuine commercial draft products.

One product_key = one CommerceProduct; every row is a priced CommerceSKU.
Preview parses and validates without DB/media changes. Confirm revalidates the
same uploaded bytes, then inserts drafts and sanitized cover/gallery photos.
This is NOT catalogue publication, payment or warehouse stock allocation.
"""
from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from io import BytesIO
from pathlib import PurePosixPath
from zipfile import BadZipFile, ZipFile

from django import forms
from django.contrib import messages
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import IntegrityError, transaction
from django.http import HttpResponse, JsonResponse
from django.shortcuts import redirect
from django.urls import reverse
from openpyxl import Workbook, load_workbook

from . import company_services as company
from .commerce_models import CommerceProduct, CommerceProductPhoto, CommerceSKU
from .company_views import page, portal
from .media import sanitize_public_plant_photo
from .models import BotanicalSpecies

HEADER = (
    "product_key", "sku", "name", "kind", "description", "variant",
    "price", "currency", "cover_image", "cover_alt", "gallery_images", "species_key",
)
MAX_ZIP_BYTES = 50 * 1024 * 1024
MAX_UNCOMPRESSED = 150 * 1024 * 1024
MAX_IMAGE_BYTES = 3 * 1024 * 1024
MAX_ROWS = 600
MAX_GALLERY = 6


class CommercialCatalogError(ValueError):
    pass


class CommercialCatalogImportForm(forms.Form):
    upload = forms.FileField(
        label="Product workbook + photos (.zip)",
        help_text="products.xlsx + images/*.jpg, *.png or *.webp. Maximum 50 MB.",
    )
    action = forms.ChoiceField(
        choices=(("preview", "Validate only"), ("import", "Confirm draft import")),
        widget=forms.HiddenInput,
    )
    digest = forms.CharField(required=False, widget=forms.HiddenInput)
    confirm = forms.BooleanField(required=False, widget=forms.HiddenInput)

    def clean_upload(self):
        upload = self.cleaned_data["upload"]
        if not upload.name.lower().endswith(".zip") or upload.size > MAX_ZIP_BYTES:
            raise forms.ValidationError("Choose a ZIP package up to 50 MB.")
        return upload


@dataclass(frozen=True)
class ProductSpec:
    key: str
    name: str
    kind: str
    description: str
    cover: str
    cover_alt: str
    gallery: tuple[tuple[str, str], ...]
    species_key: str


@dataclass(frozen=True)
class SkuSpec:
    row_number: int
    product_key: str
    code: str
    variant: str
    price: Decimal


@dataclass
class ValidatedPack:
    digest: str
    products: dict[str, ProductSpec]
    skus: list[SkuSpec]
    images: dict[str, bytes]

    @property
    def photo_count(self):
        return sum(bool(p.cover) + len(p.gallery) for p in self.products.values())


def _plain(value, max_length, label, *, required=False):
    if value is None:
        value = ""
    if isinstance(value, bool):
        raise CommercialCatalogError(f"{label}: enter text, not TRUE/FALSE.")
    text = str(value).strip()
    if len(text) > max_length or (required and not text):
        raise CommercialCatalogError(f"{label}: missing or exceeds {max_length} characters.")
    if text.startswith(("=", "+", "@")) or any(ord(c) < 32 and c not in "\n\t" for c in text):
        raise CommercialCatalogError(f"{label}: invalid text or formula-like content.")
    return text


def _image_path(value, where, paths):
    value = _plain(value, 180, where)
    if not value:
        return ""
    path = PurePosixPath(value)
    if (value.startswith("/") or "\\" in value or ".." in path.parts
            or len(path.parts) != 2 or path.parts[0] != "images"
            or path.suffix.lower() not in {".jpg", ".jpeg", ".png", ".webp"}
            or value not in paths):
        raise CommercialCatalogError(f"{where}: use an existing file under images/.")
    return value


def _gallery(value, where, paths):
    value = _plain(value, 1600, where)
    if not value:
        return ()
    photos = []
    for item in value.split(";"):
        if "|" not in item:
            raise CommercialCatalogError(f"{where}: gallery entries must be images/name.jpg|Photo description.")
        path, alt = item.split("|", 1)
        name = _image_path(path.strip(), where, paths)
        description = _plain(alt, 180, where + " description", required=True)
        if not name or name in [p for p, _ in photos]:
            raise CommercialCatalogError(f"{where}: missing or repeated image.")
        photos.append((name, description))
    if len(photos) > MAX_GALLERY:
        raise CommercialCatalogError(f"{where}: maximum {MAX_GALLERY} extra images per product.")
    return tuple(photos)


def parse_commercial_pack(upload):
    """Validate the complete ZIP and all supplied photo bytes, without database writes."""
    if upload.size > MAX_ZIP_BYTES:
        raise CommercialCatalogError("ZIP exceeds the 50 MB upload limit.")
    upload.seek(0)
    body = upload.read(MAX_ZIP_BYTES + 1)
    if len(body) > MAX_ZIP_BYTES:
        raise CommercialCatalogError("ZIP exceeds the 50 MB upload limit.")

    try:
        with ZipFile(BytesIO(body)) as archive:
            entries = [f for f in archive.infolist() if not f.is_dir()]
            paths = [f.filename for f in entries]
            if len(entries) > 800 or sum(f.file_size for f in entries) > MAX_UNCOMPRESSED:
                raise CommercialCatalogError("ZIP has too many files or more than 150 MB expanded data.")
            if len(set(paths)) != len(paths):
                raise CommercialCatalogError("ZIP contains duplicated filenames.")
            if (any(not p or p.startswith("/") or "\\" in p or ".." in PurePosixPath(p).parts for p in paths)
                    or "products.xlsx" not in paths):
                raise CommercialCatalogError("ZIP needs products.xlsx at its root and safe image paths.")
            if any(p != "products.xlsx" and not p.startswith("images/") for p in paths):
                raise CommercialCatalogError("Only products.xlsx and images/ files are allowed.")
            try:
                book = load_workbook(BytesIO(archive.read("products.xlsx")), read_only=True, data_only=True)
            except (BadZipFile, OSError, ValueError, KeyError) as error:
                raise CommercialCatalogError("products.xlsx could not be read.") from error
            try:
                if "Products" not in book.sheetnames:
                    raise CommercialCatalogError("products.xlsx needs a Products worksheet.")
                sheet = book["Products"]
                values = sheet.iter_rows(values_only=True)
                head = next(values, ())
                if tuple(str(x).strip() if x is not None else "" for x in head[:len(HEADER)]) != HEADER:
                    raise CommercialCatalogError("Products header must match the downloadable commercial template.")
                products = {}
                skus = []
                seen_skus = set()
                for index, row in enumerate(values, 2):
                    if not any(value is not None and str(value).strip() for value in row):
                        continue
                    if len(skus) >= MAX_ROWS:
                        raise CommercialCatalogError(f"Maximum {MAX_ROWS} SKU rows per ZIP.")
                    fields = dict(zip(HEADER, row))
                    where = f"Row {index}"
                    key = _plain(fields["product_key"], 64, where + " product_key", required=True).lower()
                    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,63}", key):
                        raise CommercialCatalogError(f"{where}: product_key requires 3–64 lowercase letters/numbers/hyphens.")
                    code = _plain(fields["sku"], 64, where + " sku", required=True).upper()
                    if not re.fullmatch(r"[A-Z0-9][A-Z0-9_-]{2,63}", code) or code in seen_skus:
                        raise CommercialCatalogError(f"{where}: invalid or duplicate SKU.")
                    name = _plain(fields["name"], 160, where + " name", required=True)
                    kind = _plain(fields["kind"], 12, where + " kind")
                    if kind not in CommerceProduct.Kind.values:
                        raise CommercialCatalogError(f"{where}: kind must be plant, planter or care.")
                    description = _plain(fields["description"], 3000, where + " description")
                    variant = _plain(fields["variant"], 120, where + " variant")
                    currency = _plain(fields["currency"], 3, where + " currency")
                    if currency != "EGP":
                        raise CommercialCatalogError(f"{where}: only EGP is supported by the current SKU model.")
                    try:
                        price = Decimal(str(fields["price"]))
                    except (InvalidOperation, TypeError, ValueError) as error:
                        raise CommercialCatalogError(f"{where}: invalid price.") from error
                    if (not price.is_finite() or price <= 0 or price > Decimal("999999999.99")
                            or price.as_tuple().exponent < -2):
                        raise CommercialCatalogError(f"{where}: price must be a positive EGP value with at most 2 decimals.")

                    cover = _image_path(fields["cover_image"], where + " cover_image", paths)
                    cover_alt = _plain(fields["cover_alt"], 180, where + " cover_alt")
                    gallery = _gallery(fields["gallery_images"], where + " gallery_images", paths)
                    if (cover and not cover_alt) or (cover_alt and not cover) or (gallery and not cover):
                        raise CommercialCatalogError(f"{where}: cover photo needs a description, and gallery needs a cover.")
                    if cover and cover in [name for name, _ in gallery]:
                        raise CommercialCatalogError(f"{where}: cover and gallery photo cannot be the same file.")
                    species_key = _plain(fields["species_key"], 120, where + " species_key")
                    if species_key and kind != CommerceProduct.Kind.PLANT:
                        raise CommercialCatalogError(f"{where}: botanical species references only belong to plants.")
                    spec = ProductSpec(key, name, kind, description, cover, cover_alt, gallery, species_key)
                    if key in products and products[key] != spec:
                        raise CommercialCatalogError(
                            f"{where}: product fields differ between variants of {key}; repeat the same product details.")
                    products[key] = spec
                    seen_skus.add(code)
                    skus.append(SkuSpec(index, key, code, variant, price))
                if not skus:
                    raise CommercialCatalogError("No products or SKUs were found in the workbook.")
            finally:
                book.close()

            required_photos = {
                path for spec in products.values()
                for path in ([spec.cover] if spec.cover else []) + [p for p, _ in spec.gallery]
            }
            image_data = {}
            for path in required_photos:
                info = archive.getinfo(path)
                if info.file_size > MAX_IMAGE_BYTES:
                    raise CommercialCatalogError(f"{path}: image exceeds 3 MB.")
                photo = SimpleUploadedFile(path.rsplit("/", 1)[-1], archive.read(path))
                try:
                    normalized = sanitize_public_plant_photo(photo)
                except ValueError as error:
                    raise CommercialCatalogError(f"{path}: invalid image ({error}).") from error
                if normalized.size > MAX_IMAGE_BYTES:
                    raise CommercialCatalogError(f"{path}: normalized image exceeds 3 MB.")
                image_data[path] = normalized.read()
            return ValidatedPack(hashlib.sha256(body).hexdigest(), products, skus, image_data)
    except (BadZipFile, OSError, RuntimeError) as error:
        raise CommercialCatalogError("Cannot read ZIP archive.") from error


def verify_commercial_duplicates(pack):
    """A bulk operation creates drafts; it never overwrites existing product or SKU data."""
    keys = list(pack.products)
    codes = [s.code for s in pack.skus]
    existing_keys = list(CommerceProduct.objects.filter(catalog_key__in=keys)
                         .values_list("catalog_key", flat=True)[:5])
    existing_skus = list(CommerceSKU.objects.filter(code__in=codes)
                         .values_list("code", flat=True)[:5])
    if existing_keys or existing_skus:
        raise CommercialCatalogError(
            "Already present; import will not overwrite: "
            + ", ".join(existing_keys + existing_skus))
    names = list(CommerceProduct.objects.filter(name__in=[p.name for p in pack.products.values()])
                 .values_list("name", flat=True)[:5])
    if names:
        raise CommercialCatalogError("A commercial product already uses this name: " + ", ".join(names))
    keys = {p.species_key for p in pack.products.values() if p.species_key}
    missing = keys - set(BotanicalSpecies.objects.filter(key__in=keys).values_list("key", flat=True))
    if missing:
        raise CommercialCatalogError("Unknown botanical species key: " + ", ".join(sorted(missing)[:5]))


def save_commercial_pack(staff, pack):
    """Atomic DB inserts with best-effort media rollback; no stock count or publication."""
    stored = []
    try:
        with transaction.atomic():
            get_user_model().objects.select_for_update().get(pk=staff.pk)
            verify_commercial_duplicates(pack)
            species_keys = {p.species_key for p in pack.products.values() if p.species_key}
            species = {x.key: x for x in BotanicalSpecies.objects.filter(key__in=species_keys)}
            for spec in pack.products.values():
                obj = CommerceProduct(
                    name=spec.name, catalog_key=spec.key, kind=spec.kind,
                    description=spec.description, created_by=staff,
                    reference_species=species.get(spec.species_key),
                    image_description=spec.cover_alt,
                )
                if spec.cover:
                    obj.cover_photo.save(
                        f"{spec.key}-cover.jpg",
                        SimpleUploadedFile("cover.jpg", pack.images[spec.cover], content_type="image/jpeg"),
                        save=False,
                    )
                    stored.append((obj.cover_photo.storage, obj.cover_photo.name))
                obj.full_clean()
                obj.save()
                for index, (path, alt_text) in enumerate(spec.gallery, 1):
                    photo = CommerceProductPhoto(product=obj, alt_text=alt_text, position=index)
                    photo.image.save(
                        f"{spec.key}-gallery-{index}.jpg",
                        SimpleUploadedFile("gallery.jpg", pack.images[path], content_type="image/jpeg"),
                        save=False,
                    )
                    stored.append((photo.image.storage, photo.image.name))
                    photo.full_clean()
                    photo.save()
                company.audit(staff, obj,
                    f"Imported commercial product draft {spec.key}; {len(spec.gallery)} gallery images; not published.")
            products = {p.catalog_key: p for p in
                        CommerceProduct.objects.filter(catalog_key__in=pack.products)}
            for line in pack.skus:
                sku = CommerceSKU(
                    product=products[line.product_key],
                    code=line.code, option_label=line.variant,
                    price=line.price, currency="EGP",
                )
                sku.full_clean()
                sku.save()
                company.audit(staff, sku, f"Imported draft SKU from XLSX row {line.row_number}; no stock or sale.")
        return len(pack.products), len(pack.skus), pack.photo_count
    except Exception:
        for storage, name in stored:
            try:
                storage.delete(name)
            except Exception:
                pass
        raise


def commercial_template_bytes():
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "Products"
    sheet.append(HEADER)
    sheet.freeze_panes = "A2"
    sizes = [18, 22, 34, 14, 45, 22, 16, 12, 30, 36, 58, 20]
    for i, size in enumerate(sizes, 1):
        sheet.column_dimensions[__import__("openpyxl").utils.get_column_letter(i)].width = size
    sheet.auto_filter.ref = "A1:L1"
    notes = workbook.create_sheet("Instructions")
    for row in [
        ("Plantupia / Commercial draft catalogue import",),
        ("One row = one SKU. Several rows may share product_key; repeat identical product fields.",),
        ("product_key: stable lowercase key 3-64 letters/numbers/hyphens.",),
        ("kind: plant, planter or care. currency: EGP.",),
        ("cover_image: images/name.jpg. cover_alt: a truthful description of that image.",),
        ("gallery_images: images/detail1.jpg|alt text;images/detail2.webp|alt text (up to 6).",),
        ("species_key: optional existing BotanicalSpecies key; only for plant kind.",),
        ("ZIP layout: products.xlsx at root; images/*.jpg, *.png or *.webp."),
        ("Preview is read-only. Confirm creates unpublished products, SKUs and sanitized photos.",),
        ("Stock counts, publication, actual payment and delivery are separate operations.",),
    ]:
        notes.append(row)
    notes.column_dimensions["A"].width = 100
    buffer = BytesIO()
    workbook.save(buffer)
    workbook.close()
    return buffer.getvalue()


@portal("plants.view_commerceproduct", methods=("GET",))
def commercial_import_template(request):
    response = HttpResponse(commercial_template_bytes(),
        content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    response["Content-Disposition"] = 'attachment; filename="plantupia-commercial-products-template.xlsx"'
    return response


@portal("plants.view_commerceproduct", "plants.add_commerceproduct", "plants.add_commercesku")
def commercial_import(request):
    """Customer API stays untouched. Staff may only create unpublished drafts."""
    form = CommercialCatalogImportForm(request.POST or None, request.FILES or None)
    preview, outcome = [], None
    json_mode = "application/json" in request.headers.get("Accept", "")
    if request.method == "POST":
        try:
            if not form.is_valid():
                raise CommercialCatalogError("Choose a valid ZIP and action.")
            pack = parse_commercial_pack(form.cleaned_data["upload"])
            verify_commercial_duplicates(pack)
            preview = [{
                "key": product.key, "name": product.name, "kind": product.kind,
                "sku_count": sum(x.product_key == product.key for x in pack.skus),
                "photos": bool(product.cover) + len(product.gallery),
            } for product in pack.products.values()]
            if form.cleaned_data["action"] == "import":
                if not form.cleaned_data["confirm"] or form.cleaned_data["digest"] != pack.digest:
                    raise CommercialCatalogError("Validate this exact ZIP first, then confirm its draft import.")
                try:
                    product_count, sku_count, photo_count = save_commercial_pack(request.user, pack)
                except (IntegrityError, ValidationError) as error:
                    raise CommercialCatalogError(
                        "Import changed during confirmation or a record failed validation. "
                        "Nothing was committed; check existing SKUs and retry."
                    ) from error
                messages.success(request,
                    f"Imported {product_count} draft products, {sku_count} SKUs and {photo_count} photos; not published.")
                if json_mode:
                    return JsonResponse({"ok": True, "imported": product_count,
                                         "redirect": reverse("company:commerce-products")})
                return redirect("company:commerce-products")
            outcome = f"Validated {len(pack.products)} products, {len(pack.skus)} SKUs and {pack.photo_count} images. Nothing was saved."
            if json_mode:
                return JsonResponse({
                    "ok": True, "digest": pack.digest, "products": len(pack.products),
                    "skus": len(pack.skus), "photos": pack.photo_count,
                    "preview": preview[:30], "notice": outcome,
                })
        except CommercialCatalogError as error:
            form.add_error(None, str(error))
            if json_mode:
                return JsonResponse({"ok": False, "error": str(error)}, status=400)
    return page(request, "commerce_import", {
        "title": "Import commercial product drafts",
        "section": "commerce_products", "form": form,
        "preview": preview, "outcome": outcome,
    }, status=400 if form.errors else 200)
