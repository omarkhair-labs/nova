"""Public photo boundary: normalize image bytes, never publish camera metadata."""
from io import BytesIO

from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image, ImageOps, UnidentifiedImageError


def sanitize_public_plant_photo(upload):
    """Return a safe JPEG with camera EXIF/GPS and original filename removed."""
    if upload is None:
        return None
    if upload.size > 10 * 1024 * 1024:
        raise ValueError("Public photos must be 10 MB or smaller.")
    try:
        upload.seek(0)
        with Image.open(upload) as source:
            # Excludes screenshots and compressed-image decompression bombs
            # too large to safely transform on a small VM.
            if source.width * source.height > 20_000_000:
                raise ValueError("Public photo resolution is too large.")
            if source.format not in {"JPEG", "PNG", "WEBP"}:
                raise ValueError("Use a JPEG, PNG or WebP photo.")
            upright = ImageOps.exif_transpose(source)
            rgb = upright.convert("RGB")
            buffer = BytesIO()
            rgb.save(buffer, format="JPEG", quality=90, optimize=True)
        data = buffer.getvalue()
        if len(data) > 10 * 1024 * 1024:
            raise ValueError("Public photo after conversion is too large.")
        return SimpleUploadedFile(
            "public-plant-photo.jpg", data, content_type="image/jpeg"
        )
    except (OSError, ValueError, UnidentifiedImageError) as error:
        if isinstance(error, ValueError):
            raise
        raise ValueError("The selected photo could not be read.") from error
