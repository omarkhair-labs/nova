"use client";

import { useState } from "react";
import { environments } from "./environments";
import { Photograph } from "./Photograph";

export default function SpacePassage() {
  const [active, setActive] = useState(0);
  const place = environments[active];

  return <section aria-label="Types of spaces" className="field-shell space-passage">
    <div className="space-passage-plate" key={place.slug}>
      <Photograph image={place.image} caption={place.name + " · Generated concept"}
        sizes="(max-width: 780px) 90vw, 58vw" priority={active === 0} />
    </div>
    <ul className="space-passage-links">
      {environments.map((item, index) => <li key={item.slug}>
        <a href={"/spaces/" + item.slug} aria-label={item.name + " — " + item.question}
          data-preview={active === index}
          onFocus={() => setActive(index)}
          onPointerEnter={event => { if (event.pointerType === "mouse") setActive(index); }}>
          <div><h2>{item.name}</h2><p>{item.question}</p></div>
          <span aria-hidden="true">↗</span>
        </a>
      </li>)}
    </ul>
  </section>;
}
