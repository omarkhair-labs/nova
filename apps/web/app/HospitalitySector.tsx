"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import "./space-explorer.css";
import "./hospitality-sector.css";

const moments = [
  { label: "Arrival", heading: "The welcome starts outside.", body: "A planted facade can frame the entrance and express the character of a restaurant before guests step inside.", name: "Mazal", source: "Facade Planting Proposal / option 03 / page 5", detail: "Luxury Tropical layers Strelitzia, Areca palm and Monstera. Nine directions explore tropical, statement and evergreen identities.", image: "sector-hospitality-arrival.webp", alt: "Mazal restaurant facade planting visualization, Luxury Tropical option", w: 1920, h: 770 },
  { label: "Seating", heading: "Make room for the view.", body: "Planting can shape a boundary between tables without closing the space. The vessel, plant height and view belong to the same design decision.", name: "EM Sherif Sea Cafe", source: "Planting Proposal / option E / page 7", detail: "A low Schefflera hedge is proposed to keep the sea view open. The navy planter specification includes a fiberglass liner and self-drainage.", image: "sector-hospitality-seating.webp", alt: "EM Sherif navy planter proposal with a low Schefflera hedge", w: 1402, h: 1122 },
  { label: "Terrace", heading: "An atmosphere that can be cared for.", body: "Outdoor planting needs to respond to light, water and the upkeep of a busy hospitality space.", name: "El Hallab", source: "Planting Concepts for the Terrace / option 03 / page 5", detail: "Silver Sculpted proposes clipped Texas sage for full sun, low water needs and regular shaping. Five options compare seasonal appearance and maintenance.", image: "sector-hospitality-terrace.webp", alt: "El Hallab terrace proposal with a clipped silver-grey hedge", w: 1420, h: 556 },
] as const;
const steps = [
  ["Choose a direction", "Discuss the experience, character and plant palette."],
  ["Understand the site", "Confirm planter dimensions, quantities and sun exposure on site."],
  ["Specify the proposal", "Prepare a detailed quotation for plants, installation and care."],
  ["Install & care", "Agree delivery, planting and ongoing maintenance terms."],
] as const;

export default function HospitalitySector() {
  const [active,setActive] = useState(0);
  const m = moments[active];
  return <div className="sx-page hs-page">
    <a className="sx-skip" href="#hs-main">Skip to hospitality</a>
    <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">↗</span></a></header>
    <main id="hs-main" className="hs-main">
      <a className="sx-underlink hs-back" href="/spaces">← Spaces / sector index</a>
      <section aria-labelledby="hs-title">
        <div className="hs-title"><div><p className="sx-kicker">01 / HOSPITALITY</p><h1 id="hs-title">From arrival<br/>to <em>atmosphere.</em></h1></div><p>Biophilic design for hotels, restaurants and cafés. Consider the whole guest journey, from the first welcome to the places people choose to spend time.</p></div>
        <div className="hs-moments" role="group" aria-label="Explore guest journey moments">
          {moments.map((x,i)=><button type="button" key={x.label} aria-pressed={i===active} onClick={()=>setActive(i)}><span>{String(i+1).padStart(2,"0")}</span>{x.label}</button>)}
        </div>
        <div className="hs-study" data-moment={m.label.toLowerCase()} aria-live="polite" key={m.label}>
          <figure><img src={"/api/private-arrival/"+m.image} alt={m.alt} width={m.w} height={m.h}/><figcaption>{m.name} / {m.source}. Design visualization; installation not verified.</figcaption></figure>
          <div className="hs-study-copy"><div><p className="sx-kicker">{m.label.toUpperCase()} / {m.name}</p><h2>{m.heading}</h2></div><div><p>{m.body}</p><p className="hs-evidence">{m.detail}</p>{active===1 && <dl className="hs-spec"><div><dt>Vessel</dt><dd>Navy metal / fiberglass liner</dd></div><div><dt>Dimensions</dt><dd>140 × 50 × 50 or 150 × 50 × 50 cm</dd></div><div><dt>Drainage</dt><dd>Self-drainage system</dd></div></dl>}{active===2 && <dl className="hs-spec"><div><dt>Light</dt><dd>Full sun</dd></div><div><dt>Water</dt><dd>Low</dd></div><div><dt>Maintenance</dt><dd>Regular shaping</dd></div></dl>}</div></div>
        </div>
      </section>
      <section className="hs-process" aria-labelledby="hs-process-title"><div><p className="sx-kicker">FROM CONCEPT TO CARE</p><h2 id="hs-process-title">A considered process.</h2><p>Based on the proposal workflow. The exact service scope is defined for each site.</p></div><ol>{steps.map(([title,body])=><li key={title}><h3>{title}</h3><p>{body}</p></li>)}</ol></section>
      <section className="hs-contact"><p className="sx-kicker">YOUR SPACE</p><h2>What should your guests feel?</h2><a className="sx-underlink" href="/start-a-space?space=gather">Discuss your hospitality project ↗</a></section>
    </main><footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Design preview</span><a href="/studio">The Studio ↗</a></footer>
  </div>;
}
