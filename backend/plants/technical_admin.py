"""Keep company employee roles out of the inherited unscoped technical Admin.

Existing technical staff (without company access) and superusers retain their
previous Admin permissions. Company portal permissions must not accidentally
expose another customer's Space through an inherited ModelAdmin queryset.
"""
from django.contrib.admin import AdminSite
from django.contrib.admin.forms import AdminAuthenticationForm
from django.core.exceptions import ValidationError


def company_employee(user):
    return not user.is_superuser and user.has_perm("plants.access_company_dashboard")


class TechnicalAuthenticationForm(AdminAuthenticationForm):
    def confirm_login_allowed(self, user):
        super().confirm_login_allowed(user)
        if company_employee(user):
            raise ValidationError("Use the Plantupia Company Dashboard for employee operations.")


class TechnicalAdminSite(AdminSite):
    login_form = TechnicalAuthenticationForm

    def has_permission(self, request):
        return super().has_permission(request) and not company_employee(request.user)
