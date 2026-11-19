// Private preview only. Independent supplied studies, not completed installation or growth evidence.
export const privateSceneOverrides = [
  {
    visual: "/api/private-arrival/arrival.webp",
    alt: "Planting proposal for a commercial courtyard with palms in a circular planter.",
    detail: "Pixels Mall / MT Architects proposal, page 3. A design visualization for a commercial arrival space.",
    markerLabels: ["The landmark", "The threshold", "The materials"],
    markerDescriptions: [
      "A cluster of palms gives the courtyard a visual focal point.",
      "The planting is positioned near the architectural entrance.",
      "The circular planter relates to the curves of the building.",
    ],
  },
  {
    visual: "/api/private-arrival/hospitality.webp",
    alt: "Hospitality catalogue illustration of greenery integrated into a dining room.",
    detail: "Hospitality greenery catalogue, page 1. An illustrative reference for the relationship between plants and interiors.",
    markerLabels: ["Living material", "Vertical planting", "A softer boundary"],
    markerDescriptions: [
      "Leaves and planted surfaces introduce a living layer to the interior.",
      "Planting wraps the columns and connects to the architecture.",
      "A linear planted edge softens the boundary of the dining area.",
    ],
  },
  {
    visual: "/api/private-arrival/residence.webp",
    alt: "Residential terrace planting visualization with lounge seating and a planted wall.",
    detail: "Villa 7 proposal, page 18, Shelves & Green Wall. A design visualization, not a documented maintenance record or a time-lapse.",
    markerLabels: ["Notice", "Return", "Remember"],
    markerDescriptions: [
      "A planted lounge is a space for repeated everyday use.",
      "Living planting calls for ongoing attention after installation.",
      "The image illustrates a proposed setting, not a record of growth.",
    ],
  },
] as const;
