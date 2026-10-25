from django.urls import path
from . import company_views as views

app_name = "company"
urlpatterns = [
    path("", views.overview, name="overview"),
    path("login/", views.login_view, name="login"),
    path("logout/", views.logout_view, name="logout"),
    path("installations/", views.installations, name="installations"),
    path("installations/new/", views.installation_edit, name="installation-create"),
    path("installations/<uuid:batch_id>/", views.installation_detail, name="installation"),
    path("installations/<uuid:batch_id>/edit/", views.installation_edit, name="installation-edit"),
    path("installations/<uuid:batch_id>/review/", views.installation_review, name="installation-review"),
    path("installations/<uuid:batch_id>/import/", views.import_workspace, name="import"),
    path("installations/<uuid:batch_id>/template/", views.import_template, name="template"),
    path("installations/<uuid:batch_id>/plants/new/", views.plant_edit, name="plant-create"),
    path("installations/<uuid:batch_id>/plants/<uuid:plant_id>/", views.plant_edit, name="plant-edit"),
    path("inventory/", views.inventory, name="inventory"),
    path("care/", views.care_library, name="care"),
    path("care/species/new/", views.species_create, name="species-create"),
    path("care/new/", views.guide_edit, name="guide-create"),
    path("care/<int:guide_id>/", views.guide_detail, name="guide"),
    path("care/<int:guide_id>/edit/", views.guide_edit, name="guide-edit"),
    path("handovers/", views.handovers, name="handovers"),
    path("handovers/issue/<uuid:batch_id>/", views.handover_issue, name="handover-issue"),
    path("handovers/<uuid:invitation_id>/revoke/", views.handover_revoke, name="handover-revoke"),
]
