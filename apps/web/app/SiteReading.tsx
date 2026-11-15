"use client";

import { useState } from "react";
import { courtyard } from "./Photograph";
import { SceneBackdrop, SceneCredit, SceneNote } from "./SpatialStage";

const lenses = [
  { key: "light", label: "Light", title: "Watch the shadow.", text: "The arch shades one end while direct light reaches the open court. Watch both through the day before choosing what should grow here." },
  { key: "movement", label: "Movement", title: "Walk the passage.", text: "People follow the clear strip beside the bed. Leave space for the canopy to grow and for a person to pass, sit or turn." },
  { key: "care", label: "Care", title: "Leave room to work.", text: "The bed gives roots room below ground. Its edge needs to be reachable without blocking the passage people use." },
] as const;

export default function SiteReading() {
  const [active, setActive] = useState(0);
  const lens = lenses[active];

  return <section className="scene-world study-world" data-lens={lens.key}>
    <SceneBackdrop image={courtyard} className="study-backdrop" />
    <div className="scene-shadow" aria-hidden="true" />
    <div className="study-mark" aria-hidden="true"><span /></div>
    <div className="study-note">
      <SceneNote label="Approach" title="Read the courtyard">
        <fieldset className="scene-lenses">
          <legend>What do you see?</legend>
          {lenses.map((item, index) => <label key={item.key}>
            <input type="radio" name="reading-lens" checked={active === index} onChange={() => setActive(index)} />
            <span>{item.label}</span>
          </label>)}
        </fieldset>
        <div role="status" className="study-caption">
          <strong>{lens.title}</strong><p>{lens.text}</p>
        </div>
        <noscript><p>Follow the left passage and leave enough space at the bed edge for someone to tend the tree.</p></noscript>
        <a className="world-action" href="/living-systems">Return after installation <span aria-hidden="true">↗</span></a>
      </SceneNote>
    </div>
    <SceneCredit>Courtyard study · Generated concept</SceneCredit>
  </section>;
}
