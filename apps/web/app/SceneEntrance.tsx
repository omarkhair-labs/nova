"use client";

import { useState } from "react";
import { courtyard } from "./Photograph";
import { tending, threshold } from "./courtyard-scenes";
import { SceneBackdrop, SceneCredit, SceneNote } from "./SpatialStage";

const scenes = [
  { id: "threshold", image: threshold, label: "Plantupia", title: "Where life takes root.",
    description: "Planting and continuing care for the places we build." },
  { id: "planting", image: courtyard, label: "Inside the courtyard", title: "Room to grow.",
    description: "The bed makes space for roots, and the path gives people a way through." },
  { id: "return", image: tending, label: "A return visit", title: "Come back to it.",
    description: "Look at what changes when people keep coming back to care for the same planting." },
] as const;

export default function SceneEntrance() {
  const [active, setActive] = useState(0);
  const place = scenes[active];

  return <section className="scene-world entry-world" data-moment={place.id} aria-label="An imagined living courtyard">
    <div className="entry-views" aria-hidden="true">
      {scenes.map((scene, index) => <div key={scene.id} data-shot={scene.id} data-visible={index === active}>
        <SceneBackdrop image={scene.image} priority={index === 0} />
      </div>)}
    </div>
    <div className="scene-shadow" aria-hidden="true" />
    <div className="entry-note" aria-live="polite">
      <SceneNote label={place.label} title={place.title}><p>{place.description}</p></SceneNote>
    </div>
    <a className="scene-gateway" href={active === 2 ? "/living-systems#observation" : "/studio"}
      onClick={event => {
        if (active !== 0 || event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return;
        event.preventDefault();
        setActive(1);
      }}>
      <span className="gateway-ring" aria-hidden="true" />
      <span>{active === 0 ? "Step inside" : active === 1 ? "Read this place" : "Follow its care"}</span>
    </a>
    {active > 0 && <div className="entry-return">
      <button type="button" onClick={() => setActive(active - 1)}>← {active === 1 ? "Threshold" : "Planting"}</button>
      {active === 1 && <button type="button" onClick={() => setActive(2)}>Return later →</button>}
    </div>}
    <SceneCredit>Imagined courtyard · Generated scenes</SceneCredit>
  </section>;
}
