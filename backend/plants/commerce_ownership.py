"""Exact-once private My Plants activation from a *physically evidenced* Shop delivery.

A SKU is a saleable product, not a botanical verification of the received plant.
Only plant-kind items become individual private PlantUnits, with a reproducible
order-line provenance and no inferred care tasks, photos, species confirmation
or public sharing. Returns retain historical provenance.
"""
from django.contrib.auth import get_user_model
from django.db import transaction

from . import company_services as company
from .commerce_models import (
    CommerceOrder, CommerceOrderLine, CommerceCustomerPlantSpace, CommerceDeliveredPlant,
)
from .models import PlantUnit, Space


@transaction.atomic
def activate_delivered_shop_plants(*, order_id, staff):
    """Only call from the authorized, already-locked delivery evidence flow."""
    order = CommerceOrder.objects.select_for_update().get(pk=order_id)
    if order.status != CommerceOrder.Status.DELIVERED or not hasattr(order, "fulfillment_receipt"):
        raise ValueError("Recorded delivery and retained proof are required.")
    if not order.fulfillment_receipt.delivery_proof_reference:
        raise ValueError("An independently retained delivery proof reference is required.")

    lines = list(
        CommerceOrderLine.objects.filter(order=order, sku__product__kind="plant")
        .select_related("sku__product__reference_species").order_by("pk")
    )
    if not lines:
        return []

    # Lock the customer record to serialize two concurrent independent deliveries.
    get_user_model().objects.select_for_update().get(pk=order.customer_id)
    bound = CommerceCustomerPlantSpace.objects.select_related("space").filter(
        customer_id=order.customer_id,
    ).first()
    if bound:
        space = bound.space
        if space.owner_id != order.customer_id:
            raise ValueError("Purchase space ownership does not match customer.")
    else:
        space = Space.objects.create(
            owner=order.customer,
            name="Plants from Plantupia",
            description="Your delivered plant purchases. Personalize each plant's name, location and actual photo.",
        )
        CommerceCustomerPlantSpace.objects.create(customer=order.customer, space=space)

    created = []
    for line in lines:
        product = line.sku.product
        # A staff-curated SKU reference is not physical specimen identity.
        species_name = (
            product.reference_species.scientific_name
            if product.reference_species_id else ""
        )
        for unit_index in range(1, line.quantity + 1):
            existing = CommerceDeliveredPlant.objects.filter(
                order_line=line, unit_index=unit_index,
            ).first()
            if existing:
                created.append(existing.plant_id)
                continue
            label = line.product_name_snapshot
            if line.option_snapshot:
                label += " · " + line.option_snapshot
            if line.quantity > 1:
                label += f" #{unit_index}"
            unit = PlantUnit.objects.create(
                space=space,
                label=label[:120],
                species_label=species_name[:180],
                # No verified_species or selected_species: no identity claim.
            )
            CommerceDeliveredPlant.objects.create(
                order_line=line, unit_index=unit_index, plant=unit,
            )
            company.audit(staff, unit, (
                f"Delivered physical plant from Shop order {order.pk}; SKU {line.sku_code_snapshot}; "
                "individual identity not independently verified; customer private"
            ))
            created.append(unit.pk)
    return created
