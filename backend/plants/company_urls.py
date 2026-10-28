from django.urls import path
from . import company_views as views
from . import company_demo as demo
from . import catalog_bulk_import as catalog_import

app_name = "company"
urlpatterns = [
    path("plans/", demo.plans, name="plans"),
    path("plans/new/", demo.plan_create, name="plan-create"),
    path("plans/<uuid:plan_id>/", demo.plan_review, name="plan-review"),
    path("support/", demo.support, name="support"),
    path("support/<uuid:ticket_id>/", demo.support_detail, name="support-detail"),
    path("catalog/", demo.catalog, name="catalog"),
    path("catalog/new/", demo.product_edit, name="product-create"),
    path("catalog/import/", catalog_import.catalog_zip_import, name="catalog-zip-import"),
    path("catalog/<uuid:product_id>/", demo.product_edit, name="product-edit"),
    path("orders/", demo.orders, name="orders"),
    path("orders/<uuid:order_id>/", demo.order_detail, name="order-detail"),
    path("demo-access/", demo.access, name="demo-access"),
    path("demo-access/<slug:offer_key>/", demo.offer_edit, name="offer-edit"),
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
