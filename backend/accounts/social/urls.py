from django.urls import path

from ..social_paging import (
    FollowersView,
    FollowingView,
    PaginatedPeopleView,
    PaginatedPersonPostsView,
    PaginatedPersonRepostsView,
)
from ..views import FollowView, PersonView


urlpatterns = [
    path("people/", PaginatedPeopleView.as_view(), name="people"),
    path("people/<str:username>/", PersonView.as_view(), name="person-detail"),
    path(
        "people/<str:username>/posts/",
        PaginatedPersonPostsView.as_view(),
        name="person-posts",
    ),
    path(
        "people/<str:username>/reposts/",
        PaginatedPersonRepostsView.as_view(),
        name="person-reposts",
    ),
    path(
        "people/<str:username>/followers/",
        FollowersView.as_view(),
        name="person-followers",
    ),
    path(
        "people/<str:username>/following/",
        FollowingView.as_view(),
        name="person-following-list",
    ),
    path("people/<str:username>/follow/", FollowView.as_view(), name="person-follow"),
]
