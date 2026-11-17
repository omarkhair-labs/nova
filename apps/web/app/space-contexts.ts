export const spaces = [
  {
    "id": "01",
    "name": "Homes & residences",
    "type": "RESIDENTIAL / DAILY LIFE",
    "headline": "Make room for nature.",
    "text": "A home is full of everyday rituals. Plant selection, daylight and movement shape a living environment.",
    "points": [
      "DAYLIGHT",
      "HUMAN SCALE",
      "EVERYDAY CARE"
    ]
  },
  {
    "id": "02",
    "name": "Workplaces & offices",
    "type": "WORK / RHYTHM",
    "headline": "A different way to focus.",
    "text": "From a desk to a shared atrium, greenery can belong with the architecture and working routines.",
    "points": [
      "WORKFLOW",
      "CIRCULATION",
      "MAINTENANCE"
    ]
  },
  {
    "id": "03",
    "name": "Hotels & hospitality",
    "type": "HOSPITALITY / ARRIVAL",
    "headline": "Give arrival its own character.",
    "text": "From hotel lobbies to restaurants and shared guest spaces, consider how planting shapes atmosphere and how it can be cared for.",
    "points": [
      "ARRIVAL",
      "ATMOSPHERE",
      "GUEST FLOW"
    ]
  },
  {
    "id": "04",
    "name": "Industry & factories",
    "type": "INDUSTRY / CONDITIONS",
    "headline": "Life beyond the office.",
    "text": "Explore greenery around industrial sites, administrative areas and staff spaces where appropriate. Operational safety and site constraints come first.",
    "points": [
      "SITE RULES",
      "EXPOSURE",
      "SAFE ACCESS"
    ]
  },
  {
    "id": "05",
    "name": "Retail & destinations",
    "type": "RETAIL / EXPERIENCE",
    "headline": "Make a place worth noticing.",
    "text": "From a small shop to a large retail destination, explore how living material can relate to circulation, displays and the visitor journey.",
    "points": [
      "VISITOR FLOW",
      "SCALE",
      "UPKEEP"
    ]
  },
  {
    "id": "06",
    "name": "Public & shared spaces",
    "type": "PUBLIC / COMMON GROUND",
    "headline": "Nature belongs in shared places.",
    "text": "Courtyards, cultural spaces and common areas offer different opportunities for living material, shaped by climate, access and use.",
    "points": [
      "CLIMATE",
      "ACCESS",
      "SHARED USE"
    ]
  }
] as const;

export type EditorialPhoto = {
  src: string;
  fallback: string;
  credit: string;
  source: string;
  subject: string;
};

export const editorialSpaces: readonly EditorialPhoto[] = [
  {
    "src": "https://images.pexels.com/photos/34992781/pexels-photo-34992781.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/space.webp",
    "credit": "Liuuu _61 / Pexels",
    "source": "https://www.pexels.com/photo/contemporary-minimalist-living-room-interior-34992781/",
    "subject": "A contemporary residential interior with houseplants"
  },
  {
    "src": "https://images.pexels.com/photos/28461045/pexels-photo-28461045.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/life.webp",
    "credit": "Alpha En / Pexels",
    "source": "https://www.pexels.com/photo/modern-office-space-with-indoor-plants-28461045/",
    "subject": "A contemporary office with greenery and natural light"
  },
  {
    "src": "https://images.pexels.com/photos/28999490/pexels-photo-28999490.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/time.webp",
    "credit": "Quang Nguyen Vinh / Pexels",
    "source": "https://www.pexels.com/photo/modern-hotel-lobby-with-blue-and-gray-chairs-28999490/",
    "subject": "A contemporary hospitality lobby"
  },
  {
    "src": "https://images.pexels.com/photos/33095875/pexels-photo-33095875.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/space.webp",
    "credit": "Andy Lee / Pexels",
    "source": "https://www.pexels.com/photo/modern-industrial-building-with-greenery-33095875/",
    "subject": "Industrial architecture surrounded by greenery"
  },
  {
    "src": "https://images.pexels.com/photos/36776751/pexels-photo-36776751.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/life.webp",
    "credit": "mingche lee / Pexels",
    "source": "https://www.pexels.com/photo/modern-shopping-mall-with-indoor-trees-36776751/",
    "subject": "Contemporary retail atrium with indoor trees"
  },
  {
    "src": "https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=2200",
    "fallback": "/scene/hd/time.webp",
    "credit": "DAIPI / Pexels",
    "source": "https://www.pexels.com/photo/sunlit-urban-courtyard-tree-surrounded-by-walls-31814108/",
    "subject": "A tree within a contemporary concrete courtyard"
  }
];

export const spacePrompts = [
  [
    {
      "label": "Light",
      "detail": "Evaluate daylight in the actual room."
    },
    {
      "label": "Movement",
      "detail": "Keep everyday circulation open."
    },
    {
      "label": "Care",
      "detail": "Choose a practical rhythm of care."
    }
  ],
  [
    {
      "label": "Rhythm",
      "detail": "Observe working hours and shared routines."
    },
    {
      "label": "Access",
      "detail": "Protect circulation and maintenance access."
    },
    {
      "label": "Responsibility",
      "detail": "Plan who will care for the planting."
    }
  ],
  [
    {
      "label": "Arrival",
      "detail": "Consider the experience at the entrance."
    },
    {
      "label": "Atmosphere",
      "detail": "Balance natural material and the existing interior."
    },
    {
      "label": "Maintenance",
      "detail": "Plan for frequent use and cleaning."
    }
  ],
  [
    {
      "label": "Safety",
      "detail": "Respect operational hazards and restricted zones."
    },
    {
      "label": "Conditions",
      "detail": "Assess exposure, dust and site constraints."
    },
    {
      "label": "Access",
      "detail": "Keep maintenance and emergency routes clear."
    }
  ],
  [
    {
      "label": "Flow",
      "detail": "Keep sightlines and visitor routes clear."
    },
    {
      "label": "Scale",
      "detail": "Consider the relationship between plants and architecture."
    },
    {
      "label": "Upkeep",
      "detail": "Plan care around opening times."
    }
  ],
  [
    {
      "label": "Climate",
      "detail": "Study heat, light and exposure."
    },
    {
      "label": "Access",
      "detail": "Preserve inclusive circulation."
    },
    {
      "label": "Use",
      "detail": "Plan for the people who share the space."
    }
  ]
] as const;
