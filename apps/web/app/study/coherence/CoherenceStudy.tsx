"use client";

import { useState } from "react";
import SiteIndex from "../../SiteIndex";
import "./coherence.css";

type Scene = { image: string; source: string; title: string; detail: string };
const asset = (file: string) => "/api/private-arrival/" + file;

const places: Scene[] = [
  { image: "home-pixels-courtyard.webp", source: "PIXELS MALL / PROPOSAL P03", title: "The courtyard", detail: "A place already has its own light, structure and movement." },
  { image: "home-audi-lounge.webp", source: "AUDI CAIRO / PROPOSAL P05", title: "The interior", detail: "Planting meets the human scale of an interior." },
  { image: "home-villa-terrace.webp", source: "VILLA 7 / PROPOSAL P17", title: "The terrace", detail: "A different setting, the same living material." },
];

const objects = [
  { image: "shop-augusta-wide.webp", number: "01", title: "In its setting", source: "HOSPITALITY CATALOGUE / P27", description: "Read the object at the scale of its room." },
  { image: "shop-augusta-object.webp", number: "02", title: "As an object", source: "HOSPITALITY CATALOGUE / P27", description: "Bring proportion, finish and vessel into focus." },
  { image: "shop-fiberglass-pots.webp", number: "03", title: "As a family", source: "HOSPITALITY CATALOGUE / P37", description: "See how a material becomes a range of choices." },
] as const;

export default function CoherenceStudy() {
  const [scene, setScene] = useState(0);
  const [comparison, setComparison] = useState(52);
  const [object, setObject] = useState(0);
  const current = places[scene];
  const selected = objects[object];
  return (
    <div className="pc-study">
      <a className="pc-skip" href="#pc-content">Skip to study</a>
      <div className="pc-research-label"><span>PLANTUPIA / CREATIVE R&D</span><span>PRIVATE COHERENCE STUDY — NOT PRODUCTION</span></div>
      <header className="pc-header">
        <a className="pc-wordmark" href="/" aria-label="Plantupia existing website">plantupia<span aria-hidden="true">✳</span></a>
        <SiteIndex current="experience" tone="dark" />
        <div className="pc-header-aside"><span>SPACE / LIFE / CARE</span></div>
      </header>

      <main id="pc-content">
        <section className="pc-arrival" aria-labelledby="pc-arrival-title">
          <div className="pc-arrival-images" role="img" aria-label={current.title + " — " + current.source + ", proposed design imagery"}>
            {places.map((place, index) => <img key={place.image} src={asset(place.image)} alt="" aria-hidden="true" className={index === scene ? "is-visible" : ""} loading={index === 0 ? "eager" : "lazy"} />)}
          </div>
          <div className="pc-arrival-shade" aria-hidden="true" />
          <div className="pc-arrival-meta"><span>01 / ENCOUNTER</span><span>IMAGE FIRST, INTERFACE SECOND</span></div>
          <div className="pc-arrival-title">
            <span className="pc-kicker">A SPATIAL READING / 01—03</span>
            <h1 id="pc-arrival-title">Living, <em>in place.</em></h1>
            <p>One visual language for space, plants and what continues.</p>
          </div>
          <div className="pc-arrival-bottom">
            <div className="pc-caption"><span>{String(scene + 1).padStart(2, "0")} / 03 · {current.title}</span><small>{current.source} · DESIGN PROPOSAL, NOT A VERIFIED INSTALLATION</small></div>
            <div className="pc-scene-controls" role="group" aria-label="Choose an architectural view">
              {places.map((place, index) => <button key={place.image} type="button" onClick={() => setScene(index)} aria-pressed={scene === index} aria-label={"View " + place.title}><span>0{index + 1}</span></button>)}
            </div>
            <a className="pc-scroll-cue" href="#pc-choice">SEE THE CHOICE <span aria-hidden="true">↓</span></a>
          </div>
        </section>

        <section className="pc-choice" id="pc-choice" aria-labelledby="pc-choice-title">
          <div className="pc-section-meta"><span>02 / THE DECISION</span><span>MAZAL / TWO PLANTING OPTIONS</span></div>
          <div className="pc-choice-intro"><h2 id="pc-choice-title">One place.<br/><em>More than one life.</em></h2><p>Hold the architecture still. Change the planting choice. The images, not the animation, should explain the difference.</p></div>
          <div className="pc-compare">
            <img src={asset("mazal-option-05.webp")} alt="Mazal facade — dense green planting proposal, option 05" className="pc-compare-under" />
            <img src={asset("mazal-option-01.webp")} alt="Mazal facade — tropical planting proposal, option 01" className="pc-compare-over" style={{clipPath:"inset(0 "+(100-comparison)+"% 0 0)"}} />
            <span className="pc-compare-divider" style={{left:comparison+"%"}} aria-hidden="true"><i>↔</i></span>
            <span className="pc-compare-label pc-compare-label-left">01 / TROPICAL</span>
            <span className="pc-compare-label pc-compare-label-right">05 / DENSE GREEN</span>
          </div>
          <div className="pc-compare-controls"><label htmlFor="pc-compare-range">MOVE BETWEEN DIRECTIONS <strong>{comparison}%</strong></label><input id="pc-compare-range" type="range" min={0} max={100} step={1} value={comparison} onChange={(event) => setComparison(Number(event.target.value))}/><p>MAZAL / DESIGN OPTIONS, NOT BEFORE & AFTER</p></div>
        </section>

        <section className="pc-material" id="pc-material" aria-labelledby="pc-material-title">
          <div className="pc-section-meta"><span>03 / THE MATERIAL</span><span>CONTEXT / OBJECT / FAMILY</span></div>
          <div className="pc-material-layout">
            <div className="pc-material-reading"><h2 id="pc-material-title">Closer to<br/><em>the living thing.</em></h2><p>The way an object is seen changes with its context. We don't need to force every image into the same card.</p><div className="pc-material-choices" role="group" aria-label="Choose the material reading">{objects.map((item,index)=><button type="button" key={item.image} aria-pressed={object===index} onClick={()=>setObject(index)}><span>{item.number}</span><strong>{item.title}</strong><i aria-hidden="true">↗</i></button>)}</div></div>
            <figure className="pc-material-visual"><div className="pc-material-photo" role="img" aria-label={selected.title+" — "+selected.source}>{objects.map((item,index)=><img key={item.image} src={asset(item.image)} alt="" aria-hidden="true" className={object===index?"is-visible":""} loading="lazy"/>)}</div><figcaption><span>{selected.source} / CATALOGUE STUDY</span><strong>{selected.description}</strong></figcaption></figure>
          </div>
        </section>

        <section className="pc-continuity" aria-labelledby="pc-continuity-title"><span>04 / AFTER THE IMAGE</span><h2 id="pc-continuity-title">A living space<br/><em>doesn't end at handover.</em></h2><p>The visual encounter leads to the actual product: spaces, plants, ownership and ongoing care. This is a creative reading, not a claim of measured growth or sensor data.</p><div><a href="/living-systems">EXPLORE LIVING SYSTEMS <span>↗</span></a><a href="/shop">EXPLORE THE COLLECTION <span>↗</span></a></div></section>
      </main>
      <footer className="pc-footer"><span>PRIVATE STUDY / 22 NOV 2026</span><span>ARCHIVE-LED / SOURCE-LABELLED / NO PRODUCTION CHANGES</span><a href="/study/creative-lab">EARLIER EXPERIMENTS ↗</a></footer>
    </div>
  );
}
