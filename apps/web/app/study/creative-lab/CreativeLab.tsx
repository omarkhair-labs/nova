"use client";

import { useEffect, useRef, useState } from "react";
import "./creative-lab.css";

type Direction = "lumen" | "strata" | "afterimage";
const asset = (name: string) => "/api/private-arrival/" + name;

const directionNames: { id: Direction; number: string; name: string; cue: string }[] = [
  { id: "lumen", number: "01", name: "LUMEN", cue: "Light becomes navigation" },
  { id: "strata", number: "02", name: "STRATA", cue: "Material becomes choice" },
  { id: "afterimage", number: "03", name: "AFTERIMAGE", cue: "Time becomes continuity" },
];

const lightFrames = [
  {
    image: "home-pixels-courtyard.webp",
    name: "The courtyard",
    source: "PIXELS MALL / PROPOSAL P03",
    number: "01",
    line: "Architecture makes an opening.",
    note: "A planted landmark changes how the entrance is perceived.",
  },
  {
    image: "home-audi-lounge.webp",
    name: "The interior",
    source: "AUDI CAIRO / PROPOSAL P05",
    number: "02",
    line: "Living material takes its place.",
    note: "The planted layer changes the scale of a room.",
  },
  {
    image: "home-villa-terrace.webp",
    name: "The terrace",
    source: "VILLA 7 / PROPOSAL P17",
    number: "03",
    line: "The outside comes closer.",
    note: "Light, planting and use meet in one proposed setting.",
  },
] as const;

const materialStudies = [
  {
    image: "shop-augusta-wide.webp",
    name: "In the room",
    type: "SETTING",
    source: "HOSPITALITY CATALOGUE / P27",
    line: "The object belongs to a larger composition.",
  },
  {
    image: "shop-augusta-object.webp",
    name: "The vessel",
    type: "OBJECT",
    source: "HOSPITALITY CATALOGUE / P27",
    line: "The planter holds a decision about scale and proportion.",
  },
  {
    image: "shop-fiberglass-pots.webp",
    name: "The family",
    type: "MATERIAL",
    source: "HOSPITALITY CATALOGUE / P37",
    line: "A catalogue family opens several spatial possibilities.",
  },
] as const;

const traceMoments = [
  {
    image: "audi-chapter-01.webp",
    chapter: "READ",
    title: "See the condition.",
    number: "I",
    source: "AUDI CAIRO / ENTRANCE PROPOSAL",
    note: "A place is read before living material is introduced.",
    shade: "arrival",
  },
  {
    image: "audi-chapter-03.webp",
    chapter: "COMPOSE",
    title: "Give life a place.",
    number: "II",
    source: "AUDI CAIRO / ROOM PROPOSAL",
    note: "A planting choice changes the composition of a room.",
    shade: "material",
  },
  {
    image: "world-systems.webp",
    chapter: "RETURN",
    title: "Design for what follows.",
    number: "III",
    source: "EL HALLAB / PLANTING OPTION",
    note: "Light, water, ownership and maintenance become part of the brief.",
    shade: "return",
  },
] as const;

export default function CreativeLab() {
  const root = useRef<HTMLDivElement>(null);
  const target = useRef({ x: 65, y: 38 });
  const [direction, setDirection] = useState<Direction>("lumen");
  const [light, setLight] = useState(0);
  const [compare, setCompare] = useState(52);
  const [material, setMaterial] = useState(0);
  const [moment, setMoment] = useState(0);
  const [navigationOpen, setNavigationOpen] = useState(false);
  const selectedLight = lightFrames[light];
  const selectedMaterial = materialStudies[material];
  const selectedMoment = traceMoments[moment];

  useEffect(() => {
    let frame = 0;
    let currentX = 65;
    let currentY = 38;
    const update = () => {
      currentX += (target.current.x - currentX) * 0.055;
      currentY += (target.current.y - currentY) * 0.055;
      root.current?.style.setProperty("--lab-x", currentX.toFixed(2) + "%");
      root.current?.style.setProperty("--lab-y", currentY.toFixed(2) + "%");
      frame = requestAnimationFrame(update);
    };
    frame = requestAnimationFrame(update);
    return () => cancelAnimationFrame(frame);
  }, []);

  function choose(next: Direction) {
    setDirection(next);
    setNavigationOpen(false);
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    window.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" });
  }

  return (
    <div
      className="cl-root"
      data-direction={direction}
      ref={root}
      onPointerMove={(event) => {
        if (event.pointerType !== "mouse") return;
        target.current = {
          x: Math.max(0, Math.min(100, (event.clientX / window.innerWidth) * 100)),
          y: Math.max(0, Math.min(100, (event.clientY / window.innerHeight) * 100)),
        };
      }}
    >
      <a className="cl-skip" href="#cl-main">Skip to current direction</a>
      <div className="cl-labline">
        <span>PLANTUPIA / CREATIVE R&D</span>
        <span>THREE UNRELEASED DIRECTIONS — SOURCE-LED</span>
        <a href="/">EXISTING WEBSITE ↗</a>
      </div>

      <header className="cl-topbar">
        <a className="cl-logo" href="/" aria-label="Plantupia existing site">plantupia<span className="cl-logo-mark">✳</span></a>
        <nav aria-label="Visual directions" className="cl-directions">
          {directionNames.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => choose(item.id)}
              aria-pressed={direction === item.id}
              title={item.cue}
            >
              <span>{item.number}</span> {item.name}
            </button>
          ))}
        </nav>
        <button className="cl-menu" type="button" aria-expanded={navigationOpen} onClick={() => setNavigationOpen((o) => !o)}>
          {navigationOpen ? "CLOSE —" : "INDEX +"}
        </button>
      </header>

      {navigationOpen && (
        <div className="cl-index-panel">
          <p>NOT THREE COLOURWAYS. THREE DIFFERENT BEHAVIOURS.</p>
          {directionNames.map((item) => (
            <button type="button" key={item.id} onClick={() => choose(item.id)}>
              <span>{item.number}</span>
              <strong>{item.name}</strong>
              <small>{item.cue}</small>
              <i aria-hidden="true">↗</i>
            </button>
          ))}
        </div>
      )}

      <main id="cl-main">
        {direction === "lumen" && (
          <>
            <section className="cl-lumen-opening" aria-labelledby="cl-lumen-title">
              <div className="cl-lumen-stage" role="img" aria-label={selectedLight.name + " — " + selectedLight.source}>
                {lightFrames.map((frame, index) => (
                  <img
                    key={frame.image}
                    className={"cl-lumen-frame" + (light === index ? " is-active" : "")}
                    src={asset(frame.image)}
                    alt=""
                    loading={index === 0 ? "eager" : "lazy"}
                  />
                ))}
                <div className="cl-lumen-veil" aria-hidden="true" />
                <div className="cl-lumen-light" aria-hidden="true" />
              </div>
              <div className="cl-lumen-top-note">
                <span>01 / LIGHT AS ARCHITECTURE</span><span>LIGHT FOLLOWS THE EYE</span>
              </div>
              <div className="cl-lumen-center">
                <p>PLANTUPIA / SPATIAL DESIGN</p>
                <h1 id="cl-lumen-title">Space is not<br/><em>still.</em></h1>
              </div>
              <div className="cl-lumen-bottom">
                <div className="cl-lumen-story" aria-live="polite">
                  <span>{selectedLight.number} / 03 — {selectedLight.name.toUpperCase()}</span>
                  <strong>{selectedLight.line}</strong>
                  <small>{selectedLight.source} · DESIGN PROPOSAL</small>
                </div>
                <div className="cl-lumen-controls" role="group" aria-label="Move between spatial photographs">
                  <button type="button" onClick={() => setLight((i) => (i + 2) % 3)} aria-label="Previous spatial proposal">←</button>
                  <span>{selectedLight.number} — 03</span>
                  <button type="button" onClick={() => setLight((i) => (i + 1) % 3)} aria-label="Next spatial proposal">→</button>
                </div>
                <a className="cl-lumen-enter" href="#cl-lumen-work">DISCOVER THE FIELD <span>↓</span></a>
              </div>
            </section>
            <section className="cl-lumen-work" id="cl-lumen-work" aria-labelledby="cl-lumen-work-title">
              <div className="cl-section-register"><span>01: FIELD NOTES</span><span>PROPOSAL IMAGERY / REAL SOURCES</span></div>
              <div className="cl-lumen-work-head">
                <h2 id="cl-lumen-work-title">The work is the<br/><em>landscape.</em></h2>
                <p>Not a catalogue of projects. Architectural conditions, living material and the spaces they change.</p>
              </div>
              <div className="cl-lumen-works-composition">
                <a className="cl-lumen-work-a" href="/spaces/projects/pixels-mall">
                  <figure><img src={asset("home-pixels-promenade.webp")} alt="Pixels Mall planted promenade proposal" loading="lazy"/><figcaption><span>01 / PIXELS MALL</span><strong>A planted route</strong><small>COMMERCIAL / PROPOSAL P16</small></figcaption></figure>
                </a>
                <a className="cl-lumen-work-b" href="/spaces/projects/audi-cairo">
                  <figure><img src={asset("home-audi-brand.webp")} alt="Audi Cairo planting proposal, framing the brand" loading="lazy"/><figcaption><span>02 / AUDI CAIRO</span><strong>Light meets identity</strong><small>INTERIOR / PROPOSAL P14</small></figcaption></figure>
                </a>
              </div>
            </section>
            <section className="cl-lumen-exit" aria-label="Continuity and commerce">
              <img src={asset("shop-indoor-greenery.webp")} alt="Indoor greenery catalogue spatial study" loading="lazy"/>
              <div><span>LIGHT IS ONLY THE BEGINNING</span><h2>Then something<br/><em>has to live.</em></h2><p>Design / material / care / return. The physical world and its continuing system belong in the same story.</p><div><a href="/living-systems">LIVING SYSTEMS ↗</a><a href="/shop">EXPLORE OBJECTS ↗</a></div></div>
            </section>
          </>
        )}

        {direction === "strata" && (
          <>
            <section className="cl-strata-opening" aria-labelledby="cl-strata-title">
              <div className="cl-strata-head">
                <div className="cl-strata-stamp"><span>02 / MATERIAL STUDY</span><small>SAME ARCHITECTURAL FRAME / TWO PLANTING DIRECTIONS</small></div>
                <h1 id="cl-strata-title">One place.<br/><em>Many lives.</em></h1>
                <p>Move the seam. The architecture stays; the planting direction changes.</p>
              </div>
              <div className="cl-strata-comparison">
                <img className="cl-strata-base" src={asset("mazal-option-05.webp")} alt="Mazal facade, dense green proposal option 05"/>
                <img className="cl-strata-overlay" src={asset("mazal-option-01.webp")} alt="Mazal facade, tropical blend proposal option 01" style={{ clipPath: "inset(0 " + (100 - compare) + "% 0 0)" }}/>
                <div className="cl-strata-seam" style={{ left: compare + "%" }} aria-hidden="true"><span>↔</span></div>
                <div className="cl-strata-image-label cl-strata-left">01 / TROPICAL BLEND</div>
                <div className="cl-strata-image-label cl-strata-right">05 / DENSE GREEN</div>
              </div>
              <div className="cl-strata-range">
                <label htmlFor="cl-strata-slider">RECOMPOSE THE PLANTING <strong>{compare.toString().padStart(2, "0")}%</strong></label>
                <input id="cl-strata-slider" type="range" min="0" max="100" value={compare} onChange={(e) => setCompare(Number(e.target.value))} aria-label="Compare Mazal planting proposal alternatives"/>
                <span>MAZAL / TWO OF NINE DESIGN OPTIONS · NOT AN INSTALLED BEFORE/AFTER</span>
              </div>
            </section>
            <section className="cl-strata-material" aria-labelledby="cl-strata-material-title">
              <div className="cl-section-register"><span>OBJECT STUDY / 03</span><span>SETTING — VESSEL — MATERIAL</span></div>
              <div className="cl-strata-material-head">
                <h2 id="cl-strata-material-title">The vessel is<br/><em>architecture.</em></h2>
                <p>Choose how to read the same material family. The composition changes without inventing a new product.</p>
              </div>
              <div className="cl-strata-material-field">
                <figure>
                  {materialStudies.map((study, index) => (
                    <img key={study.image} src={asset(study.image)} alt={study.name + " — " + study.source} className={index === material ? "is-active" : ""} loading="lazy"/>
                  ))}
                  <figcaption>{selectedMaterial.source} / CATALOGUE STUDY</figcaption>
                </figure>
                <div className="cl-strata-material-index" role="group" aria-label="Material study viewpoints">
                  {materialStudies.map((study, index) => (
                    <button type="button" key={study.image} aria-pressed={index === material} onClick={() => setMaterial(index)}>
                      <span>0{index + 1} / {study.type}</span><strong>{study.name}</strong><small>{study.line}</small><i aria-hidden="true">↗</i>
                    </button>
                  ))}
                </div>
              </div>
            </section>
            <section className="cl-strata-outro">
              <span>SPACE × OBJECT × CARE</span>
              <h2>Design is not<br/>a single frame.</h2>
              <div><a href="/spaces/projects/mazal">OPEN MAZAL ↗</a><a href="/shop">FIND THE OBJECT ↗</a><a href="/start-a-space">START A SPACE ↗</a></div>
            </section>
          </>
        )}

        {direction === "afterimage" && (
          <>
            <section className="cl-after-opening" data-moment={selectedMoment.shade} aria-labelledby="cl-after-title">
              <div className="cl-after-atmosphere" aria-hidden="true" />
              <div className="cl-after-register"><span>03 / THE AFTERIMAGE</span><span>SPACE · HUMAN CARE · TIME</span></div>
              <div className="cl-after-layout">
                <div className="cl-after-copy">
                  <span className="cl-after-roman">{selectedMoment.number} / III</span>
                  <p>PLANTUPIA / CONTINUITY</p>
                  <h1 id="cl-after-title">What remains<br/><em>alive?</em></h1>
                  <div className="cl-after-dynamic" aria-live="polite"><strong>{selectedMoment.title}</strong><span>{selectedMoment.note}</span></div>
                </div>
                <div className="cl-after-visual">
                  {traceMoments.map((item, index) => <img key={item.image} className={index === moment ? "is-active" : ""} src={asset(item.image)} alt={item.source} loading={index === 0 ? "eager" : "lazy"}/>)}
                  <div className="cl-after-dust" aria-hidden="true"/>
                  <div className="cl-after-vertical">SOURCE IMAGE / NOT A DOCUMENTED TIME-LAPSE</div>
                  <div className="cl-after-image-note">{selectedMoment.source}<span>DESIGN STUDY</span></div>
                </div>
              </div>
              <div className="cl-after-controls" role="group" aria-label="Read the living system">
                {traceMoments.map((item, index) => (
                  <button type="button" key={item.chapter} aria-pressed={moment === index} onClick={() => setMoment(index)}>
                    <span>{item.number}</span><strong>{item.chapter}</strong><i aria-hidden="true">↗</i>
                  </button>
                ))}
              </div>
            </section>
            <section className="cl-after-record" aria-labelledby="cl-after-record-title">
              <div className="cl-section-register"><span>A LIVING SYSTEM</span><span>DESIGN / INSTALL / RETURN / REMEMBER</span></div>
              <div className="cl-after-record-grid">
                <div><span>NO INVENTED SENSOR FEEDS</span><h2 id="cl-after-record-title">The quiet work<br/>after the image.</h2><p>Green technology is useful when it changes real care: handover, plant ownership, maintenance routes and the record of human observations.</p><a href="/living-systems">EXPLORE LIVING SYSTEMS ↗</a></div>
                <figure><img src={asset("shop-office-greenery.webp")} alt="Executive office greenery catalogue design study" loading="lazy"/><figcaption>HOSPITALITY CATALOGUE P17 / A SPATIAL EXAMPLE, NOT A CARE RECORD</figcaption></figure>
              </div>
            </section>
            <section className="cl-after-ending"><p>THE NEXT VISIT IS PART OF THE DESIGN.</p><a href="/start-a-space">BEGIN WITH A PLACE <span>↗</span></a></section>
          </>
        )}
      </main>

      <footer className="cl-footer">
        <span>PLANTUPIA / INTERNAL DESIGN LAB / 22 NOV 2026</span>
        <span>REAL PROPOSAL IMAGES — NO CLAIM OF COMPLETED INSTALLATION</span>
        <a href="/">RETURN TO EXISTING WEBSITE ↗</a>
      </footer>
    </div>
  );
}
