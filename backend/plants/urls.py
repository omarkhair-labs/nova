from django.urls import path
from .views import PlantEventsView, PlantNicknameView, PlantsView, SpacesView, PlantQuestionsView, PlantQuestionResolveView, PlantCircleView

urlpatterns = [
    path("spaces/", SpacesView.as_view(), name="plantupia-spaces"),
    path("plants/", PlantsView.as_view(), name="plantupia-plants"),
    path("plants/<uuid:plant_id>/events/", PlantEventsView.as_view(), name="plantupia-events"),
    path("plants/<uuid:plant_id>/nickname/", PlantNicknameView.as_view(), name="plantupia-nickname"),
    path("plants/<uuid:plant_id>/questions/", PlantQuestionsView.as_view(), name="plantupia-questions"),
    path("plants/<uuid:plant_id>/questions/<uuid:discussion_id>/resolve/", PlantQuestionResolveView.as_view(), name="plantupia-resolve"),
    path("circle/", PlantCircleView.as_view(), name="plantupia-circle"),
]
