"""G2 isolated pilot contract: explicit account-bound, one-time activation."""
import os
from datetime import timedelta
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from .handovers import HandoverError, issue_pilot_invitation
from .models import (
    HandoverInvitation, InstallationBatch, InstallationManifestRow,
    PlantEvent, PlantUnit, Space, SpaceMember,
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
