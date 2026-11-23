import MobileReadingRibbon from "./MobileReadingRibbon";
"use client";

import SiteIndex from "./SiteIndex";
import ProjectLeadImage from "./ProjectLeadImage";
import CaseTrail from "./CaseTrail";
import { PixelsArchiveExpansion } from "./ArchiveExpansion";
import { pixelsPrinciples, pixelsSequence } from "./pixels-case-data";
import "./space-explorer.css";
import "./pixels-case.css";

function SequenceFrame({ frame, index }: { frame: (typeof pixelsSequence)[number]; index: number }) {
  return <figure className={"px-frame px-frame--" + frame.tone + (index % 2 ? " px-frame--reverse" : "")}>
    <div className="px-frame-media">
      <img src={"/api/private-arrival/" + frame.image} alt={"Pixels Mall proposal visualization, page " + frame.page} loading="lazy" />
    </div>
    <figcaption>
      <span className="px-frame-no">{frame.id}</span>
      <div>
        <p className="sx-kicker">PROPOSAL VISUAL / P{frame.page}</p>
        <h3>{frame.title}</h3>
        <p>{frame.note}</p>
      </div>
    </figcaption>
  </figure>;
}

export default function PixelsCaseStudy() {
  return <div className="sx-page px-page">
    <a className="sx-skip" href="#px-main">Skip to Pixels Mall study</a>
    <header className="sx-header">
      <a href="/" className="sx-brand">Plantupia</a>
      <SiteIndex current="spaces" />
      <a className="sx-contact" href="/start-a-space?space=retail">Get in touch <span aria-hidden="true">&#8599;</span></a>
    </header>

    <MobileReadingRibbon number="11" title="Pixels Mall" stops={[{label:"The landscape",href:"#px-scenes"},{label:"Principles",href:"#px-principles-title"},{label:"The system",href:"#px-system-title"}]} returnHref="/spaces/projects"/>
    <main id="px-main" className="px-main">
      <div className="px-crumb">
        <a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a>
        <span>Commercial / Architecture / Proposal</span>
      </div>

      <section className="px-hero" aria-labelledby="px-title">
        <div>
          <p className="sx-kicker">CASE STUDY 04 / MT ARCHITECTS / NEW CAIRO</p>
          <h1 id="px-title">A landscape<br/><em>through the building.</em></h1>
        </div>
        <div className="px-hero-copy">
          <p>The proposal imagines planting as part of the commercial destination itself: arrival marker, circulation layer, terrace edge, facade material and interior atmosphere. The visual captions below are editorial readings of those proposal frames.</p>
          <dl>
            <div><dt>Context</dt><dd>High-end commercial destination</dd></div>
            <div><dt>Location</dt><dd>New Cairo</dd></div>
            <div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div>
          </dl>
        </div>

<ProjectLeadImage project="pixels-mall" />
</section>

      <section id="px-scenes" className="px-sequence" aria-label="Pixels Mall proposal visual sequence">
        {pixelsSequence.slice(0, 2).map((frame, index) => <SequenceFrame key={frame.id} frame={frame} index={index} />)}
      </section>

      <section className="px-principles" aria-labelledby="px-principles-title">
        <div>
          <p className="sx-kicker">DESIGN PRINCIPLES / SOURCE P02</p>
          <h2 id="px-principles-title">Five intentions.<br/>One continuous landscape.</h2>
          <p>The proposal names these five principles explicitly. They interrupt the image sequence once, then the project returns to space and movement.</p>
        </div>
        <ol>{pixelsPrinciples.map(([n, t]) => <li key={n}><span>{n}</span><strong>{t}</strong></li>)}</ol>
      </section>

      <section className="px-sequence px-sequence--continuation" aria-label="Pixels Mall proposal sequence continued">
        {pixelsSequence.slice(2).map((frame, index) => <SequenceFrame key={frame.id} frame={frame} index={index + 2} />)}
      </section>

      <PixelsArchiveExpansion />

      <section className="px-system" aria-labelledby="px-system-title">
        <div>
          <p className="sx-kicker">WHAT THE SEQUENCE REVEALS</p>
          <h2 id="px-system-title">Not pots in a mall.<br/>A planted spatial system.</h2>
        </div>
        <div className="px-system-grid">
          <article><span>01</span><h3>Landmarks</h3><p>Planting establishes recognizable spatial markers at arrival and circulation points.</p></article>
          <article><span>02</span><h3>Continuity</h3><p>Greenery repeats across ground, terrace, facade and interior conditions.</p></article>
          <article><span>03</span><h3>Edges</h3><p>Planters work with architectural boundaries, curves and thresholds rather than floating independently.</p></article>
          <article><span>04</span><h3>Care</h3><p>Low-maintenance intent belongs to the system, but the visual story leads before operational detail.</p></article>
        </div>
      </section>

      <section className="px-source-note">
        <span>VISUAL SOURCE NOTE</span>
        <p>All imagery on this page is retained inside its proposal context. Nothing here is presented as evidence of completed construction, installation or client approval.</p>
      </section>

      <section className="px-next">
        <p className="sx-kicker">YOUR COMMERCIAL DESTINATION</p>
        <h2>Begin with the architecture.</h2>
        <a className="sx-underlink" href="/start-a-space?space=retail">Discuss a commercial project &#8599;</a>
      </section>
    <CaseTrail current="pixels-mall" />
</main>

    <footer className="sx-footer">
      <a href="/spaces/projects">Project archive</a>
      <span>Plantupia / Pixels Mall proposal study</span>
      <a href="/spaces/commercial">Commercial &#8599;</a>
    </footer>
  </div>;
}
