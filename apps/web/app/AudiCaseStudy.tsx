"use client";

import SiteIndex from "./SiteIndex";
import { audiChapters, audiPalette } from "./audi-case-data";
import "./space-explorer.css";
import "./audi-case.css";

function Chapter({ chapter, index }: { chapter: (typeof audiChapters)[number]; index: number }) {
  return <article className={"au-chapter" + (index % 2 ? " au-chapter--reverse" : "")}>
    <figure>
      <img
        src={"/api/private-arrival/" + chapter.image}
        alt={"Audi Cairo proposal, chapter " + chapter.id + ", " + chapter.name}
        loading={index < 2 ? "eager" : "lazy"}
      />
      <figcaption>PROPOSAL P{chapter.page} / DESIGN VISUALIZATION</figcaption>
    </figure>
    <div className="au-chapter-copy">
      <span className="au-chapter-no">0{chapter.id}</span>
      <div>
        <p className="sx-kicker">LIVING INTERIORS / CHAPTER 0{chapter.id}</p>
        <h3>{chapter.name}</h3>
        <p>The chapter name comes directly from the proposal. The image remains presented as a design visualization, not as evidence of a completed installation.</p>
      </div>
    </div>
  </article>;
}

export default function AudiCaseStudy() {
  return <div className="sx-page au-page">
    <a className="sx-skip" href="#au-main">Skip to Audi study</a>
    <header className="sx-header">
      <a href="/" className="sx-brand">Plantupia</a>
      <SiteIndex current="spaces" />
      <a className="sx-contact" href="/start-a-space?space=retail">Get in touch <span aria-hidden="true">&#8599;</span></a>
    </header>

    <main id="au-main" className="au-main">
      <div className="au-crumb">
        <a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a>
        <span>Commercial / Showroom / Natural planting concept</span>
      </div>

      <section className="au-hero" aria-labelledby="au-title">
        <div>
          <p className="sx-kicker">CASE STUDY 05 / AUDI CAIRO CENTER</p>
          <h1 id="au-title">Six rooms.<br/><em>One living showroom.</em></h1>
        </div>
        <div>
          <p>The proposal moves through six specific showroom moments before it reaches the plant palette and care programme. The digital story now follows that movement instead of turning the chapters into tabs.</p>
          <dl>
            <div><dt>Format</dt><dd>Six spatial chapters</dd></div>
            <div><dt>Proposal</dt><dd>October 2026</dd></div>
            <div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div>
          </dl>
        </div>
      </section>

      <section className="au-sequence" aria-label="Audi Cairo proposal chapters">
        <header className="au-sequence-head">
          <p className="sx-kicker">CHAPTERS 01—06</p>
          <h2>Move through<br/>the showroom.</h2>
          <p>International showroom references on source page 4 are excluded from this sequence and are not represented as Plantupia work.</p>
        </header>
        {audiChapters.map((chapter, index) => <Chapter key={chapter.id} chapter={chapter} index={index} />)}
      </section>

      <section className="au-palette" aria-labelledby="au-palette-title">
        <div>
          <p className="sx-kicker">PLANT PALETTE / P16</p>
          <h2 id="au-palette-title">The living<br/>collection.</h2>
          <p>Twelve plant names and botanical labels are transcribed from the proposal. Here they read as the material vocabulary behind the six spatial chapters.</p>
        </div>
        <ol>{audiPalette.map(([name, latin], i) => <li key={name}><span>{String(i + 1).padStart(2, "0")}</span><strong>{name}</strong><em>{latin}</em></li>)}</ol>
      </section>

      <section className="au-care" aria-labelledby="au-care-title">
        <div>
          <p className="sx-kicker">CARE PROGRAMME / P17</p>
          <h2 id="au-care-title">The project does not end at installation.</h2>
          <p>The source continues from design into operation, which is why care sits after the visual journey rather than as a detached service page.</p>
        </div>
        <ol>
          <li><span>01</span>Selection &amp; installation</li>
          <li><span>02</span>Smart irrigation</li>
          <li><span>03</span>Scheduled maintenance</li>
          <li><span>04</span>Plant warranty</li>
        </ol>
      </section>

      <section className="au-source-note">
        <span>VISUAL SOURCE NOTE</span>
        <p>The six images are proposal visuals. No page on this review presents them as completed Audi Cairo installations or as client-approved final work.</p>
      </section>

      <section className="au-next">
        <p className="sx-kicker">NEXT STEP / P18</p>
        <h2>Bring the showroom to life.</h2>
        <a className="sx-underlink" href="/start-a-space?space=retail">Discuss a commercial project &#8599;</a>
      </section>
    </main>

    <footer className="sx-footer">
      <a href="/spaces/projects">Project archive</a>
      <span>Plantupia / Audi Cairo proposal study</span>
      <a href="/spaces/commercial">Commercial &#8599;</a>
    </footer>
  </div>;
}
