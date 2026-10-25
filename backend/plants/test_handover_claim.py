"""G2 isolated pilot contract: explicit account-bound, one-time activation."""
import os
from datetime import timedelta
from unittest.mock import patch
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
import uuid

from django.contrib.auth import get_user_model
from django.test import TestCase, TransactionTestCase, override_settings, skipUnlessDBFeature
from django.db import close_old_connections
from django.core.exceptions import ValidationError
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .handovers import HandoverError, issue_pilot_invitation
from .models import (
    HandoverInvitation, InstallationBatch, InstallationManifestRow,
    PlantEvent, PlantUnit, Space, SpaceMember,
    BotanicalSpecies, CareGuideVersion,
)


class HandoverPilotTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.staff = User.objects.create_superuser(
            username="handover_staff", email="handover_staff@example.test",
            password="test-only",
        )
        self.customer = User.objects.create_user(
            username="handover_customer", email="handover_customer@example.test",
            password="test-only",
        )
        self.stranger = User.objects.create_user(
            username="handover_stranger", email="handover_stranger@example.test",
            password="test-only",
        )
        self.space = Space.objects.create(
            owner=self.staff, name="Simulated seven-plant installation",
        )
        self.batch = InstallationBatch.objects.create(
            reference_code="QA-DELIVERY-0001", space=self.space, prepared_by=self.staff,
        )
        self.plants = [
            PlantUnit.objects.create(space=self.space, label=f"QA living plant {i}")
            for i in range(7)
        ]
        for i, plant in enumerate(self.plants):
            InstallationManifestRow.objects.create(
                batch=self.batch, plant=plant, source_key=f"QA-{i:02}",
            )
        self.batch.status = InstallationBatch.Status.READY
        self.batch.reviewed_by = self.staff
        self.batch.reviewed_at = timezone.now()
        self.batch.full_clean()
        self.batch.save(update_fields=("status", "reviewed_by", "reviewed_at"))
        self.api = APIClient()
        self.preview = reverse("plantupia-handover-preview")
        self.claim = reverse("plantupia-handover-claim")
        enabled = patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "1"})
        enabled.start()
        self.addCleanup(enabled.stop)

    def issue(self, mode=HandoverInvitation.Mode.CUSTOMER_OWNED):
        token, invitation = issue_pilot_invitation(
            batch_id=self.batch.pk, recipient=self.customer,
            staff=self.staff, mode=mode,
        )
        self.assertNotEqual(token, invitation.token_digest)
        self.assertNotIn(token, str(invitation))
        return token, invitation

    def test_feature_is_off_by_default_and_does_not_claim_or_preview(self):
        token, _ = self.issue()
        self.api.force_authenticate(self.customer)
        with patch.dict(os.environ, {"PLANTUPIA_HANDOVER_QA_ENABLED": "0"}):
            self.assertEqual(
                self.api.post(self.preview, {"invitation": token}, format="json").status_code,
                503,
            )
            self.assertEqual(
                self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code,
                503,
            )
            with self.assertRaises(HandoverError):
                self.issue()
        self.space.refresh_from_db()
        self.assertEqual(self.space.owner_id, self.staff.id)
        self.assertFalse(HandoverInvitation.objects.get().claimed_at)

    def test_unauthenticated_wrong_account_and_bad_token_never_reveal_inventory(self):
        token, _ = self.issue()
        self.assertEqual(
            self.api.post(self.preview, {"invitation": token}, format="json").status_code,
            401,
        )
        self.api.force_authenticate(self.stranger)
        for uri in (self.preview, self.claim):
            payload = {"invitation": token, "confirm": True}
            resp = self.api.post(uri, payload, format="json")
            self.assertEqual(resp.status_code, 404, resp.content)
            self.assertNotIn(self.space.name, str(resp.content))
        self.api.force_authenticate(self.customer)
        self.assertEqual(
            self.api.post(self.preview, {"invitation": "bad"}, format="json").status_code,
            404,
        )
        self.assertEqual(
            self.api.post(self.claim, {"invitation": token}, format="json").status_code,
            400,
        )
        self.assertEqual(InstallationBatch.objects.get().status, InstallationBatch.Status.READY)

    def test_seven_real_plant_identities_survive_sale_claim_and_exact_replay(self):
        token, invite = self.issue()
        self.api.force_authenticate(self.customer)
        preview = self.api.post(self.preview, {"invitation": token}, format="json")
        self.assertEqual(preview.status_code, 200, preview.content)
        self.assertEqual(preview.json()["state"], "ready")
        self.assertEqual(preview.json()["plant_count"], 7)
        self.assertNotIn("plants", preview.json())
        before = set(PlantUnit.objects.values_list("id", flat=True))
        claimed = self.api.post(
            self.claim, {"invitation": token, "confirm": True}, format="json",
        )
        self.assertEqual(claimed.status_code, 200, claimed.content)
        self.assertEqual(claimed.json()["state"], "claimed")
        self.assertEqual(claimed.json()["space_id"], str(self.space.pk))
        self.assertEqual(
            set(PlantUnit.objects.values_list("id", flat=True)), before,
        )
        self.assertEqual(PlantUnit.objects.count(), 7)
        self.assertEqual(InstallationManifestRow.objects.count(), 7)
        self.space.refresh_from_db()
        invite.refresh_from_db()
        self.batch.refresh_from_db()
        self.assertEqual(self.space.owner_id, self.customer.id)
        self.assertEqual(invite.claimed_by_id, self.customer.id)
        self.assertEqual(self.batch.status, InstallationBatch.Status.CLAIMED)
        self.assertEqual(
            self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").json(),
            claimed.json(),
        )
        self.assertEqual(len(self.api.get(reverse("plantupia-plants")).json()["results"]), 7)
        self.api.force_authenticate(self.staff)
        self.assertEqual(self.api.get(reverse("plantupia-plants")).json()["results"], [])
        self.api.force_authenticate(self.stranger)
        self.assertEqual(self.api.get(reverse("plantupia-plants")).json()["results"], [])
        self.assertEqual(
            self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code,
            404,
        )

    def test_managed_claim_keeps_company_owner_and_grants_specific_carer_role(self):
        token, _ = self.issue(HandoverInvitation.Mode.MANAGED)
        self.api.force_authenticate(self.customer)
        response = self.api.post(
            self.claim, {"invitation": token, "confirm": True}, format="json",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.space.refresh_from_db()
        self.assertEqual(self.space.owner_id, self.staff.id)
        member = SpaceMember.objects.get(space=self.space, user=self.customer)
        self.assertEqual(member.role, SpaceMember.Role.CARER)
        self.assertEqual(len(self.api.get(reverse("plantupia-plants")).json()["results"]), 7)
        event = self.api.post(
            reverse("plantupia-events", args=[self.plants[0].pk]),
            {"kind": "observation", "note": "QA observed, no watering",
             "observed_at": timezone.now().isoformat()},
            format="json",
        )
        self.assertEqual(event.status_code, 201, event.content)
        self.assertEqual(PlantEvent.objects.count(), 1)
        member.delete()
        self.assertEqual(
            self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code,
            409,
        )
        self.assertEqual(self.api.get(reverse("plantupia-plants")).json()["results"], [])

    def test_expired_revoked_and_reissued_tokens_cannot_claim(self):
        token, invite = self.issue()
        self.api.force_authenticate(self.customer)
        HandoverInvitation.objects.filter(pk=invite.pk).update(
            expires_at=timezone.now() - timedelta(seconds=1),
        )
        self.assertEqual(
            self.api.post(self.preview, {"invitation": token}, format="json").status_code,
            410,
        )
        self.assertEqual(
            self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code,
            410,
        )
        new_token, newer = self.issue()
        invite.refresh_from_db()
        self.assertIsNotNone(invite.revoked_at)
        self.assertEqual(
            self.api.post(self.preview, {"invitation": token}, format="json").status_code,
            404,
        )
        self.assertEqual(
            self.api.post(self.preview, {"invitation": new_token}, format="json").status_code,
            200,
        )
        HandoverInvitation.objects.filter(pk=newer.pk).update(revoked_at=timezone.now())
        self.assertEqual(
            self.api.post(self.claim, {"invitation": new_token, "confirm": True}, format="json").status_code,
            404,
        )
        self.space.refresh_from_db()
        self.assertEqual(self.space.owner_id, self.staff.id)

    def test_cannot_issue_before_ready_or_to_staff(self):
        self.batch.status = InstallationBatch.Status.DRAFT
        self.batch.save(update_fields=("status",))
        with self.assertRaises(HandoverError):
            self.issue()
        self.batch.status = InstallationBatch.Status.READY
        self.batch.save(update_fields=("status",))
        with self.assertRaises(HandoverError):
            issue_pilot_invitation(
                batch_id=self.batch.pk, recipient=self.staff,
                staff=self.staff, mode=HandoverInvitation.Mode.CUSTOMER_OWNED,
            )
        self.assertEqual(HandoverInvitation.objects.count(), 0)

    @override_settings(IS_PRODUCTION=True)
    def test_production_refuses_even_with_qa_flag_set(self):
        with self.assertRaises(HandoverError):
            self.issue()

    def test_extra_unmanifested_plant_blocks_issue_and_claim(self):
        token, _ = self.issue()
        PlantUnit.objects.create(space=self.space, label="Not reviewed")
        with self.assertRaises(HandoverError):
            self.issue()
        self.api.force_authenticate(self.customer)
        self.assertEqual(self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code, 409)
        self.space.refresh_from_db()
        self.assertEqual(self.space.owner_id, self.staff.pk)

    def test_revoked_membership_blocks_claimed_preview_too(self):
        token, _ = self.issue(HandoverInvitation.Mode.MANAGED)
        self.api.force_authenticate(self.customer)
        self.assertEqual(self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json").status_code, 200)
        SpaceMember.objects.filter(space=self.space, user=self.customer).delete()
        result = self.api.post(self.preview, {"invitation": token}, format="json")
        self.assertEqual(result.status_code, 409)
        self.assertNotIn(self.space.name, str(result.content))

    def test_complete_seven_plant_customer_care_journey_and_private_history(self):
        # All advice here is explicitly synthetic fixture content, never a botanical claim.
        species = BotanicalSpecies.objects.create(key="qa-species", scientific_name="Synthetic QA identity")
        guide = CareGuideVersion.objects.create(
            species=species, version=1, source_url="https://example.test/qa-guide",
            source_reference="Synthetic fixture only", instructions="QA inspection text, not real care advice.",
            context_notes="Use recorded placement, never inferred measurements.", reviewer=self.staff,
            reviewed_at=timezone.now(), published=True,
        )
        plant = self.plants[0]
        plant.verified_species = species
        plant.placement_note = "QA north window"
        plant.save()
        token, _ = self.issue()
        self.api.force_authenticate(self.customer)
        self.assertEqual(self.api.get(reverse("plantupia-spaces")).json()["results"], [])
        preview = self.api.post(self.preview, {"invitation": token}, format="json").json()
        self.assertEqual(set(preview), {"state", "plant_count", "space_label", "ownership_mode"})
        claim = self.api.post(self.claim, {"invitation": token, "confirm": True}, format="json")
        self.assertEqual(claim.status_code, 200)
        inventory = self.api.get(reverse("plantupia-plants"), {"space": claim.json()["space_id"]}).json()["results"]
        self.assertEqual({row["id"] for row in inventory}, {str(p.pk) for p in self.plants})
        self.assertEqual({row["label"] for row in inventory}, {p.label for p in self.plants})
        guide_url = reverse("plantupia-care-guide", args=[plant.pk])
        response = self.api.get(guide_url)
        self.assertEqual(response.json()["guide"]["version"], 1)
        self.assertEqual(response.json()["placement"], plant.placement_note)
        self.assertEqual(self.api.get(reverse("plantupia-care-guide", args=[self.plants[1].pk])).json()["state"], "unverified")
        self.assertEqual(PlantEvent.objects.count(), 0)  # Reading advice is never a care event.
        event_url = reverse("plantupia-event-submissions", args=[plant.pk])
        payload = {"client_submission_id": str(uuid.uuid4()), "kind": "observation",
                   "note": "Synthetic inspection; did not water", "observed_at": timezone.now().isoformat(),
                   "follow_up_on": (timezone.localdate() + timedelta(days=3)).isoformat()}
        recorded = self.api.post(event_url, payload, format="json")
        self.assertEqual(recorded.status_code, 201, recorded.content)
        # Lost acknowledgement / app restart: exact submission is replayed via a fresh client.
        fresh = APIClient()
        fresh.force_authenticate(self.customer)
        self.assertEqual(fresh.post(event_url, payload, format="json").json(), recorded.json())
        history_url = reverse("plantupia-events", args=[plant.pk])
        self.assertEqual(fresh.get(history_url, {"pending_followups": "1"}).json()["results"][0]["id"], recorded.json()["id"])
        outcome = {"client_submission_id": str(uuid.uuid4()), "kind": "outcome",
                   "follow_up_for": recorded.json()["id"], "note": "Synthetic observed outcome",
                   "observed_at": timezone.now().isoformat()}
        result = fresh.post(event_url, outcome, format="json")
        self.assertEqual(result.status_code, 201, result.content)
        history = fresh.get(history_url).json()["results"]
        self.assertEqual(len(history), 2)
        self.assertEqual({e["id"] for e in history}, {recorded.json()["id"], result.json()["id"]})
        self.assertEqual(result.json()["follow_up_for"], recorded.json()["id"])
        self.assertEqual(fresh.get(history_url, {"pending_followups": "1"}).json()["results"], [])
        self.assertEqual(fresh.get(reverse("plantupia-circle")).json()["results"], [])
        fresh.force_authenticate(self.stranger)
        for url in (history_url, guide_url):
            self.assertEqual(fresh.get(url).status_code, 404)
        guide.instructions = "Changed advice"
        with self.assertRaises(ValidationError):
            guide.full_clean()

    @override_settings(STORAGES={"default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
                                "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}})
    def test_admin_issues_once_reissues_revokes_and_never_persists_secret(self):
        self.client.force_login(self.staff)
        url = reverse("admin:plants_handoverinvitation_issue")
        data = {"batch": self.batch.pk, "recipient": self.customer.pk,
                "mode": "customer_owned", "approved_qa_policy": "on"}
        response = self.client.post(url, data)
        self.assertEqual(response.status_code, 200)
        code = response.context["raw_code"]
        self.assertTrue(code)
        self.assertIn("no-store", response["Cache-Control"])
        self.assertNotIn(code, str(list(HandoverInvitation.objects.values())))
        self.assertIsNone(self.client.get(url).context["raw_code"])
        self.client.post(url, data)
        self.assertIsNotNone(HandoverInvitation.objects.order_by("created_at").first().revoked_at)
        self.client.force_login(self.customer)
        self.assertEqual(self.client.get(url).status_code, 302)


class HandoverConcurrentClaimsTests(TransactionTestCase):
    setUp = HandoverPilotTests.setUp
    issue = HandoverPilotTests.issue

    @skipUnlessDBFeature("has_select_for_update")
    def test_simultaneous_claims_return_same_original_seven_plants(self):
        token, _ = self.issue()
        barrier = Barrier(2)

        def claim():
            close_old_connections()
            try:
                api = APIClient()
                api.force_authenticate(get_user_model().objects.get(pk=self.customer.pk))
                barrier.wait(timeout=10)
                result = api.post(self.claim, {"invitation": token, "confirm": True}, format="json")
                return result.status_code, result.json()
            finally:
                close_old_connections()

        with ThreadPoolExecutor(max_workers=2) as pool:
            responses = list(pool.map(lambda _: claim(), range(2)))
        self.assertEqual([r[0] for r in responses], [200, 200], responses)
        self.assertEqual(responses[0][1], responses[1][1])
        self.assertEqual(PlantUnit.objects.count(), 7)
        self.assertEqual(HandoverInvitation.objects.filter(claimed_by=self.customer).count(), 1)
