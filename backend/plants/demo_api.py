"""Authenticated native contracts for the isolated, noncommercial CEO QA journey."""
import base64
from django.contrib.auth import get_user_model
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from . import demo_services as service
from .models import CareInspectionPlan, ServiceRequest, CatalogProduct, DemoCartLine, DemoOrder
from .views import accessible_plants


class DemoView(APIView):
    permission_classes = [IsAuthenticated]

    def initial(self, request, *args, **kwargs):
        service.require_demo()
        super().initial(request, *args, **kwargs)

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        response["Cache-Control"] = "private, no-store"
        return response


class MutationInput(serializers.Serializer):
    client_id = serializers.UUIDField()


def parsed(cls, request):
    serializer = cls(data=request.data)
    serializer.is_valid(raise_exception=True)
    return serializer.validated_data


class AccessInput(MutationInput):
    offer = serializers.SlugField(max_length=40)
    confirm_no_payment = serializers.BooleanField()

    def validate_confirm_no_payment(self, value):
        if value is not True:
            raise serializers.ValidationError("Confirm this is short-lived QA access with no payment.")
        return value


class DemoAccessView(DemoView):
    def get(self, request):
        return Response(service.access_json(request.user))

    def post(self, request):
        data = parsed(AccessInput, request)
        return Response(service.activate(request.user, data["offer"], data["client_id"]))


class InspectionInput(MutationInput):
    action = serializers.ChoiceField(choices=("opt_in", "decline", "complete", "skip", "reschedule"))
    revision = serializers.IntegerField(min_value=0)
    note = serializers.CharField(max_length=2000, required=False, allow_blank=True)
    next_due_on = serializers.DateField(required=False)

    def validate(self, data):
        if data["action"] == "reschedule" and "next_due_on" not in data:
            raise serializers.ValidationError("Choose the next inspection date.")
        return data


class InspectionPlansView(DemoView):
    def get(self, request):
        plans = CareInspectionPlan.objects.filter(plant__in=accessible_plants(request.user), approved_at__isnull=False)
        if request.query_params.get("plant_id"):
            try:
                plant_id = serializers.UUIDField().run_validation(request.query_params["plant_id"])
            except serializers.ValidationError:
                raise serializers.ValidationError("Invalid plant identity.")
            plans = plans.filter(plant_id=plant_id)
        plans = plans.select_related("plant", "guide", "approved_by").order_by("start_on", "pk")[:200]
        return Response({"results": [service.plan_json(p, request.user) for p in plans],
                         "notice": "Optional inspection prompts. No watering or measurements are inferred."})


class InspectionActionView(DemoView):
    def post(self, request, plan_id):
        return Response(service.plan_action(request.user, plan_id, parsed(InspectionInput, request)))


class SupportInput(MutationInput):
    plant_id = serializers.UUIDField()
    subject = serializers.CharField(max_length=140)
    body = serializers.CharField(max_length=2000)
    share_context = serializers.BooleanField()
    share_placement = serializers.BooleanField(default=False)


class ReplyInput(MutationInput):
    body = serializers.CharField(max_length=2000)


def ticket_json(ticket, detail=False):
    result = {"id": str(ticket.pk), "subject": ticket.subject, "plant_id": str(ticket.plant_id),
              "plant_label": ticket.shared_plant_label, "shared_context": ticket.shared_context,
              "status": ticket.status, "created_at": ticket.created_at.isoformat(),
              "assigned_staff": ticket.assigned_to.username if ticket.assigned_to_id else None}
    if detail:
        result["messages"] = [{"id": m.pk, "body": m.body, "sender": m.sender.username,
                                "from_staff": m.sender_id != ticket.customer_id,
                                "created_at": m.created_at.isoformat()}
                               for m in ticket.conversation.messages.select_related("sender").order_by("id")[:200]]
        result["notice"] = "Only the explicitly shared request is visible to the support team. No response time is promised."
    return result


class ServiceRequestsView(DemoView):
    def get(self, request):
        tickets = ServiceRequest.objects.filter(customer=request.user).select_related("assigned_to").order_by("-created_at")[:100]
        return Response({"results": [ticket_json(t) for t in tickets]})

    def post(self, request):
        return Response(service.create_request(request.user, parsed(SupportInput, request)))


class ServiceRequestView(DemoView):
    def get(self, request, ticket_id):
        ticket = get_object_or_404(ServiceRequest.objects.select_related("assigned_to", "conversation"), pk=ticket_id, customer=request.user)
        # Past shared records remain readable after expiry; further requests/replies require access.
        return Response(ticket_json(ticket, True))

    def post(self, request, ticket_id):
        data = parsed(ReplyInput, request)
        return Response(service.support_reply(request.user, ticket_id, data["body"], data["client_id"]))


class CatalogView(DemoView):
    def get(self, request):
        return Response({"results": [service.product_json(p) for p in CatalogProduct.objects.filter(active=True).order_by("name")[:100]],
                         "notice": "FICTIONAL QA products. Provisional prices. No commercial offer."})


class CatalogImageView(DemoView):
    def get(self, request, product_id):
        product = get_object_or_404(CatalogProduct, pk=product_id, active=True)
        if not product.image:
            from django.http import Http404
            raise Http404
        with product.image.open("rb") as source:
            photo = source.read(2 * 1024 * 1024 + 1)
        if len(photo) > 2 * 1024 * 1024:
            raise serializers.ValidationError("The catalog photograph is too large to display.")
        return Response({"image_base64": base64.b64encode(photo).decode("ascii")})


class CartInput(serializers.Serializer):
    product_id = serializers.UUIDField()
    quantity = serializers.IntegerField(min_value=0, max_value=10)


class CartView(DemoView):
    def get(self, request):
        return Response(service.cart_json(request.user))

    @transaction.atomic
    def post(self, request):
        service.customer(request.user)
        data = parsed(CartInput, request)
        get_user_model().objects.select_for_update().get(pk=request.user.pk)
        product = get_object_or_404(CatalogProduct.objects.select_for_update(), pk=data["product_id"])
        if data["quantity"] == 0:
            DemoCartLine.objects.filter(customer=request.user, product=product).delete()
        else:
            if not product.active or data["quantity"] > product.qa_stock:
                raise service.DemoProblem("This QA quantity is unavailable.")
            if not DemoCartLine.objects.filter(customer=request.user, product=product).exists() and DemoCartLine.objects.filter(customer=request.user).count() >= 20:
                raise serializers.ValidationError("The cart supports at most twenty different QA items.")
            DemoCartLine.objects.update_or_create(customer=request.user, product=product, defaults={"quantity": data["quantity"]})
        return Response(service.cart_json(request.user))


class CheckoutInput(MutationInput):
    cart_fingerprint = serializers.CharField(max_length=64, min_length=64)
    confirm_no_charge = serializers.BooleanField()

    def validate_confirm_no_charge(self, value):
        if value is not True:
            raise serializers.ValidationError("Confirm that this is a QA order with no charge or delivery.")
        return value


def order_json(order):
    return {"id": str(order.pk), "status": order.status, "created_at": order.created_at.isoformat(),
            "total_provisional": str(order.total_provisional), "charged": False, "delivery_initiated": False,
            "installation_id": str(order.installation_id) if order.installation_id else None,
            "lines": [{"name": l.name_snapshot, "quantity": l.quantity, "unit_price_provisional": str(l.unit_price_provisional)} for l in order.lines.all()],
            "notice": "Demo order — no charge or delivery initiated"}


class DemoOrdersView(DemoView):
    def get(self, request):
        return Response({"results": [order_json(o) for o in DemoOrder.objects.filter(customer=request.user).prefetch_related("lines").order_by("-created_at")[:100]]})

    def post(self, request):
        return Response(service.checkout(request.user, parsed(CheckoutInput, request)))
