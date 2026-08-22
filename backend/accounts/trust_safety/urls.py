from django.urls import path

from . import BlockedUsersView, DeleteAccountView, UserBlockView, UserReportView


urlpatterns = [
    path("auth/account/delete/", DeleteAccountView.as_view(), name="account-delete"),
    path("auth/blocks/", BlockedUsersView.as_view(), name="blocked-users"),
    path("people/<str:username>/block/", UserBlockView.as_view(), name="person-block"),
    path("people/<str:username>/report/", UserReportView.as_view(), name="person-report"),
]
