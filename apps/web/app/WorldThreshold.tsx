"use client";

import { useState } from "react";
import "./world-threshold.css";

type WorldId = 0 | 1 | 2;

const worlds = [
  {
    number: "01",
    name: "Spaces",
    overline: "PLACE / CONTEXT",
    headline: "Where life belongs.",
    description:
      "Explore three kinds of place. Light, circulation, material and human rhythms change how nature can become part of architecture.",
    href: "/spaces",
    image:
      "https://images.pexels.com/photos/28577085/pexels-photo-28577085.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/space.webp",
    imageLabel: "Architectural editorial photo: a skylit interior, used to study spatial context.",
  },
  {
    number: "02",
    name: "The Studio",
    overline: "DESIGN / MATERIAL",
    headline: "Look closer at the making.",
    description:
      "From observing the environment to composing a living intervention: follow the design choices rather than a list of services.",
    href: "/studio",
    image:
      "https://images.pexels.com/photos/33708096/pexels-photo-33708096.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/life.webp",
    imageLabel: "Architectural editorial photo: planted interior and structural daylight.",
  },
  {
    number: "03",
    name: "Living Systems",
    overline: "CARE / CONTINUITY",
    headline: "Stay with the living.",
    description:
      "A place keeps changing after installation. Discover the relationship between plants, observation, human care and the passing of time.",
    href: "/living-systems",
    image:
      "https://images.pexels.com/photos/40883631/pexels-photo-40883631.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/time.webp",
    imageLabel: "Botanical editorial photo: an interior with hanging plants, not a record of growth.",
  },
] as const;

export default function WorldThreshold() {
  const [active, setActive] = useState<WorldId>(0);
  const current = worlds[active];

  return (
    <section className="wt-section" id="world" aria-labelledby="wt-title">
      <div className="wt-photo-field" aria-hidden="true">
        {worlds.map((world, i) => (
          <div className={"wt-photo" + (active === i ? " wt-visible" : "")} key={world.number}>
            <img
              src={world.image}
              alt=""
              loading="lazy"
              onError={event => {
                const img = event.currentTarget;
                const local = new URL(world.fallback, window.location.origin).href;
                if (img.src !== local) img.src = local;
              }}
            />
          </div>
        ))}
        <div className="wt-photo-shade" />
        <div className="wt-plane-outline" />
      </div>
      <div className="wt-folio">
        <span>PLANTUPIA / THE WORLD BEYOND</span>
        <span>CHAPTERS 02—04 <span aria-hidden="true">✳</span></span>
      </div>
      <div className="wt-content">
        <div className="wt-essay">
          <p className="wt-overline"><span className="wt-overline-line" /> FROM A FIRST IMPRESSION</p>
          <h2 id="wt-title">The world<br/>doesn&apos;t end<br/><em>at the frame.</em></h2>
          <p className="wt-essay-note">
            Step beyond the opening scene. Choose a way to understand the spaces, ideas and living relationships behind Plantupia.
          </p>
          <span className="wt-prompt">CHOOSE YOUR WAY IN ↓</span>
        </div>

        <div className="wt-navigation">
          <nav className="wt-choices" aria-label="Choose a chapter to explore">
            {worlds.map((world, i) => (
              <button
                key={world.number}
                type="button"
                className={active === i ? "wt-choice wt-choice-selected" : "wt-choice"}
                aria-pressed={active === i}
                onMouseEnter={() => setActive(i as WorldId)}
                onFocus={() => setActive(i as WorldId)}
                onClick={() => setActive(i as WorldId)}
              >
                <span className="wt-choice-index">{world.number}</span>
                <span className="wt-choice-name">{world.name}</span>
                <span className="wt-choice-sign" aria-hidden="true">{active === i ? "↗" : "+"}</span>
              </button>
            ))}
          </nav>
          <div className="wt-reading" aria-live="polite" aria-atomic="true">
            <p className="wt-reading-id">{current.overline} <span> / {current.number}</span></p>
            <h3>{current.headline}</h3>
            <p className="wt-reading-description">{current.description}</p>
            <a className="wt-enter" href={current.href}>
              ENTER {current.name.toUpperCase()}
              <span aria-hidden="true">↗</span>
            </a>
          </div>
        </div>
      </div>
      <div className="wt-bottom-rail">
        <span>ONE CREATIVE WORLD / MANY WAYS TO EXPERIENCE IT</span>
        <span>{current.imageLabel} Not a completed Plantupia project.</span>
        <a href="#ps-main">BACK TO THE SPACE ↑</a>
      </div>
    </section>
  );
}
