"use client";

import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { pixelsPrinciples, pixelsSequence } from "./pixels-case-data";
import "./space-explorer.css";
import "./pixels-case.css";

function imageUrl(name:string){ return "/api/private-arrival/" + name; }

export default function PixelsCaseStudy() {
  const [active, setActive] = useState(0);
  const scene = pixelsSequence[active];

  function step(delta:number){
    setActive(index => (index + delta + pixelsSequence.length) % pixelsSequence.length);
  }

  return <div className="sx-page px-page">
    <a className="sx-skip" href="#px-field">Skip to Pixels Mall field</a>
    <header className="sx-header px-header">
      <a href="/" className="sx-brand">Plantupia</a>
      <SiteIndex current="spaces" />
      <a className="sx-contact" href="/start-a-space?space=retail">Get in touch <span aria-hidden="true">↗</span></a>
    </header>

    <main id="px-main" className="px-main">
      <section className="px-intro" aria-labelledby="px-title">
        <div className="px-intro-id">
          <a href="/spaces/projects">← Project archive</a>
          <span>FIELD 04 / PIXELS MALL</span>
        </div>
        <div className="px-intro-title">
          <p className="sx-kicker">COMMERCIAL / NEW CAIRO / DESIGN PROPOSAL</p>
          <h1 id="px-title">A landscape<br/><em>through the building.</em></h1>
        </div>
        <div className="px-intro-note">
          <p>Eight proposal views read as one spatial route: arrival, promenade, threshold, terrace, facade and interior continuity.</p>
          <dl>
            <div><dt>Context</dt><dd>High-end commercial destination</dd></div>
            <div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div>
          </dl>
        </div>
      </section>

      <section id="px-field" className="px-field" aria-label="Pixels Mall spatial field">
        <figure className="px-field-stage">
          <img key={scene.image} src={imageUrl(scene.image)} alt={"Pixels Mall proposal visualization, page " + scene.page} />
          <div className="px-field-overlay" aria-hidden="true"/>
          <figcaption>
            <span>SCENE {scene.id} / SOURCE P{scene.page}</span>
            <strong>{scene.title}</strong>
          </figcaption>
          <div className="px-field-step" aria-label="Move through the Pixels Mall proposal">
            <button type="button" onClick={()=>step(-1)} aria-label="Previous scene">←</button>
            <span>{scene.id} / {String(pixelsSequence.length).padStart(2,"0")}</span>
            <button type="button" onClick={()=>step(1)} aria-label="Next scene">→</button>
          </div>
        </figure>

        <aside className="px-field-reading" aria-live="polite">
          <div className="px-field-reading-head">
            <span>FIELD NOTE</span>
            <span>{scene.tone.toUpperCase()}</span>
          </div>
          <p>{scene.note}</p>
          <div className="px-field-rule"/>
          <small>Descriptive reading of proposal imagery — not a source chapter title.</small>
        </aside>
      </section>

      <section className="px-route" aria-label="Pixels Mall route index">
        <header><span>ROUTE INDEX</span><span>08 SCENES</span></header>
        <div className="px-route-strip">
          {pixelsSequence.map((item,index)=><button key={item.id} type="button" aria-current={index===active?"true":undefined} onClick={()=>setActive(index)}>
            <figure>
              <img src={imageUrl(item.image)} alt="" loading={index<3?"eager":"lazy"} />
              <figcaption><span>{item.id}</span><strong>{item.title}</strong></figcaption>
            </figure>
          </button>)}
        </div>
      </section>

      <section className="px-source-grid" aria-label="Pixels Mall project context">
        <article className="px-source-summary">
          <p className="sx-kicker">SOURCE READING</p>
          <h2>One place.<br/>Several conditions.</h2>
          <p>The source material is strongest when read as a sequence rather than eight isolated portfolio images. Planting appears at arrival, along circulation, at edges, against facades and inside the commercial volume.</p>
        </article>
        <article className="px-source-note">
          <span>VISUAL SOURCE NOTE</span>
          <p>All imagery remains inside its proposal context. Nothing here is presented as evidence of completed construction, installation or client approval.</p>
        </article>
      </section>

      <section className="px-principles" aria-labelledby="px-principles-title">
        <div>
          <p className="sx-kicker">DESIGN PRINCIPLES / SOURCE P02</p>
          <h2 id="px-principles-title">Five intentions.<br/>One continuous landscape.</h2>
          <p>The proposal names these principles explicitly. They explain the field after the visitor has already seen it.</p>
        </div>
        <ol>{pixelsPrinciples.map(([n,t])=><li key={n}><span>{n}</span><strong>{t}</strong></li>)}</ol>
      </section>

      <section className="px-next">
        <p className="sx-kicker">YOUR COMMERCIAL DESTINATION</p>
        <h2>Begin with the architecture.</h2>
        <a className="sx-underlink" href="/start-a-space?space=retail">Discuss a commercial project ↗</a>
      </section>
    </main>

    <footer className="sx-footer">
      <a href="/spaces/projects">Project archive</a>
      <span>Plantupia / Pixels Mall proposal field</span>
      <a href="/spaces/commercial">Commercial ↗</a>
    </footer>
  </div>;
}
