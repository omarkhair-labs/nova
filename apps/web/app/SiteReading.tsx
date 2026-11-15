"use client";

import { useState } from "react";
import { courtyard, Photograph } from "./Photograph";

const lenses = [
  { key: "light", label: "Light", title: "Look past the photograph.", text: "The arch casts a deep shadow while the open court catches direct sun. Watch both areas across the day before choosing what to plant." },
  { key: "movement", label: "Movement", title: "Walk the path.", text: "The passage follows the left side of the court. Keep it open as the tree grows, and read the place from the height of someone walking or sitting." },
  { key: "care", label: "Care", title: "Leave room to work.", text: "The open bed gives the roots space. Its edge needs room for someone to reach the soil and tend the tree without blocking the passage." },
] as const;

export default function SiteReading() {
  const [active, setActive] = useState<(typeof lenses)[number]["key"]>("light");
  const lens = lenses.find(item => item.key === active)!;

  return <div className="field-reading">
    <div className="field-reading-scene" data-lens={active}>
      <Photograph image={courtyard} caption="Courtyard study · Generated concept" sizes="(max-width: 760px) 90vw, 58vw" />
      <span className="field-reading-marker" aria-hidden="true"><i /></span>
    </div>
    <div className="field-reading-notes">
      <fieldset className="field-lens-options">
        <legend>Choose a lens</legend>
        <div className="field-lenses">
          {lenses.map(item => <label key={item.key}>
            <input type="radio" name="reading-lens" value={item.key} checked={active === item.key} onChange={() => setActive(item.key)} />
            <span>{item.label}</span>
          </label>)}
        </div>
      </fieldset>
      <div className="field-lens-note" role="status">
        <div key={lens.key}>
          <h3>{lens.title}</h3>
          <p>{lens.text}</p>
        </div>
      </div>
      <noscript>
        <p>Walk the left passage and allow room for future growth. Leave enough space around the bed for someone to reach the soil without blocking the passage.</p>
      </noscript>
    </div>
  </div>;
}
