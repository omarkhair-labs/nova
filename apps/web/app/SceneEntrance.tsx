"use client";

import { useEffect, useState } from "react";
import { courtyard, Photograph } from "./Photograph";
import { tending, threshold } from "./courtyard-scenes";

const stages = [
  {
    id: "space",
    name: "Space",
    line: "At the threshold",
    image: threshold,
    heading: <>Where life<br /><em>takes root.</em></>,
    text: "Plantupia designs and installs planting for the places we build, with continuing care in mind.",
    href: "/studio",
    action: "Step inside",
  },
  {
    id: "planting",
    name: "Planting",
    line: "Inside the courtyard",
    image: courtyard,
    heading: <>Room<br /><em>to grow.</em></>,
    text: "Light moves across the stone. The bed gives the tree room, and the passage stays open. The place decides what can live here.",
    href: "/studio",
    action: "Read the courtyard",
  },
  {
    id: "care",
    name: "Care",
    line: "A return visit",
    image: tending,
    heading: <>Come<br /><em>back to it.</em></>,
    text: "A planting keeps changing after installation. The people who return to it learn to notice, care and carry that knowledge forward.",
    href: "/living-systems",
    action: "Follow the aftercare",
  },
] as const;

export default function SceneEntrance() {
  const [active, setActive] = useState(0);
  const [ready, setReady] = useState(false);
  const stage = stages[active];

  useEffect(() => { setReady(true); }, []);

  return <section className="courtyard-scene" data-scene={stage.id} aria-label="Three views of a living courtyard">
    <div className="courtyard-images" aria-hidden="true">
      {stages.map((shot, index) => <div className="courtyard-shot" key={shot.id}
        data-shot={shot.id} data-active={index === active}>
        <Photograph image={{ ...shot.image, alt: "" }} caption={null}
          sizes="(max-width: 640px) 110vw, 100vw" priority={index === 0} />
      </div>)}
    </div>
    <div className="courtyard-scrim" aria-hidden="true" />
    <div className="field-shell courtyard-body">
      <div className="courtyard-telling" aria-live="polite">
        <div key={stage.id}>
          <p className="field-kicker"><span /> {stage.line}</p>
          <h1>{stage.heading}</h1>
          <p className="courtyard-description">{stage.text}</p>
        </div>
        <a className="courtyard-action" href={stage.href} onClick={event => {
          if (active !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
          event.preventDefault();
          setActive(1);
        }}>{stage.action} <span aria-hidden="true">{active === 0 ? "→" : "↗"}</span></a>
      </div>
      <div className="courtyard-console">
        <div className="courtyard-controls" role="group" aria-label="Read the courtyard">
          {stages.map((item, index) => <button key={item.id} type="button" disabled={!ready}
            aria-pressed={active === index} onClick={() => setActive(index)}>
            <span className="courtyard-control-tick" aria-hidden="true" /> {item.name}
          </button>)}
        </div>
        <a className="courtyard-index-link" href="#practice">Continue into Plantupia <span aria-hidden="true">↓</span></a>
      </div>
      <p className="courtyard-caption">Courtyard and care scenes · Generated concepts</p>
    </div>
  </section>;
}
