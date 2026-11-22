"use client";

import SiteIndex from "./SiteIndex";
import ProjectLeadImage from "./ProjectLeadImage";
import CaseTrail from "./CaseTrail";
import { villaAreas, villaChoices } from "./villa-case-data";
import "./space-explorer.css";
import "./villa-case.css";

function AreaChapter({ area }: { area: (typeof villaAreas)[number] }) {
  const choices = villaChoices.filter(choice => choice.area === area.id);
  return <section className="vl-area" id={"villa-area-" + area.id} aria-labelledby={"villa-area-title-" + area.id}>
    <header className="vl-area-head">
      <div>
        <p className="sx-kicker">{area.context.toUpperCase()} / AREA {String(area.id).padStart(2, "0")}</p>
        <h2 id={"villa-area-title-" + area.id}>{area.name}</h2>
      </div>
      <p>{choices.length === 1 ? "One proposal direction for this space." : choices.length + " proposal directions for the same space."}</p>
    </header>
    <div className={"vl-visual-field vl-visual-field--" + choices.length}>
      {choices.map((choice, index) => <figure className="vl-option" key={choice.id}>
        <div className="vl-option-media">
          <img src={"/api/private-arrival/" + choice.asset} alt={"Villa 7 " + choice.areaName + ", option " + choice.letter + ", " + choice.title} loading="lazy" />
        </div>
        <figcaption>
          <span className="vl-option-letter">{choice.letter}</span>
          <div>
            <p className="sx-kicker">OPTION {choice.letter} / PROPOSAL P{choice.page}</p>
            <h3>{choice.title}</h3>
            <p>{choice.description}</p>
            <small>{choice.plants}</small>
          </div>
        </figcaption>
      </figure>)}
    </div>
  </section>;
}

export default function VillaCaseStudy() {
  return <div className="sx-page vl-page">
    <a className="sx-skip" href="#vl-main">Skip to Villa 7 study</a>
    <header className="sx-header">
      <a href="/" className="sx-brand">Plantupia</a>
      <SiteIndex current="spaces" />
      <a className="sx-contact" href="/start-a-space?space=home">Get in touch <span aria-hidden="true">&#8599;</span></a>
    </header>

    <main id="vl-main" className="vl-main">
      <div className="vl-crumb">
        <a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a>
        <span>Residential / Landscape &amp; interior planting / Proposal</span>
      </div>

      <section className="vl-hero" aria-labelledby="vl-title">
        <div>
          <p className="sx-kicker">CASE STUDY 06 / VILLA 7</p>
          <h1 id="vl-title">24 ways<br/><em>through one home.</em></h1>
        </div>
        <div>
          <p>The proposal covers ten indoor and outdoor areas through twenty-four planting choices. Instead of turning those choices into a long selection interface, this study lets every space become its own visual chapter.</p>
          <dl>
            <div><dt>Areas</dt><dd>10 spatial chapters</dd></div>
            <div><dt>Choices</dt><dd>24 proposal directions</dd></div>
            <div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div>
          </dl>
        </div>

<ProjectLeadImage project="villa-7" />
</section>

      <nav className="vl-area-index" aria-label="Villa 7 area index">
        <p className="sx-kicker">THE HOUSE / 01—10</p>
        <div>{villaAreas.map(area => <a key={area.id} href={"#villa-area-" + area.id}><span>{String(area.id).padStart(2, "0")}</span>{area.name}</a>)}</div>
      </nav>

      <div className="vl-context-banner">
        <span>OUTDOOR / 01—06</span>
        <p>The garden moves from arrival and pool edges to dining, covered terrace and lounge conditions.</p>
      </div>

      {villaAreas.slice(0, 6).map(area => <AreaChapter key={area.id} area={area} />)}

      <div className="vl-context-banner vl-context-banner--indoor">
        <span>INDOOR / 07—10</span>
        <p>The same proposal language continues inside through entrance, living, lounge and dining moments.</p>
      </div>

      {villaAreas.slice(6).map(area => <AreaChapter key={area.id} area={area} />)}

      <section className="vl-sheet-note" aria-labelledby="vl-sheet-title">
        <div>
          <p className="sx-kicker">SOURCE SELECTION SHEET / PP27—29</p>
          <h2 id="vl-sheet-title">The proposal asks for choices.<br/>The website shows the spaces first.</h2>
        </div>
        <p>The original proposal includes a client selection sheet. That logic remains part of the source record, but it is intentionally secondary here: the twenty-four images are the project story, not controls in a configurator.</p>
      </section>

      <section className="vl-care" aria-labelledby="vl-care-title">
        <div>
          <p className="sx-kicker">INCLUDED WITH EVERY OPTION / P32</p>
          <h2 id="vl-care-title">Planted, watered,<br/>always in shape.</h2>
          <p>The proposal connects every visual direction to supply, irrigation and ongoing care.</p>
        </div>
        <ol>
          <li><span>01</span>Supply &amp; installation</li>
          <li><span>02</span>Smart irrigation</li>
          <li><span>03</span>Scheduled maintenance</li>
          <li><span>04</span>Plant warranty</li>
        </ol>
      </section>

      <section className="vl-source-note">
        <span>VISUAL SOURCE NOTE</span>
        <p>All twenty-four images are proposal visuals. They remain labelled as options and are not presented as completed Villa 7 installations or confirmed client selections.</p>
      </section>

      <section className="vl-next">
        <p className="sx-kicker">NEXT STEP / P33</p>
        <h2>Shape a garden around the space.</h2>
        <a className="sx-underlink" href="/start-a-space?space=home">Discuss a residential space &#8599;</a>
      </section>
    <CaseTrail current="villa-7" />
</main>

    <footer className="sx-footer">
      <a href="/spaces/projects">Project archive</a>
      <span>Plantupia / Villa 7 proposal study</span>
      <a href="/spaces">Residential context &#8599;</a>
    </footer>
  </div>;
}
