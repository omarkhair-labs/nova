from django.urls import path
from .views import PlantEventsView, PlantNicknameView, PlantsView, SpacesView

urlpatterns = [
    path("spaces/", SpacesView.as_view(), name="plantupia-spaces"),
    path("plants/", PlantsView.as_view(), name="plantupia-plants"),
    path("plants/<uuid:plant_id>/events/", PlantEventsView.as_view(), name="plantupia-events"),
    path("plants/<uuid:plant_id>/nickname/", PlantNicknameView.as_view(), name="plantupia-nickname"),
]
