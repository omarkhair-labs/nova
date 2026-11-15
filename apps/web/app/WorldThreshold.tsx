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
      "Explore the worlds of homes, workplaces, hospitality, industry, retail and shared public spaces. Light, circulation, material and human rhythms change how nature can become part of architecture.",
    href: "/spaces",
    image:
      "https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/space.webp",
    imageLabel: "Architectural editorial photo: a concrete courtyard and a tree.",
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
      "https://images.pexels.com/photos/28461045/pexels-photo-28461045.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/life.webp",
    imageLabel: "Architectural editorial photo: a contemporary planted workplace.",
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
      "https://images.pexels.com/photos/36776751/pexels-photo-36776751.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/time.webp",
    imageLabel: "Editorial photo: a contemporary retail atrium with indoor trees, not a record of growth.",
  },
] as const;

export default function WorldThreshold() {
  const [active,setActive]=useState<WorldId>(0);
  const [failed,setFailed]=useState<number[]>([]);
  const current=worlds[active];
  return (
    <section className="wt-section" id="world" aria-labelledby="wt-title">
      <div className="wt-intro"><p>THE WORLD OF PLANTUPIA / 02—04</p><h2 id="wt-title">The world <em>beyond the frame.</em></h2></div>
      <div className="wt-gallery">
        <figure className="wt-picture">
          <div className="wt-photo-field" aria-hidden="true">
            {worlds.map((world,i)=><div key={world.number} className={"wt-photo"+(active===i?" wt-visible":"")}><img src={failed.includes(i)?world.fallback:world.image} alt="" loading="lazy" onError={()=>setFailed(previous=>previous.includes(i)?previous:[...previous,i])}/></div>)}
          </div>
          <figcaption>{failed.includes(active)?"Generated concept illustration.":current.imageLabel} Not a completed Plantupia project.</figcaption>
        </figure>
        <div className="wt-navigation">
          <p className="wt-label">THREE WAYS TO EXPLORE</p>
          <nav className="wt-choices" aria-label="Explore Plantupia chapters">
            {worlds.map((world,i)=><a key={world.number} href={world.href} aria-label={"Enter "+world.name}
              data-chapter-target={world.href} data-preview-active={active===i}
              className={"wt-choice"+(active===i?" wt-choice-selected":"")}
              onPointerEnter={event=>{if(event.pointerType==="mouse")setActive(i as WorldId)}} onFocus={()=>setActive(i as WorldId)}>
              <span>{world.number}</span><strong>{world.name}</strong><span aria-hidden="true">↗</span>
            </a>)}
          </nav>
          <div className="wt-reading" aria-live="polite" key={active}><p className="wt-label">{current.overline}</p><h3>{current.headline}</h3><p>{current.description}</p></div>
          <a className="wt-contact" href="/start-a-space">Have a space in mind? ↗</a>
        </div>
      </div>
    </section>
  );
}
