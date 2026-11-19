// Private preview only. Independent supplied studies, not completed installation or growth evidence.
export const privateSceneOverrides = [
  {
    visual: "/api/private-arrival/home-pixels-courtyard.webp",
    alt: "Planting proposal for a commercial courtyard with palms in a circular planter.",
    detail: "Pixels Mall / MT Architects proposal, page 3. A design visualization for a commercial courtyard.",
    markerLabels: ["The landmark", "The threshold", "The materials"],
    markerDescriptions: [
      "A cluster of palms gives the courtyard a visual focal point.",
      "The planting is positioned near the architectural entrance.",
      "The circular planter relates to the curves of the building.",
    ],
  },
  {
    visual: "/api/private-arrival/home-audi-lounge.webp",
    alt: "Hospitality catalogue illustration of greenery integrated into a dining room.",
    detail: "Audi Cairo Living Interiors, page 5, The Lounge. A design proposal for planting within the showroom environment.",
    markerLabels: ["Living material", "Vertical planting", "A softer boundary"],
    markerDescriptions: [
      "Leaves and planted surfaces introduce a living layer to the interior.",
      "Planting wraps the columns and connects to the architecture.",
      "A linear planted edge softens the boundary of the dining area.",
    ],
  },
  {
    visual: "/api/private-arrival/home-villa-terrace.webp",
    alt: "Residential terrace planting visualization with lounge seating and a planted wall.",
    detail: "Villa 7 proposal, page 17, Lit Planter Shelves. A design visualization, not a documented maintenance record or a time-lapse.",
    markerLabels: ["Notice", "Return", "Remember"],
    markerDescriptions: [
      "A planted lounge is a space for repeated everyday use.",
      "Living planting calls for ongoing attention after installation.",
      "The image illustrates a proposed setting, not a record of growth.",
    ],
  },
] as const;
