"use client";

import { useState } from "react";
import SiteIndex, { type SiteSection } from "./SiteIndex";
import "./world-routes.css";

type WorldKind = "spaces" | "studio" | "systems";

const spaces = [
  {
    id: "01", name: "Places to live", type: "DWELLING / LIGHT",
    headline: "Make room for nature.",
    text: "A home is full of everyday rituals. Greenery becomes meaningful when the choice of plant, position, light and human movement are considered together.",
    points: ["DAYLIGHT", "A QUIET CORNER", "EVERYDAY RETURN"],
  },
  {
    id: "02", name: "Places to work", type: "WORK / RHYTHM",
    headline: "A different way to focus.",
    text: "Working environments have changing rhythms and shared routines. We explore the relationship between planting, circulation, moments of pause and the surrounding architecture.",
    points: ["RHYTHM", "MOVEMENT", "MATERIAL"],
  },
  {
    id: "03", name: "Places to gather", type: "HOSPITALITY / ATMOSPHERE",
    headline: "Give the room its own pulse.",
    text: "Arrival, comfort and a sense of connection shape places for gathering. Living material can become part of the atmosphere, not an afterthought.",
    points: ["ARRIVAL", "SHARED SPACE", "ATMOSPHERE"],
  },
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
    note: "The same plant is never the same experience in every setting. Explore the questions we ask of a place.",
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

function SpaceScene({ active }: { active: number }) {
  return (
    <div className={"wr-space-art wr-space-art-" + active} aria-hidden="true">
      <div className="wr-arch-vault"/><div className="wr-arch-horizon"/>
      <div className="wr-space-sun"/><div className="wr-arch-wall"/><div className="wr-arch-window"/>
      <div className="wr-arch-floor"/>
      <div className="wr-plant-form wr-plant-one"><span/><span/><span/></div>
      <div className="wr-plant-form wr-plant-two"><span/><span/><span/></div>
      <div className="wr-arch-object"/>
      <span className="wr-art-index">SPATIAL / STUDY 0{active+1}</span>
    </div>
  );
}

function StudioScene({ phase }: { phase: number }) {
  return (
    <div className={"wr-studio-art wr-studio-art-" + phase} aria-hidden="true">
      <span className="wr-plan-label">PLANTUPIA / DESIGN STUDY</span>
      <div className="wr-plan-grid"/><div className="wr-plan-window"/>
      <div className="wr-plan-arc"/><div className="wr-plan-shape wr-plan-shape-a"/>
      <div className="wr-plan-shape wr-plan-shape-b"/>
      <div className="wr-plan-light"/><div className="wr-plan-plant"/>
      <div className="wr-plan-path"/>
      <span className="wr-plan-caption">{studioSteps[phase].number} / {studioSteps[phase].layer.toUpperCase()}</span>
    </div>
  );
}

function SystemsScene({ phase }: { phase: number }) {
  return (
    <div className={"wr-system-art wr-system-art-" + phase} aria-hidden="true">
      <div className="wr-system-ring wr-ring-one" />
      <div className="wr-system-ring wr-ring-two" />
      <div className="wr-system-node wr-node-one" />
      <div className="wr-system-node wr-node-two" />
      <div className="wr-system-node wr-node-three" />
      <div className="wr-system-branch wr-branch-one" />
      <div className="wr-system-branch wr-branch-two" />
      <div className="wr-system-branch wr-branch-three" />
      <span className="wr-systems-code">A LIVING RECORD / A CONCEPTUAL STUDY</span>
      <strong>0{phase + 1}</strong>
    </div>
  );
}

export default function WorldRoute({ kind }: { kind: WorldKind }) {
  const [selection, setSelection] = useState(0);
  const masthead = mastheads[kind];
  const currentSection: SiteSection = kind === "systems" ? "systems" : kind;
  const total = kind === "studio" ? 4 : 3;

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
            <div className="wr-gallery-art" role="img"
              aria-label={kind === "spaces"
                ? "Original conceptual composition of an architectural space and planting"
                : kind === "studio"
                ? "Original diagram of a design process showing changing layers"
                : "Original abstract visualization of place, plant and care connected over time"}>
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
