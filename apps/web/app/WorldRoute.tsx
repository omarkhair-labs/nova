"use client";

import { useState } from "react";
import SiteIndex, { type SiteSection } from "./SiteIndex";
import "./world-routes.css";

type WorldKind = "spaces" | "studio" | "systems";

const spaces = [
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

const studioSteps = [
  { number: "01", verb: "Observe", short: "Read the environment", note: "Notice the light, architecture, use patterns and constraints before proposing a living intervention.", layer: "site" },
  { number: "02", verb: "Compose", short: "Find the relationship", note: "Study the balance of scale, natural material, visual weight and how the planted space is experienced.", layer: "composition" },
  { number: "03", verb: "Introduce", short: "Bring life into place", note: "Consider installation, placement and the transition from a drawing into an actual environment.", layer: "installation" },
  { number: "04", verb: "Return", short: "Think beyond day one", note: "Care, observation and adaptation are part of the continuing relationship, not a decorative afterword.", layer: "continuity" },
] as const;

const systems = [
  {
    id: "01", date: "THE START", title: "A place receives life",
    description: "An installation connects a plant to an actual space, its conditions and the people responsible for it.",
    detail: "A designed moment is the beginning of a relationship, not the whole outcome.",
  },
  {
    id: "02", date: "THE ROUTINE", title: "Care becomes visible",
    description: "Plans help people remember what deserves attention; recorded actions should remain distinct from suggested tasks.",
    detail: "A due date is not proof that care happened. Human observation and confirmation matter.",
  },
  {
    id: "03", date: "THE MEMORY", title: "Time leaves a record",
    description: "Changes worth noticing can be connected to a plant and place, building continuity rather than a frozen gallery.",
    detail: "This is a conceptual explanation, not a claim of live sensors or measured growth.",
  },
] as const;

const mastheads: Record<WorldKind, { eyebrow: string; number: string; title: string; italic: string; note: string }> = {
  spaces: {
    eyebrow: "02 / THE WORLD OF SPACE",
    number: "02—04",
    title: "Every space",
    italic: "has its own life.",
    note: "From homes and hotels to factories and public spaces, each built environment has its own conditions. Explore where nature could belong.",
  },
  studio: {
    eyebrow: "03 / THE STUDIO",
    number: "03—04",
    title: "Designed",
    italic: "to belong.",
    note: "Behind a living space is a series of choices: what to observe, what to introduce, and what to care for.",
  },
  systems: {
    eyebrow: "04 / LIVING SYSTEMS",
    number: "04—04",
    title: "Day one",
    italic: "is not the end.",
    note: "Explore a relationship that continues beyond installation — through human care, observation and honest records.",
  },
};

function Header({ current }: { current: SiteSection }) {
  const folio = current === "spaces" ? "02" : current === "studio" ? "03" : "04";
  return (
    <header className="wr-header">
      <a href="/" className="wr-wordmark" aria-label="Plantupia — home">
        <span className="wr-symbol" aria-hidden="true">◐</span><span>Plantupia</span>
      </a>
      <SiteIndex current={current} tone="dark" />
      <a className="wr-scene-link" href="/">THE EXPERIENCE <span aria-hidden="true">↗</span><small>{folio} / 04</small></a>
    </header>
  );
}

type EditorialPhoto = {
  src: string;
  fallback: string;
  credit: string;
  source: string;
  subject: string;
};

const editorialSpaces: readonly EditorialPhoto[] = [
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

const studioPhoto: EditorialPhoto = {
  src: "https://images.pexels.com/photos/28461045/pexels-photo-28461045.jpeg?auto=compress&cs=tinysrgb&w=2200",
  fallback: "/scene/hd/life.webp",
  credit: "Alpha En / Pexels",
  source: "https://www.pexels.com/photo/modern-office-space-with-indoor-plants-28461045/",
  subject: "A contemporary planted workspace used as a process reference",
};

const systemsPhoto: EditorialPhoto = {
  src: "https://images.pexels.com/photos/36776751/pexels-photo-36776751.jpeg?auto=compress&cs=tinysrgb&w=2200",
  fallback: "/scene/hd/time.webp",
  credit: "mingche lee / Pexels",
  source: "https://www.pexels.com/photo/modern-shopping-mall-with-indoor-trees-36776751/",
  subject: "A contemporary public interior with indoor trees, used as a continuity reference",
};

function EditorialImage({ photo, eager = false }: { photo: EditorialPhoto; eager?: boolean }) {
  return (
    <img
      className="wr-editorial-image"
      src={photo.src}
      alt=""
      loading={eager ? "eager" : "lazy"}
      fetchPriority={eager ? "high" : "auto"}
      onError={event => {
        const image = event.currentTarget;
        const fallbackUrl = new URL(photo.fallback, window.location.origin).href;
        if (image.src !== fallbackUrl) image.src = fallbackUrl;
      }}
    />
  );
}

const spacePrompts = [
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

function SpaceScene({ active }: { active: number }) {
  const [focus, setFocus] = useState(0);
  const currentFocus = Math.max(0, Math.min(focus, 2));
  const prompts = spacePrompts[active];

  return (
    <div className={"wr-editorial-stage wr-spatial-stage wr-spatial-stage-" + active}>
      <div className="wr-editorial-photos" aria-hidden="true">
        {editorialSpaces.map((photo, i) => (
          <div key={photo.source} className={"wr-editorial-frame" + (i === active ? " is-visible" : "")}>
            <EditorialImage photo={photo} eager={i === 0} />
          </div>
        ))}
      </div>
      <div className="wr-editorial-grade" aria-hidden="true" />
      <div className="wr-image-topline">
        <span>SPACES / THE ARCHITECTURAL FIELD</span>
        <span>STUDY {spaces[active].id} / 06</span>
      </div>
      <div className="wr-editorial-point wr-editorial-point-spaces" aria-hidden="true">
        <span className="wr-point-ring"/><span>DESIGN CONSIDERATIONS</span>
      </div>
      <div className="wr-spatial-prompts">
        <div className="wr-spatial-prompt-list" role="group" aria-label="Design prompts for this space">
          {prompts.map((item, i) => (
            <button key={item.label} type="button" aria-pressed={i === currentFocus}
              onClick={() => setFocus(i)}>
              <span>0{i + 1}</span>{item.label}<span aria-hidden="true">↗</span>
            </button>
          ))}
        </div>
        <div className="wr-spatial-prompt-note" aria-live="polite">
          <span>TO CONSIDER / {prompts[currentFocus].label.toUpperCase()}</span>
          <p>{prompts[currentFocus].detail}</p>
        </div>
      </div>
      <div className="wr-editorial-footline">
        <span>PHOTO REFERENCE / NOT AN INSTALLED PLANTUPIA SPACE</span>
        <a href={editorialSpaces[active].source} target="_blank" rel="noopener noreferrer">
          {editorialSpaces[active].credit} ↗
        </a>
      </div>
    </div>
  );
}

function StudioScene({ phase }: { phase: number }) {
  const coordinates = [
    { number: "01", label: "READ THE SPACE", position: "LIGHT / MATERIAL / MOVEMENT" },
    { number: "02", label: "FIND A COMPOSITION", position: "PROPORTION / RELATIONSHIP" },
    { number: "03", label: "INTRODUCE LIVING MATERIAL", position: "PLACEMENT / INSTALLATION" },
    { number: "04", label: "DESIGN FOR RETURN", position: "OBSERVATION / CONTINUITY" },
  ] as const;

  return (
    <div className={"wr-editorial-stage wr-atelier-stage wr-atelier-phase-" + phase}>
      <div className="wr-editorial-photos" aria-hidden="true">
        <div className="wr-editorial-frame is-visible"><EditorialImage photo={studioPhoto} eager /></div>
      </div>
      <div className="wr-editorial-grade" aria-hidden="true" />
      <div className="wr-image-topline">
        <span>THE STUDIO / A WORKING METHOD</span><span>DESIGN LAYER {studioSteps[phase].number} / 04</span>
      </div>
      <div className="wr-atelier-paper" aria-hidden="true">
        <div className="wr-atelier-axis wr-atelier-axis-h" />
        <div className="wr-atelier-axis wr-atelier-axis-v" />
        <div className="wr-atelier-orbit" />
        <div className="wr-atelier-window" />
        <div className="wr-atelier-material wr-atelier-material-a" />
        <div className="wr-atelier-material wr-atelier-material-b" />
        <div className="wr-atelier-light" />
        <div className="wr-atelier-living" />
        <div className="wr-atelier-return" />
        <span className="wr-atelier-coord wr-atelier-coord-a">NORTH / +0.00</span>
        <span className="wr-atelier-coord wr-atelier-coord-b">DETAIL / NO. {coordinates[phase].number}</span>
      </div>
      <div className="wr-atelier-caption" aria-live="polite">
        <span className="wr-atelier-caption-number">{coordinates[phase].number}</span>
        <div>
          <span>{coordinates[phase].label}</span>
          <p>{coordinates[phase].position}</p>
        </div>
      </div>
      <div className="wr-editorial-footline">
        <span>DESIGN DIAGRAM / CONCEPT, NOT A CLIENT DRAWING</span>
        <a href={studioPhoto.source} target="_blank" rel="noopener noreferrer">{studioPhoto.credit} ↗</a>
      </div>
    </div>
  );
}

function SystemsScene({ phase }: { phase: number }) {
  const steps = [
    { tag: "01", title: "Place", note: "Installation / context" },
    { tag: "02", title: "Care", note: "Human action / attention" },
    { tag: "03", title: "Memory", note: "Recorded observation / time" },
  ] as const;
  return (
    <div className={"wr-editorial-stage wr-continuity-stage wr-continuity-phase-" + phase}>
      <div className="wr-editorial-photos" aria-hidden="true">
        <div className="wr-editorial-frame is-visible"><EditorialImage photo={systemsPhoto} eager /></div>
      </div>
      <div className="wr-editorial-grade" aria-hidden="true" />
      <div className="wr-image-topline"><span>LIVING SYSTEMS / THE CONTINUING RELATIONSHIP</span><span>CHAPTER 0{phase + 1} / 03</span></div>
      <div className="wr-continuity-orbit" aria-hidden="true"><span/><span/><span/></div>
      <div className="wr-continuity-script" aria-label="Conceptual relationship stages">
        {steps.map((step, i) => (
          <div key={step.tag} className={"wr-continuity-step" + (i === phase ? " wr-step-active" : "")}>
            <span className="wr-continuity-pin" aria-hidden="true" />
            <span className="wr-continuity-tag">{step.tag} / {step.title.toUpperCase()}</span>
            <strong>{step.note}</strong>
          </div>
        ))}
      </div>
      <div className="wr-continuity-caption">
        <span>NOT A LIVE DASHBOARD</span>
        <p>A task, a recorded action and a memory are different things. Each deserves an honest place in the story.</p>
      </div>
      <div className="wr-editorial-footline">
        <span>EDITORIAL CONCEPT / NO SENSOR DATA OR VERIFIED GROWTH</span>
        <a href={systemsPhoto.source} target="_blank" rel="noopener noreferrer">{systemsPhoto.credit} ↗</a>
      </div>
    </div>
  );
}

export default function WorldRoute({ kind }: { kind: WorldKind }) {
  const [selection, setSelection] = useState(0);
  const masthead = mastheads[kind];
  const currentSection: SiteSection = kind === "systems" ? "systems" : kind;
  const total = kind === "spaces" ? spaces.length : kind === "studio" ? studioSteps.length : systems.length;

  return (
    <div className={"wr-world wr-world-" + kind}>
      <a href="#wr-main" className="wr-skip">Skip to content</a>
      <Header current={currentSection}/>
      <main id="wr-main" className="wr-main">
        <section className="wr-creative">
          <div className="wr-introduction">
            <p className="wr-eyebrow"><span/> {masthead.eyebrow}</p>
            <h1>{masthead.title}<br/><em>{masthead.italic}</em></h1>
            <p className="wr-lead">{masthead.note}</p>
            <div className="wr-controls" role="group" aria-label={"Explore " + kind}>
              {(kind === "spaces" ? spaces : kind === "studio" ? studioSteps : systems).map((entry, index) => (
                <button type="button" key={index}
                  aria-pressed={index === selection}
                  onClick={() => setSelection(index)}>
                  <span>{String(index + 1).padStart(2, "0")}</span>
                  <strong>{"name" in entry ? entry.name : "verb" in entry ? entry.verb : entry.title}</strong>
                  <i aria-hidden="true">↗</i>
                </button>
              ))}
            </div>
            <p className="wr-study-warning">CONCEPT EXPLORATION / NOT A CLAIM OF COMPLETED CLIENT WORK</p>
          </div>
          <div className="wr-gallery">
            <div className="wr-gallery-head">
              <span>PLANTUPIA / {masthead.number}</span>
              <span>{kind === "spaces" ? "THE PLACE" : kind === "studio" ? "THE PROCESS" : "THE CONTINUITY"} ↘</span>
            </div>
            <div className="wr-gallery-art" role="group"
              aria-label={kind === "spaces" ? "Spatial editorial photography and interactive design prompts" : kind === "studio" ? "Architectural process layers over an editorial photograph" : "Conceptual care continuity over editorial botanical photography"}>
              {kind === "spaces" ? <SpaceScene active={selection}/> : kind === "studio" ? <StudioScene phase={selection}/> : <SystemsScene phase={selection}/>}
            </div>
            <div className="wr-gallery-reading" aria-live="polite" aria-atomic="true">
              <div className="wr-reading-number">{String(selection+1).padStart(2,"0")} / {String(total).padStart(2,"0")}</div>
              <div>
                <span>{kind === "spaces" ? spaces[selection].type : kind === "studio" ? studioSteps[selection].short : systems[selection].date}</span>
                <h2>{kind === "spaces" ? spaces[selection].headline : kind === "studio" ? studioSteps[selection].verb : systems[selection].title}</h2>
                <p>{kind === "spaces" ? spaces[selection].text : kind === "studio" ? studioSteps[selection].note : systems[selection].description}</p>
                {kind === "systems" && <p className="wr-system-detail">{systems[selection].detail}</p>}
                {kind === "spaces" &&
                  <div className="wr-tags">{spaces[selection].points.map(p=><small key={p}>{p}</small>)}</div>}
              </div>
            </div>
          </div>
        </section>
        {kind === "spaces" && (
          <section className="wr-brief-bridge" aria-labelledby="wr-brief-title">
            <div className="wr-brief-bridge-copy">
              <span>FROM INSPIRATION / TO A FIRST CONVERSATION</span>
              <h2 id="wr-brief-title">What could nature<br/><em>bring to your space?</em></h2>
              <p>You have explored the possibilities. Now put a few thoughts about your own place together, without pressure or a long form.</p>
            </div>
            <a href="/start-a-space" className="wr-brief-bridge-link">
              START WITH YOUR SPACE <span aria-hidden="true">↗</span>
              <small>PREPARE A PRIVATE PROJECT OUTLINE — NO MESSAGE IS SENT</small>
            </a>
          </section>
        )}
        <nav className="wr-next" aria-label="Continue exploring Plantupia">
          <span>THE STORY CONTINUES</span>
          <a href={kind === "spaces" ? "/studio" : kind === "studio" ? "/living-systems" : "/"}>
            {kind === "spaces" ? "Next: The Studio" : kind === "studio" ? "Next: Living Systems" : "Return to The Experience"}
            <span aria-hidden="true">↗</span>
          </a>
        </nav>
      </main>
      <footer className="wr-footer">
        <a href="/">PLANTUPIA / THE WORLD</a>
        <span>SPACE / LIVING MATERIAL / HUMAN CARE / TIME</span>
        <span>ENGLISH DESIGN PREVIEW / 2026</span>
      </footer>
    </div>
  );
}
