"""Standalone company web product: real sessions/forms/services, synthetic QA only."""
import os
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
from unittest.mock import patch

from django.conf import settings
from django.contrib.admin.models import LogEntry
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.core.cache import cache
from django.core.exceptions import PermissionDenied
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import close_old_connections
from django.test import Client, TestCase, TransactionTestCase, override_settings, skipUnlessDBFeature
from django.urls import reverse
from rest_framework.test import APIClient

from . import company_services as service
from .inventory_import import ImportProblem, template_bytes
from .models import (BotanicalSpecies, CareGuideVersion, HandoverInvitation,
                     InstallationBatch, InventoryImportReceipt, PlantEvent, PlantUnit, Space)

TEST_SETTINGS = {
    "PLANTUPIA_COMPANY_DASHBOARD_ENABLED": True,
    "PASSWORD_HASHERS": ["django.contrib.auth.hashers.MD5PasswordHasher"],
    "STORAGES": {**settings.STORAGES, "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}},
}


class CompanyFixture:
    def setUp(self):
        cache.clear()
        User = get_user_model()
        self.staff = User.objects.create_user(username="qa_company_operator", email="company-operator@example.test", password="qa-only", is_staff=True)
        self.staff.user_permissions.set(Permission.objects.filter(content_type__app_label="plants"))
        self.recipient = User.objects.create_user(username="qa_company_recipient", email="recipient@example.test", password="qa-only")
        self.other = User.objects.create_user(username="qa_other_employee", email="other-employee@example.test", password="qa-only", is_staff=True)
        self.other.user_permissions.set(Permission.objects.filter(content_type__app_label="plants"))
        self.space = Space.objects.create(owner=self.staff, name="FICTIONAL QA — Studio 01")
        self.batch = InstallationBatch.objects.create(space=self.space, prepared_by=self.staff, reference_code="QA-DASHBOARD-001")
        self.client.force_login(self.staff)
        self.flag = patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"})
        self.flag.start()
        self.addCleanup(self.flag.stop)

    def url(self, name, *args):
        return reverse("company:" + name, args=args)

    def upload(self, batch=None):
        batch = batch or self.batch
        return SimpleUploadedFile("fictional-qa.xlsx", template_bytes(batch))

    def import_inventory(self):
        response = self.client.post(self.url("import", self.batch.pk), {"file": self.upload()})
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.context["plan"]["errors"])
        token = response.context["approval"].initial["preview"]
        result = self.client.post(self.url("import", self.batch.pk), {"preview": token, "confirmed": "on"})
        self.assertContains(result, "Import committed atomically")
        return token

    def ready(self):
        self.import_inventory()
        response = self.client.get(self.url("installation-review", self.batch.pk))
        result = self.client.post(self.url("installation-review", self.batch.pk), {
            "review_token": response.context["form"].initial["review_token"], "reviewed": "on"})
        self.assertEqual(result.status_code, 302)
        self.batch.refresh_from_db()
        self.assertEqual(self.batch.status, "ready")

    def issue(self, **extra):
        url = self.url("handover-issue", self.batch.pk)
        response = self.client.get(url)
        payload = {"recipient_username": self.recipient.username, "mode": "customer_owned", "approved_qa_policy": "on",
                   "review_token": response.context["form"].initial["review_token"], **extra}
        return self.client.post(url, payload), payload


@override_settings(**TEST_SETTINGS)
class CompanyDashboardTests(CompanyFixture, TestCase):
    def test_qa_role_presets_do_not_grant_superuser_or_account_administration(self):
        from .company_roles import create_qa_roles
        operator, reviewer = create_qa_roles()
        employee = get_user_model().objects.create_user(username="qa_scoped_operator", email="scoped@example.test", is_staff=True)
        employee.groups.add(operator)
        self.assertFalse(employee.is_superuser)
        self.assertTrue(employee.has_perm("plants.import_inventory"))
        self.assertFalse(employee.has_perm("plants.publish_careguideversion"))
        self.assertFalse(employee.has_perm("accounts.change_user"))
        self.assertFalse(operator.permissions.exclude(content_type__app_label="plants").exists())
        self.assertFalse(reviewer.permissions.filter(codename="add_handoverinvitation").exists())

    def test_seed_refuses_existing_database_and_production_without_writing_credentials(self):
        count = get_user_model().objects.count()
        with TemporaryDirectory() as directory:
            path = Path(directory) / "credentials.json"
            with self.assertRaises(CommandError):
                call_command("seed_local_plantupia_company_qa", credentials_file=str(path))
            with override_settings(IS_PRODUCTION=True, ENVIRONMENT="production"):
                with self.assertRaises(CommandError):
                    call_command("seed_local_plantupia_company_qa", credentials_file=str(path))
            self.assertFalse(path.exists())
        self.assertEqual(get_user_model().objects.count(), count)

    def test_feature_off_and_non_qa_production_refuse_entire_portal(self):
        with override_settings(PLANTUPIA_COMPANY_DASHBOARD_ENABLED=False):
            self.assertEqual(self.client.get(self.url("login")).status_code, 404)
            self.assertEqual(self.client.get(self.url("overview")).status_code, 404)
        with override_settings(IS_PRODUCTION=True), patch("plants.company_services.pilot_enabled", return_value=False):
            self.assertEqual(self.client.get(self.url("overview")).status_code, 404)
        with override_settings(IS_PRODUCTION=False, ENVIRONMENT="development", DATABASES={"default": {
            "ENGINE": "django.db.backends.postgresql", "HOST": "unverified-remote-database"}}):
            self.assertFalse(service.enabled())

    def test_staff_login_customer_denial_logout_and_no_admin_ui_dependency(self):
        self.client.logout()
        self.assertEqual(self.client.get(self.url("overview")).status_code, 302)
        login = self.client.get(self.url("login"))
        self.assertContains(login, "Company sign in")
        self.assertNotContains(login, "Django administration")
        rejected = self.client.post(self.url("login"), {"username": self.recipient.email, "password": "qa-only"})
        self.assertContains(rejected, "Company access is not available")
        accepted = self.client.post(self.url("login"), {"username": self.staff.email, "password": "qa-only", "next": "https://example.test/unsafe"})
        self.assertRedirects(accepted, self.url("overview"))
        self.assertEqual(self.client.get(self.url("logout")).status_code, 405)
        self.assertRedirects(self.client.post(self.url("logout")), self.url("login"))
        self.client.force_login(self.recipient)
        self.assertEqual(self.client.get(self.url("overview")).status_code, 403)

    def test_login_throttle_does_not_authenticate_after_limit(self):
        self.client.logout()
        for _ in range(10):
            self.client.post(self.url("login"), {"username": self.staff.email, "password": "incorrect"})
        response = self.client.post(self.url("login"), {"username": self.staff.email, "password": "qa-only"})
        self.assertContains(response, "Too many attempts")
        self.assertNotIn("_auth_user_id", self.client.session)

    def test_missing_access_or_granular_permissions_deny_direct_routes(self):
        self.staff.user_permissions.remove(Permission.objects.get(codename="access_company_dashboard", content_type__app_label="plants"))
        self.assertEqual(self.client.get(self.url("overview")).status_code, 403)
        self.staff.user_permissions.set(Permission.objects.filter(codename__in=("access_company_dashboard", "view_installationbatch"), content_type__app_label="plants"))
        self.assertEqual(self.client.get(self.url("overview")).status_code, 200)
        for name, args in (("inventory", ()), ("care", ()), ("handovers", ()), ("import", (self.batch.pk,)), ("installation-review", (self.batch.pk,))):
            with self.subTest(name=name):
                self.assertEqual(self.client.get(self.url(name, *args)).status_code, 403)

    def test_company_employees_cannot_bypass_scope_through_technical_admin(self):
        self.assertEqual(self.client.get(reverse("admin:index")).status_code, 302)
        self.assertEqual(self.client.get(reverse("admin:plants_space_changelist")).status_code, 302)
        self.client.logout()
        response = self.client.post(reverse("admin:login"), {"username": self.staff.email, "password": "qa-only"})
        self.assertContains(response, "Use the Plantupia Company Dashboard")
        # Legacy technical staff without the new portal permission retain compatibility.
        self.client.force_login(self.staff)
        self.staff.user_permissions.remove(Permission.objects.get(codename="access_company_dashboard", content_type__app_label="plants"))
        self.assertEqual(self.client.get(reverse("admin:index")).status_code, 200)

    def test_csrf_real_login_create_import_publish_issue_and_logout(self):
        client = Client(enforce_csrf_checks=True)
        self.assertEqual(client.post(self.url("login"), {"username": self.staff.email, "password": "qa-only"}).status_code, 403)
        client.get(self.url("login"))
        result = client.post(self.url("login"), {"username": self.staff.email, "password": "qa-only", "csrfmiddlewaretoken": client.cookies["csrftoken"].value})
        self.assertEqual(result.status_code, 302)
        for url in (self.url("installation-create"), self.url("import", self.batch.pk), self.url("installation-review", self.batch.pk), self.url("logout")):
            self.assertEqual(client.post(url, {}).status_code, 403)
        result = client.post(self.url("import", self.batch.pk), {"file": self.upload(), "csrfmiddlewaretoken": client.cookies["csrftoken"].value})
        self.assertIsNotNone(result.context["plan"])
        self.ready()
        guide = CareGuideVersion.objects.get()
        self.assertEqual(client.post(self.url("guide", guide.pk), {}).status_code, 403)
        self.assertEqual(client.post(self.url("handover-issue", self.batch.pk), {}).status_code, 403)

    def test_real_counts_scope_and_search_do_not_disclose_another_staff_space(self):
        elsewhere = Space.objects.create(owner=self.other, name="PRIVATE OTHER QA SITE")
        alien = InstallationBatch.objects.create(space=elsewhere, prepared_by=self.other, reference_code="QA-OTHER-PRIVATE")
        overview = self.client.get(self.url("overview"))
        self.assertEqual(overview.context["counts"], {"draft": 1, "ready": 0, "claimed": 0})
        self.assertNotContains(overview, elsewhere.name)
        self.assertNotContains(self.client.get(self.url("installations")), alien.reference_code)
        for name in ("installation", "installation-edit", "import", "template", "installation-review", "handover-issue"):
            self.assertEqual(self.client.get(self.url(name, alien.pk)).status_code, 404)

    def test_create_edit_draft_and_duplicate_create_rolls_back_extra_space(self):
        url = self.url("installation-create")
        data = {"reference_code": "QA-DASHBOARD-NEW", "name": "QA private courtyard", "description": "Fictional fixture", "location_note": "Test only"}
        result = self.client.post(url, data)
        self.assertEqual(result.status_code, 302)
        created = InstallationBatch.objects.get(reference_code=data["reference_code"])
        self.assertEqual(created.prepared_by, self.staff)
        self.assertEqual(created.space.owner, self.staff)
        count = Space.objects.count()
        self.client.post(url, data)
        self.assertEqual(Space.objects.count(), count)
        data["name"] = "QA courtyard edited"
        self.assertEqual(self.client.post(self.url("installation-edit", created.pk), data).status_code, 302)
        created.space.refresh_from_db()
        self.assertEqual(created.space.name, data["name"])

    def test_draft_operator_can_upload_a_real_plant_photo_and_invalid_media_is_rejected(self):
        from PIL import Image

        def plant_form(key, photo):
            return {
                "source_key": key,
                "label": "FICTIONAL QA studio plant",
                "unit_type": "individual",
                "species_label": "Unverified studio plant",
                "placement_note": "Near the window",
                "cover_photo": photo,
            }

        png = BytesIO()
        Image.new("RGB", (80, 120), (70, 110, 70)).save(png, format="PNG")
        with TemporaryDirectory() as media_root, override_settings(MEDIA_ROOT=media_root):
            url = self.url("plant-create", self.batch.pk)
            result = self.client.post(url, plant_form(
                "qa-real-photo-001",
                SimpleUploadedFile("camera-original.png", png.getvalue(), content_type="image/png"),
            ))
            self.assertEqual(result.status_code, 302)
            unit = PlantUnit.objects.get(label="FICTIONAL QA studio plant")
            self.assertTrue(unit.cover_photo.name.startswith("plantupia/units/"))
            self.assertTrue(unit.cover_photo.name.endswith(".jpg"))
            with unit.cover_photo.open("rb") as saved:
                self.assertEqual(saved.read(3), b"\xff\xd8\xff")
            # Only a draft's authorized operator may change this identity/photo.
            self.client.force_login(self.other)
            self.assertEqual(
                self.client.get(self.url("plant-edit", self.batch.pk, unit.pk)).status_code,
                404,
            )
            self.client.force_login(self.staff)
            invalid = self.client.post(url, plant_form(
                "qa-invalid-photo",
                SimpleUploadedFile("spoofed.png", b"not an image", content_type="image/png"),
            ))
            self.assertEqual(invalid.status_code, 200)
            self.assertIn("cover_photo", invalid.context["form"].errors)
            self.assertFalse(
                PlantUnit.objects.filter(installation_manifest_row__source_key="qa-invalid-photo").exists()
            )

    def test_import_preview_is_read_only_explicit_apply_replay_and_history(self):
        url = self.url("import", self.batch.pk)
        before = (PlantUnit.objects.count(), CareGuideVersion.objects.count(), LogEntry.objects.count())
        response = self.client.post(url, {"file": self.upload()})
        self.assertEqual((PlantUnit.objects.count(), CareGuideVersion.objects.count(), LogEntry.objects.count()), before)
        self.assertContains(response, "Incoming values")
        self.assertContains(response, str(self.space.pk))
        token = response.context["approval"].initial["preview"]
        self.client.post(url, {"preview": token})
        self.assertFalse(PlantUnit.objects.exists())
        self.client.post(url, {"preview": token, "confirmed": "on"})
        original = PlantUnit.objects.get().pk
        repeated = self.client.post(url, {"preview": token, "confirmed": "on"})
        self.assertContains(repeated, "Already applied")
        self.assertEqual(PlantUnit.objects.get().pk, original)
        self.assertEqual(InventoryImportReceipt.objects.count(), 1)
        self.assertIsNone(CareGuideVersion.objects.get().reviewer)
        self.assertContains(self.client.get(url), self.staff.username)
        self.assertFalse(PlantEvent.objects.exists())

    def test_import_token_cannot_be_rebound_to_another_destination(self):
        response = self.client.post(self.url("import", self.batch.pk), {"file": self.upload()})
        token = response.context["approval"].initial["preview"]
        second = InstallationBatch.objects.create(space=Space.objects.create(owner=self.staff, name="Second QA space"), prepared_by=self.staff, reference_code="QA-SECOND")
        response = self.client.post(self.url("import", second.pk), {"preview": token, "confirmed": "on"})
        self.assertContains(response, "different installation")
        self.assertFalse(PlantUnit.objects.exists())

    def test_manual_plant_edit_preserves_uuid_source_and_clears_obsolete_verification(self):
        url = self.url("plant-create", self.batch.pk)
        fields = {"source_key": "QA-MANUAL-01", "label": "QA plant", "unit_type": "individual", "species_label": "Unconfirmed QA", "placement_note": "QA desk"}
        self.assertEqual(self.client.post(url, fields).status_code, 302)
        plant = PlantUnit.objects.get()
        species = BotanicalSpecies.objects.create(key="qa-manual", scientific_name="Synthetic QA identity")
        plant.verified_species, plant.identity_reference, plant.identity_verified_by = species, "QA old proof", self.staff
        plant.save()
        fields.update(source_key="malicious-change", species_label="Corrected unknown")
        self.assertEqual(self.client.post(self.url("plant-edit", self.batch.pk, plant.pk), fields).status_code, 302)
        plant.refresh_from_db()
        self.assertEqual(plant.installation_manifest_row.source_key, "QA-MANUAL-01")
        self.assertIsNone(plant.verified_species)
        self.assertEqual(plant.identity_reference, "")

    def test_ready_is_explicit_state_bound_exact_and_locks_edit_import_routes(self):
        self.import_inventory()
        url = self.url("installation-review", self.batch.pk)
        token = self.client.get(url).context["form"].initial["review_token"]
        PlantUnit.objects.update(placement_note="Intervening QA placement")
        self.assertContains(self.client.post(url, {"review_token": token, "reviewed": "on"}), "changed since your review")
        token = self.client.get(url).context["form"].initial["review_token"]
        self.assertEqual(self.client.post(url, {"review_token": token, "reviewed": "on"}).status_code, 302)
        for name in ("installation-edit", "plant-create", "import", "template"):
            self.assertEqual(self.client.get(self.url(name, self.batch.pk)).status_code, 409)
        self.assertFalse(HandoverInvitation.objects.exists())

    def test_source_review_assignment_publication_withdrawal_and_version_immutability(self):
        self.import_inventory()
        guide = CareGuideVersion.objects.get()
        guide.assigned_reviewer = self.other
        guide.save()
        url = self.url("guide", guide.pk)
        token = self.client.get(url).context["form"].initial["review_token"]
        self.assertEqual(self.client.post(url, {"review_token": token, "reviewed": "on", "action": "publish"}).status_code, 403)
        self.client.force_login(self.other)
        token = self.client.get(url).context["form"].initial["review_token"]
        self.assertEqual(self.client.post(url, {"review_token": token, "reviewed": "on", "action": "publish"}).status_code, 302)
        guide.refresh_from_db()
        self.assertEqual(guide.reviewer, self.other)
        self.assertTrue(guide.published)
        self.assertTrue(guide.reviewed_at)
        self.assertEqual(self.client.get(self.url("guide-edit", guide.pk)).status_code, 409)
        token = self.client.get(url).context["form"].initial["review_token"]
        self.client.post(url, {"review_token": token, "reviewed": "on", "action": "withdraw"})
        guide.refresh_from_db()
        self.assertFalse(guide.published)
        self.assertTrue(guide.ever_published)

    def test_guide_edit_invalid_version_and_stale_publish_never_approve_unreviewed_content(self):
        self.import_inventory()
        guide = CareGuideVersion.objects.get()
        url = self.url("guide", guide.pk)
        token = self.client.get(url).context["form"].initial["review_token"]
        guide.instructions = "Changed synthetic draft"
        guide.save()
        response = self.client.post(url, {"review_token": token, "reviewed": "on", "action": "publish"})
        self.assertContains(response, "changed since your review")
        self.assertFalse(CareGuideVersion.objects.get().published)
        response = self.client.post(self.url("guide-create"), {"species": guide.species_id, "version": "", "source_url": "https://example.test/qa", "source_reference": "QA", "instructions": "QA"})
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "This field is required")
        response = self.client.post(self.url("guide-create"), {"species": guide.species_id, "version": 2, "source_url": "https://example.test/qa", "source_reference": "QA", "instructions": "x" * 8001})
        self.assertContains(response, "at most 8000 characters")

    def test_issue_once_replay_refused_reissue_revokes_and_register_has_no_secret(self):
        self.ready()
        response, payload = self.issue()
        self.assertEqual(response.status_code, 200)
        code = response.context["code"]
        self.assertContains(response, "<svg")
        self.assertIn("no-store", response["Cache-Control"])
        self.assertIn("frame-ancestors 'none'", response["Content-Security-Policy"])
        self.assertNotIn(code, str(list(HandoverInvitation.objects.values())))
        self.assertNotIn(code, str(list(LogEntry.objects.values())))
        self.assertNotContains(self.client.get(self.url("handovers")), code)
        repeated = self.client.post(self.url("handover-issue", self.batch.pk), payload)
        self.assertContains(repeated, "changed since your review")
        self.assertEqual(HandoverInvitation.objects.count(), 1)
        replacement, _ = self.issue(replace_existing="on")
        self.assertNotEqual(replacement.context["code"], code)
        self.assertEqual(HandoverInvitation.objects.filter(revoked_at__isnull=False).count(), 1)
        invitation = HandoverInvitation.objects.filter(revoked_at__isnull=True).get()
        revoke_url = self.url("handover-revoke", invitation.pk)
        token = self.client.get(revoke_url).context["form"].initial["review_token"]
        self.assertEqual(self.client.post(revoke_url, {"review_token": token, "reviewed": "on"}).status_code, 302)
        invitation.refresh_from_db()
        self.assertIsNotNone(invitation.revoked_at)

    def test_disabled_or_unapproved_handover_and_wrong_recipient_never_issue(self):
        self.ready()
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "0"}):
            self.assertEqual(self.client.get(self.url("handover-issue", self.batch.pk)).status_code, 409)
        result, _ = self.issue(approved_qa_policy="")
        self.assertContains(result, "This field is required")
        result, _ = self.issue(recipient_username=self.staff.username)
        self.assertContains(result, "No eligible existing recipient")
        self.assertFalse(HandoverInvitation.objects.exists())

    def test_complete_portal_flow_to_existing_android_contract_and_claim_privacy(self):
        self.ready()
        guide = CareGuideVersion.objects.get()
        guide_url = self.url("guide", guide.pk)
        token = self.client.get(guide_url).context["form"].initial["review_token"]
        self.client.post(guide_url, {"review_token": token, "reviewed": "on", "action": "publish"})
        plant = PlantUnit.objects.get()
        # Actual independently reviewed synthetic identity fixture; import proof path
        # is covered by the preserved import suite.
        plant.verified_species = guide.species
        plant.save()
        self.assertContains(self.client.get(self.url("installation", self.batch.pk)), "Published care · version 1")
        response, _ = self.issue()
        code = response.context["code"]
        api = APIClient()
        api.force_authenticate(self.recipient)
        result = api.post(reverse("plantupia-handover-claim"), {"invitation": code, "confirm": True}, format="json")
        self.assertEqual(result.status_code, 200)
        self.assertEqual(PlantUnit.objects.get().pk, plant.pk)
        care = api.get(reverse("plantupia-care-guide", args=[plant.pk])).json()
        self.assertEqual(care["state"], "available")
        self.assertEqual(care["guide"]["reviewer"], self.staff.username)
        self.assertEqual(care["placement"], plant.placement_note)
        self.space.refresh_from_db()
        self.space.name = "PRIVATE CUSTOMER RENAMED SPACE"
        self.space.save()
        detail = self.client.get(self.url("installation", self.batch.pk))
        self.assertContains(detail, "Customer-private space after handover")
        self.assertNotContains(detail, self.space.name)
        self.assertNotContains(detail, plant.label)
        self.assertNotContains(self.client.get(self.url("inventory")), plant.label)
        api.force_authenticate(self.staff)
        self.assertEqual(api.get(reverse("plantupia-care-guide", args=[plant.pk])).status_code, 404)

    def test_response_escapes_authored_data_and_serves_no_customer_upload(self):
        self.batch.space.name = '<script>alert("qa")</script> نبات'
        self.batch.space.save()
        response = self.client.get(self.url("installation", self.batch.pk))
        self.assertNotContains(response, '<script>alert("qa")</script>')
        self.assertContains(response, "&lt;script&gt;")
        template = self.client.get(self.url("template", self.batch.pk))
        self.assertIn("no-store", template["Cache-Control"])
        self.assertIn("plantupia-inventory-v1.xlsx", template["Content-Disposition"])


@override_settings(**TEST_SETTINGS)
class CompanyConcurrencyTests(CompanyFixture, TransactionTestCase):
    @skipUnlessDBFeature("has_select_for_update")
    def test_simultaneous_issue_confirmation_is_one_invitation_and_one_secret(self):
        self.ready()
        token = service.action_token(self.staff, "issue", self.batch, service.invitation_state(self.batch))
        barrier = Barrier(2)

        def issue(_):
            close_old_connections()
            try:
                staff = get_user_model().objects.get(pk=self.staff.pk)
                barrier.wait(timeout=10)
                try:
                    service.issue_invitation(staff, self.batch.pk, self.recipient, "customer_owned", token)
                    return "issued"
                except ImportProblem:
                    return "stale"
            finally:
                close_old_connections()

        with ThreadPoolExecutor(max_workers=2) as pool:
            self.assertEqual(sorted(pool.map(issue, range(2))), ["issued", "stale"])
        self.assertEqual(HandoverInvitation.objects.count(), 1)
