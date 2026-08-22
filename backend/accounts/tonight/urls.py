from django.urls import path

from .views import TonightView


urlpatterns = [
    path("tonight/", TonightView.as_view(), name="tonight"),
]
