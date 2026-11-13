"use client";

import { useEffect, useState } from "react";
import "./study/scene/scene.css";
import SiteIndex from "./SiteIndex";

type Stage = 0 | 1 | 2;
type MarkerIndex = 0 | 1 | 2;

const scenes = [
  {
    eyebrow: "01 / THE SPACE",
    label: "The Space",
    line: "A space has a life before we enter it.",
    detail: "A quiet architectural study. Light, material, proportion and how people use a room come first.",
    visual: "/scene/hd/space.webp",
    alt: "Original generated architectural visualization of a softly sunlit interior, with a skylight and restrained furniture, before the planting intervention.",
    markerLabels: ["The light", "The threshold", "The materials"],
    markerDescriptions: [
      "The light and ceiling opening create the room's first atmosphere.",
      "A threshold tells us where life could belong within the built space.",
      "Stone, timber and natural light create the conditions we begin with.",
    ],
  },
  {
    eyebrow: "02 / WITH LIFE",
    label: "With Life",
    line: "Nature changes how architecture is felt.",
    detail: "Planting softens edges and makes the room more layered. The intervention is deliberate, not an instant-growth effect.",
    visual: "/scene/hd/life.webp",
    alt: "Original generated cinematic visualization of the same architectural concept with abundant integrated tropical planting and sunlight.",
    markerLabels: ["Living material", "Filtered light", "A softer boundary"],
    markerDescriptions: [
      "Planting becomes part of the room rather than an object placed on top.",
      "Leaves interrupt and filter the light, bringing another rhythm into the atmosphere.",
      "An architectural edge can hold a living, changing layer.",
    ],
  },
  {
    eyebrow: "03 / OVER TIME",
    label: "Over Time",
    line: "The story does not end on installation day.",
    detail: "A closer reading of living material: observation, return, and human care. This is not real growth data.",
    visual: "/scene/hd/time.webp",
    alt: "Original generated close botanical view of planted architecture with sunlight, leaves, and textured stone; an illustrative change of viewpoint, not measured growth.",
    markerLabels: ["Notice", "Return", "Remember"],
    markerDescriptions: [
      "Observation turns the living space into an ongoing relationship.",
      "Care is a repeated human action, not an automatic green checkmark.",
      "Documented moments can become the honest memory of a place over time.",
    ],
  },
] as const;

export default function SceneStudy() {
  const [stage, setStage] = useState<Stage>(1);
  const [activePoint, setActivePoint] = useState<MarkerIndex | null>(null);
  const [showInfo, setShowInfo] = useState(false);
  const [compare, setCompare] = useState(false);

  const currentStage = compare && stage > 0 ? ((stage - 1) as Stage) : stage;
  const scene = scenes[currentStage];
  const selectedNote = activePoint !== null ? scene.markerDescriptions[activePoint] : null;

  function switchStage(next: Stage) {
    setCompare(false);
    setStage(next);
    setActivePoint(null);
  }

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      // SiteIndex owns modal keyboard input; the scene must not react behind it.
      if (document.querySelector(".si-overlay")) return;
      if (event.key === "Escape") {
        if (showInfo) setShowInfo(false);
        else if (activePoint !== null) setActivePoint(null);
        return;
      }
      if (showInfo || event.altKey || event.metaKey || event.ctrlKey) return;
      const target = event.target as HTMLElement | null;
      if (target?.closest("a, button, input, textarea, select, [role='slider']")) return;
      if (event.key === "ArrowRight") {
        event.preventDefault();
        switchStage(Math.min(stage + 1, 2) as Stage);
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        switchStage(Math.max(stage - 1, 0) as Stage);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [showInfo, activePoint, stage]);

  const compareHandlers = {
    onPointerDown: () => stage > 0 && setCompare(true),
    onPointerUp: () => setCompare(false),
    onPointerLeave: () => setCompare(false),
    onPointerCancel: () => setCompare(false),
    onBlur: () => setCompare(false),
    onKeyDown: (event: React.KeyboardEvent<HTMLButtonElement>) => {
      if ((event.key === " " || event.key === "Enter") && !event.repeat) {
        event.preventDefault();
        if (stage > 0) setCompare(true);
      }
    },
    onKeyUp: (event: React.KeyboardEvent<HTMLButtonElement>) => {
      if (event.key === " " || event.key === "Enter") {
        event.preventDefault(); setCompare(false);
      }
    },
  };

  return (
    <div className="ps-site" data-stage={currentStage} data-comparing={compare}>
      <a href="#ps-main" className="ps-skip">Skip to experience</a>
      <div className="ps-stage-images" role="img" aria-label={scene.alt}>
        {scenes.map((view, i) => (
          <div className={"ps-image ps-image-" + i + (i === currentStage ? " ps-current" : "")}
            key={view.label}
            style={{ backgroundImage: "url('" + view.visual + "')" }}
            aria-hidden="true" />
        ))}
        <div className="ps-grading" aria-hidden="true" />
        <div className="ps-grain" aria-hidden="true" />
      </div>

      <header className="ps-topbar">
        <a href="/" className="ps-brand" aria-label="Plantupia home">
          <span className="ps-brand-symbol" aria-hidden="true">◕</span>
          <span>Plantupia</span>
        </a>
        <SiteIndex current="experience" tone="dark" />
        <nav aria-label="Stages of the space" className="ps-modes">
          {(["SPACE", "LIFE", "TIME"] as const).map((name, index) => (
            <button key={name} type="button" aria-pressed={stage === index}
              onClick={() => switchStage(index as Stage)}>{name}</button>
          ))}
        </nav>
      </header>

      <main className="ps-main" id="ps-main">
        <section className="ps-story" aria-label="Introducing the space">
          <div className="ps-kicker"><span className="ps-kicker-rule" /> LIVING ARCHITECTURE / STUDY 01</div>
          <h1>A Space<br />Becoming <em>Alive.</em></h1>
          <p>A crafted environment, ready to grow with nature, people, and time.</p>
          <button type="button" className="ps-cta" onClick={() => switchStage(stage === 0 ? 1 : stage === 1 ? 2 : 0)}>
            <span className="ps-cta-icon" aria-hidden="true">{stage === 2 ? "↺" : "↗"}</span>
            {stage === 0 ? "Reveal life" : stage === 1 ? "See the continuity" : "Start again"}
          </button>
        </section>

        <div className="ps-scene-markers" role="group" aria-label="Explore details in this scene">
          {scene.markerLabels.map((label, index) => (
            <button type="button" key={index} className={"ps-scene-point ps-scene-point-" + index +
              (activePoint === index ? " is-active" : "")}
              onClick={() => setActivePoint(activePoint === index ? null : (index as MarkerIndex))}
              aria-pressed={activePoint === index}
              aria-label={"Explore " + label}>
              <span className="ps-scene-dot" aria-hidden="true" />
              <span className="ps-scene-point-label">{label}</span>
            </button>
          ))}
        </div>

        {selectedNote && (
          <aside className="ps-note" role="status" aria-live="polite">
            <button type="button" aria-label="Close detail" className="ps-note-close"
              onClick={() => setActivePoint(null)}>×</button>
            <span className="ps-note-index">FIELD NOTE / 0{(activePoint ?? 0) + 1}</span>
            <h2>{scene.markerLabels[activePoint ?? 0]}</h2>
            <p>{selectedNote}</p>
          </aside>
        )}
        <div className="ps-timeline-area">
          <div className="ps-timeline-intro">
            <span>EXPERIENCE THE CHANGE</span>
            <p>{scene.line}</p>
          </div>
          <div className="ps-sequence" aria-label="Current chapter">
            {scenes.map((item, i) => (
              <div className={"ps-step" + (stage === i ? " ps-step-active" : "")} key={item.label}>
                <span className="ps-step-connector" aria-hidden="true" />
                <span className="ps-step-dot" aria-hidden="true" />
                <span>{item.label}</span>
                <small>{item.eyebrow}</small>
              </div>
            ))}
          </div>
          <div className="ps-sequence-actions">
            <button type="button" className="ps-circle-button" aria-label="Previous chapter"
              disabled={stage === 0} onClick={() => switchStage((stage-1) as Stage)}>←</button>
            <button type="button" className="ps-circle-button" aria-label="Next chapter"
              disabled={stage === 2} onClick={() => switchStage((stage+1) as Stage)}>→</button>
          </div>
        </div>

        <div className="ps-utility-row">
          <span>DESIGN REVEAL / CONCEPT VISUALIZATION</span>
          <button type="button" className="ps-compare" disabled={stage === 0}
            aria-label="Hold to compare with the previous stage"
            aria-pressed={compare} {...compareHandlers}>
            {compare ? "VIEWING PREVIOUS" : "HOLD TO COMPARE"} <span aria-hidden="true">◌</span>
          </button>
          <button type="button" onClick={() => setShowInfo(value => !value)}
            aria-expanded={showInfo} className="ps-about-button">ABOUT THIS STUDY ↗</button>
        </div>
        {showInfo && <aside className="ps-study-info">
          <button type="button" aria-label="Close study details" onClick={() => setShowInfo(false)}>×</button>
          <h2>What is this scene?</h2>
          <p>This is a cinematic design exploration, not a photograph of a completed Plantupia project.
          The three visual studies explore architecture, a planting intervention, and attentive care over time.
          The interactive study now uses three locally hosted, original generated architectural images. The third image is a closer view of botanical material, and the stages are not a frame-accurate 3D morph or a real time-lapse. No biological growth, measured effects, completed Plantupia installation, sensor readings, or customer history are represented.</p>
        </aside>}
      </main>


    </div>
  );
}
