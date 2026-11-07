from django.urls import path
from .assistant import PlantAssistantView
from .commerce_api import QACommerceRequestsView, QACommerceRequestView, QACommerceCatalogView, QACommerceRequestActionView, QACommerceRFQsView, QACommerceRFQAcceptView
from .species_library import BotanicalSpeciesLibraryView, BotanicalSpeciesLibraryDetailView
from .demo_api import (DemoAccessView, InspectionPlansView, InspectionActionView, InspectionPhotoView, ServiceRequestsView,
                       ServiceRequestView, CatalogView, CatalogImageView, CartView, DemoOrdersView, DemoOrderView)
from .views import HandoverClaimView, HandoverPreviewView
from .views import PlantCareGuideView
from .views import PlantEventSubmissionsView, PlantQuestionSubmissionsView
from .views import SpaceDetailView, PlantDetailView, PlantEventsView, PlantFollowUpDateView, PlantNicknameView, PlantsView, SpacesView, PlantQuestionsView, PlantQuestionResolveView, PlantCircleView

urlpatterns = [
    path("library/species/", BotanicalSpeciesLibraryView.as_view(), name="plantupia-library-species"),
    path("library/species/<slug:key>/", BotanicalSpeciesLibraryDetailView.as_view(), name="plantupia-library-species-detail"),
    path("demo/access/", DemoAccessView.as_view(), name="plantupia-demo-access"),
    path("care-plans/", InspectionPlansView.as_view(), name="plantupia-inspection-plans"),
    path("care-plans/<uuid:plan_id>/actions/", InspectionActionView.as_view(), name="plantupia-inspection-action"),
    path("plants/<uuid:plant_id>/events/<uuid:event_id>/inspection-photo/", InspectionPhotoView.as_view(), name="plantupia-inspection-photo"),
    path("service-requests/", ServiceRequestsView.as_view(), name="plantupia-service-requests"),
    path("service-requests/<uuid:ticket_id>/", ServiceRequestView.as_view(), name="plantupia-service-request"),
    path("commerce/qa/catalog/", QACommerceCatalogView.as_view(), name="plantupia-commerce-qa-catalog"),
    path("commerce/qa/business/rfqs/", QACommerceRFQsView.as_view(), name="plantupia-commerce-b2b-qa-rfqs"),
    path("commerce/qa/business/rfqs/<uuid:rfq_id>/accept/", QACommerceRFQAcceptView.as_view(), name="plantupia-commerce-b2b-qa-accept"),
    path("commerce/qa/requests/", QACommerceRequestsView.as_view(), name="plantupia-commerce-qa-requests"),
    path("commerce/qa/requests/<uuid:order_id>/", QACommerceRequestView.as_view(), name="plantupia-commerce-qa-request"),
    path("commerce/qa/requests/<uuid:order_id>/actions/", QACommerceRequestActionView.as_view(), name="plantupia-commerce-qa-request-action"),
    path("catalog/", CatalogView.as_view(), name="plantupia-catalog"),
    path("catalog/<uuid:product_id>/image/", CatalogImageView.as_view(), name="plantupia-catalog-image"),
    path("cart/", CartView.as_view(), name="plantupia-cart"),
    path("demo/orders/", DemoOrdersView.as_view(), name="plantupia-demo-orders"),
    path("demo/orders/<uuid:order_id>/", DemoOrderView.as_view(), name="plantupia-demo-order"),
    path("plants/<uuid:plant_id>/care-guide/", PlantCareGuideView.as_view(), name="plantupia-care-guide"),
    path("handovers/preview/", HandoverPreviewView.as_view(), name="plantupia-handover-preview"),
    path("handovers/claim/", HandoverClaimView.as_view(), name="plantupia-handover-claim"),
    path("plants/<uuid:plant_id>/event-submissions/", PlantEventSubmissionsView.as_view(), name="plantupia-event-submissions"),
    path("plants/<uuid:plant_id>/question-submissions/", PlantQuestionSubmissionsView.as_view(), name="plantupia-question-submissions"),
    path("spaces/", SpacesView.as_view(), name="plantupia-spaces"),
    path("spaces/<uuid:space_id>/", SpaceDetailView.as_view(), name="plantupia-space-detail"),
    path("plants/", PlantsView.as_view(), name="plantupia-plants"),
    path("plants/<uuid:plant_id>/", PlantDetailView.as_view(), name="plantupia-plant-detail"),
    path("plants/<uuid:plant_id>/events/", PlantEventsView.as_view(), name="plantupia-events"),
    path("plants/<uuid:plant_id>/events/<uuid:event_id>/follow-up/", PlantFollowUpDateView.as_view(), name="plantupia-follow-up-date"),
    path("plants/<uuid:plant_id>/nickname/", PlantNicknameView.as_view(), name="plantupia-nickname"),
    path("plants/<uuid:plant_id>/questions/", PlantQuestionsView.as_view(), name="plantupia-questions"),
    path("plants/<uuid:plant_id>/questions/<uuid:discussion_id>/resolve/", PlantQuestionResolveView.as_view(), name="plantupia-resolve"),
    path("circle/", PlantCircleView.as_view(), name="plantupia-circle"),
    path("assistant/ask/", PlantAssistantView.as_view(), name="plantupia-assistant-ask"),
]
