from django.urls import path
from .assistant import PlantAssistantView
from .views import HandoverClaimView, HandoverPreviewView
from .views import PlantEventSubmissionsView, PlantQuestionSubmissionsView
from .views import SpaceDetailView, PlantDetailView, PlantEventsView, PlantFollowUpDateView, PlantNicknameView, PlantsView, SpacesView, PlantQuestionsView, PlantQuestionResolveView, PlantCircleView

urlpatterns = [
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
