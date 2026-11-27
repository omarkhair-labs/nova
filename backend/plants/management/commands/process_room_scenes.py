"""Run a bounded, restartable worker for consented photo reconstructions."""
import json
import subprocess
import sys
import time
import uuid
from datetime import timedelta
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile

from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from django.db.models import Q
from django.utils import timezone

from plants.models import RoomPhotoScene


def process_one():
    now = timezone.now()
    jobs = RoomPhotoScene.objects.filter(Q(state="queued") | Q(state="processing", updated_at__lt=now - timedelta(minutes=6)))
    first = jobs.order_by("updated_at", "pk").values_list("pk", flat=True).first()
    if first is None:
        return False
    token = uuid.uuid4()
    if not jobs.filter(pk=first).update(state="processing", lease=token, updated_at=now):
        return True
    owned = RoomPhotoScene.objects.filter(pk=first, lease=token)
    scene = owned.first()
    if scene is None:
        return True
    try:
        with TemporaryDirectory(prefix="plantupia-room-") as tmp:
            root = Path(tmp)
            images = root / "views"
            images.mkdir()
            with ZipFile(BytesIO(scene.inputs)) as archive:
                for i in range(scene.submitted_views):
                    name = f"view-{i}.jpg"
                    (images / name).write_bytes(archive.read(name))
            output = root / "room.ply"
            subprocess.run([sys.executable, "-m", "plants.room_sfm", str(images), str(output), "--surface"],
                           cwd=settings.BASE_DIR, timeout=240, check=True,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            summary = json.loads(output.with_suffix(".json").read_text(encoding="utf-8"))
            if output.stat().st_size > 700_000:
                raise ValueError("Scene too large.")
            owned.update(state="ready", lease=None, ply=output.read_bytes(), inputs=b"",
                         summary=summary, surface=json.loads(output.with_suffix(".surface.json").read_text()),
                         updated_at=timezone.now())
    except (subprocess.SubprocessError, OSError, ValueError, KeyError):
        owned.update(state="failed", lease=None, inputs=b"", summary={}, updated_at=timezone.now())
    return True


class Command(BaseCommand):
    help = "Process private room photo sets. Use --watch for a long-running QA worker."

    def add_arguments(self, parser):
        parser.add_argument("--watch", action="store_true")

    def handle(self, *args, **options):
        if not settings.PLANTUPIA_LIVING_SYSTEMS_ENABLED or not settings.PLANTUPIA_ROOM_SFM_ENABLED:
            raise CommandError("Enable living systems and room SfM on this backend.")
        try:
            import pycolmap  # noqa: F401
        except ImportError as exc:
            raise CommandError("Install the room reconstruction requirements first.") from exc
        while True:
            if not process_one():
                if not options["watch"]:
                    return
                time.sleep(3)
