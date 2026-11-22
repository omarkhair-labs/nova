"use client";

import { useState } from "react";
import { homeField } from "./visual-production-map";
import "./home-visual-field.css";

const chapters = [
  {
    name: "Pixels Mall",
    context: "COMMERCIAL / FOUR SPATIAL READINGS",
    description: "One place, read through its courtyard, landmark planting and promenade.",
    href: "/spaces/projects/pixels-mall",
    images: [homeField[0], homeField[3], homeField[5], homeField[7]],
  },
  {
    name: "Audi Cairo",
    context: "INTERIORS / TWO SPATIAL READINGS",
    description: "Living material considered at the scale of the room and the brand.",
    href: "/spaces/projects/audi-cairo",
    images: [homeField[1], homeField[4]],
  },
  {
    name: "Villa 7",
    context: "RESIDENTIAL / TWO SPATIAL READINGS",
    description: "Different moments in one residential planting proposal.",
    href: "/spaces/projects/villa-7",
    images: [homeField[2], homeField[6]],
  },
] as const;

export default function HomeVisualField() {
  const [selected, setSelected] = useState<number[]>([0, 0, 0]);
  return <section className="hvf" aria-labelledby="hvf-title">
    <header className="hvf-head">
      <div><p>SELECTED WORK / PLACES IN VIEW</p><h2 id="hvf-title">A living practice,<br/><em>seen in place.</em></h2></div>
      <div><p>Explore several design views of the same place. These are supplied proposal studies, not verified photographs of completed installations.</p><a href="/spaces/projects">Explore the full project archive &#8599;</a></div>
    </header>
    <div className="hvf-grid">
      {chapters.map((chapter, groupIndex) => {
        const active = chapter.images[selected[groupIndex] ?? 0];
        return <article className={"hvf-project hvf-project-" + (groupIndex + 1)} key={chapter.name}>
          <div className="hvf-project-copy">
            <span className="hvf-project-number">0{groupIndex + 1} / 03</span>
            <div><span className="hvf-project-type">{chapter.context}</span><h3>{chapter.name}</h3><p>{chapter.description}</p></div>
            <a href={chapter.href}>READ THE PROJECT <span aria-hidden="true">↗</span></a>
          </div>
          <figure className="hvf-project-feature">
            <div className="hvf-project-image"><img key={active.asset} src={"/api/private-arrival/" + active.asset} alt={active.label + " — source proposal image"} loading={groupIndex === 0 ? "eager" : "lazy"}/></div>
            <figcaption><span>{active.label}</span><small>{active.meta} · Design proposal</small></figcaption>
          </figure>
          <div className="hvf-project-strip" role="group" aria-label={"Select a view of " + chapter.name}>
            {chapter.images.map((item, imageIndex) => <button type="button" key={item.asset} aria-pressed={selected[groupIndex] === imageIndex}
              aria-label={"View " + item.label} onClick={() => setSelected(previous => previous.map((n,i) => i === groupIndex ? imageIndex : n))}>
              <img src={"/api/private-arrival/" + item.asset} alt="" loading="lazy" />
              <span>{String(imageIndex + 1).padStart(2,"0")}</span>
            </button>)}
          </div>
        </article>;
      })}
    </div>
    <div className="hvf-rapid-index" aria-hidden="true"><div className="hvf-rapid-list">{homeField.map((item,i)=><div className="hvf-rapid-item" key={"rapid-"+item.asset}><span>{String(i+1).padStart(2,"0")}</span><img src={"/api/private-arrival/"+item.asset} alt="" loading="lazy"/><strong>{item.label}</strong></div>)}</div><p>SELECTED WORK / RAPID INDEX</p></div>
    <footer><span>SPACE / LIVING MATERIAL / CONTINUITY</span><p>Different scales, one living relationship.</p><a href="/spaces">Explore the environments &#8599;</a></footer>
  </section>;
}
