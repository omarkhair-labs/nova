from django import forms
from django.contrib.auth import get_user_model
from django.contrib.auth.forms import AuthenticationForm
from django.core.validators import MaxLengthValidator

from .company_services import ACCESS, PUBLISH
from .media import sanitize_public_plant_photo
from .models import BotanicalSpecies, CareGuideVersion, HandoverInvitation, PlantUnit


class StaffAuthenticationForm(AuthenticationForm):
    def confirm_login_allowed(self, user):
        super().confirm_login_allowed(user)
        if not user.is_staff or not user.has_perms(ACCESS):
            raise forms.ValidationError("Company access is not available for this account.", code="invalid_login")


class InstallationForm(forms.Form):
    reference_code = forms.CharField(max_length=80, label="Installation reference")
    name = forms.CharField(max_length=140, label="Space / site name")
    description = forms.CharField(required=False, max_length=4000, widget=forms.Textarea(attrs={"rows": 3}))
    location_note = forms.CharField(required=False, max_length=200, help_text="Private placement context. Avoid unnecessary customer identifying details in QA.")


class PlantForm(forms.ModelForm):
    source_key = forms.CharField(max_length=80, help_text="Stable internal key within this installation. A replacement needs a new key and UUID.")

    class Meta:
        model = PlantUnit
        fields = ("source_key", "label", "unit_type", "species_label", "placement_note", "cover_photo")
        labels = {
            "species_label": "Species label (unverified text)",
            "cover_photo": "Photo of this actual plant",
        }
        help_texts = {
            "cover_photo": "Optional authentic photo of this installed plant. JPEG, PNG or WebP, up to 10 MB. Camera metadata is removed.",
        }

    def clean_cover_photo(self):
        photo = self.cleaned_data.get("cover_photo")
        # Existing uploaded media is retained when no replacement is supplied.
        if not photo or not hasattr(photo, "content_type"):
            return photo
        try:
            return sanitize_public_plant_photo(photo)
        except ValueError as exc:
            raise forms.ValidationError(str(exc)) from exc


class SpeciesForm(forms.ModelForm):
    class Meta:
        model = BotanicalSpecies
        fields = ("key", "scientific_name")


class GuideForm(forms.ModelForm):
    class Meta:
        model = CareGuideVersion
        fields = ("species", "version", "source_url", "source_reference", "instructions", "context_notes", "assigned_reviewer")
        widgets = {"instructions": forms.Textarea(attrs={"rows": 7}), "context_notes": forms.Textarea(attrs={"rows": 3})}
        help_texts = {"assigned_reviewer": "Assignment is not botanical approval. Only authorized active reviewers are listed."}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        ids = [u.pk for u in get_user_model().objects.filter(is_active=True, is_staff=True)
               if u.has_perms((*ACCESS, *PUBLISH))]
        self.fields["assigned_reviewer"].queryset = get_user_model().objects.filter(pk__in=ids).order_by("username")
        self.fields["assigned_reviewer"].label_from_instance = lambda user: user.username
        self.fields["source_url"].label = "HTTPS source URL"
        for name in ("instructions", "context_notes"):
            self.fields[name].max_length = 8000
            self.fields[name].validators.append(MaxLengthValidator(8000))
            self.fields[name].widget.attrs["maxlength"] = 8000


class ReviewForm(forms.Form):
    review_token = forms.CharField(widget=forms.HiddenInput)
    reviewed = forms.BooleanField(label="I reviewed this exact record and confirm this action")


class UploadForm(forms.Form):
    file = forms.FileField(label="Version 1 XLSX or inventory-only UTF-8 CSV", help_text="2 MiB maximum. Download this installation's template first.")


class ImportApprovalForm(forms.Form):
    preview = forms.CharField(widget=forms.HiddenInput)
    confirmed = forms.BooleanField(label="I reviewed every row and the exact destination; apply this import atomically")
    identities_confirmed = forms.BooleanField(required=False, label="For every supplied verified species, I independently reviewed the identity evidence and confirm it")


class InvitationForm(forms.Form):
    recipient_username = forms.CharField(max_length=150, label="Exact recipient username", help_text="An existing active, non-staff account. No account or invitation is created by a search.")
    mode = forms.ChoiceField(choices=[("", "Choose an approved QA ownership mode")] + list(HandoverInvitation.Mode.choices))
    approved_qa_policy = forms.BooleanField(label="This is isolated QA; the recipient and selected ownership mode are explicitly approved for this test")
    replace_existing = forms.BooleanField(required=False, label="Reissue: revoke any unused previous invitation for this installation")
    review_token = forms.CharField(widget=forms.HiddenInput)

    def clean_recipient_username(self):
        user = get_user_model().objects.filter(username=self.cleaned_data["recipient_username"], is_active=True, is_staff=False).first()
        if not user:
            raise forms.ValidationError("No eligible existing recipient matches this exact username.")
        return user
