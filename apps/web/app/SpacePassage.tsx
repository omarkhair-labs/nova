"use client";

import { useState } from "react";
import { environments } from "./environments";
import { SceneBackdrop, SceneCredit, SceneNote } from "./SpatialStage";

export default function SpacePassage() {
  const [active, setActive] = useState(0);
  const place = environments[active];

  return <section className="scene-world places-world" aria-label="Choose an environment">
    <SceneBackdrop image={place.image} key={place.slug} />
    <div className="scene-shadow" aria-hidden="true" />
    <div className="places-note">
      <SceneNote label="Spaces" title="Where it can grow.">
        <p>{place.question}</p>
      </SceneNote>
    </div>
    <nav className="place-paths" aria-label="Types of spaces">
      {environments.map((item, index) => <a key={item.slug} href={"/spaces/" + item.slug}
        data-preview={active === index} onFocus={() => setActive(index)}
        onPointerEnter={event => { if (event.pointerType === "mouse") setActive(index); }}>
        <span className="map-pin" aria-hidden="true" />{item.name}<span aria-hidden="true">↗</span>
      </a>)}
    </nav>
    <SceneCredit>Space studies · Generated concepts</SceneCredit>
  </section>;
}
