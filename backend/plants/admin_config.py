from django.contrib.admin.apps import AdminConfig


class PlantupiaAdminConfig(AdminConfig):
    default_site = "plants.technical_admin.TechnicalAdminSite"
