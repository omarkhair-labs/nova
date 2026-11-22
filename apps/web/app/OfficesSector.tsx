"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { spacePrompts } from "./space-contexts";
import "./space-explorer.css";
import "./offices-sector.css";

const arrangements = [
  {title:"Individual vessels",source:"Catalogue pp21–30",body:"Use distinct plant forms where daylight, floor area and access support them. The plant reads as an object within the room rather than a decorative afterthought."},
  {title:"Grouped planting",source:"Catalogue pp28, 33",body:"Compose plants and vessels as one field. Scale, mature plant size and the room conditions determine whether the group belongs."},
  {title:"Linear boundaries",source:"Catalogue p36",body:"A long planted vessel can define an edge or route while preserving circulation. The catalogue includes metal planter studies at 200 × 40 × 40 cm."},
] as const;

export default function OfficesSector(){
  const [chosen,setChosen]=useState<number[]>([]);
  const href="/start-a-space?space=work"+(chosen.length?"&focus="+[...chosen].sort().join(","):"");
  function toggle(i:number){setChosen(old=>old.includes(i)?old.filter(x=>x!==i):[...old,i]);}
  return <div className="sx-page of-page">
    <a className="sx-skip" href="#of-main">Skip to workplaces</a>
    <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=work">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
    <main id="of-main" className="of-main">
      <a className="sx-underlink of-back" href="/spaces">&#8592; Spaces / sector index</a>

      <section className="of-intro" aria-labelledby="of-title">
        <div><p className="sx-kicker">03 / OFFICES &amp; HQs</p><h1 id="of-title">Room for<br/><em>working life.</em></h1><p>Workplace greenery starts with how the building is used: daylight, circulation, maintenance access and the character the organisation wants to live with.</p><a className="sx-underlink" href="#of-brief-title">Build the workplace brief &#8595;</a></div>
        <figure><img src="/api/private-arrival/spaces-workplace-palette.webp" width={1402} height={1122} alt="Plantupia catalogue illustration of individual and grouped planting vessels" fetchPriority="high"/><figcaption>Plantupia Hospitality Greenery Catalogue / p28. Collection illustration, not an installed office project.</figcaption></figure>
      </section>

      <section className="of-arrangements" aria-labelledby="of-arrangements-title">
        <div className="of-arrangements-head"><div><p className="sx-kicker">THE PLANTING PALETTE / THREE COMPOSITION LOGICS</p><h2 id="of-arrangements-title">A composition,<br/>not a decoration.</h2></div><p>These are three ways to organise the same material vocabulary. They are not packages to click through; the site conditions decide which logic is useful.</p></div>
        <div className="of-arrangement-field">
          <figure><img src="/api/private-arrival/spaces-workplace-palette.webp" alt="Plantupia catalogue study of individual and grouped planting vessels" loading="lazy"/><figcaption>PLANT COLLECTION / CATALOGUE STUDY / P28</figcaption></figure>
          <ol>{arrangements.map((m,i)=><li key={m.title}><span>0{i+1}</span><div><p className="sx-kicker">{m.source}</p><h3>{m.title}</h3><p>{m.body}</p></div></li>)}</ol>
        </div>
      </section>

      <section className="of-material" aria-labelledby="of-material-title">
        <figure><img src="/api/private-arrival/sector-office-vessels.webp" width={1536} height={1024} alt="Fiberglass planter collection illustration from the Plantupia catalogue" loading="lazy"/><figcaption>Plantupia catalogue / p37. Planter collection illustration.</figcaption></figure>
        <div><p className="sx-kicker">MATERIAL &amp; SPECIFICATION</p><h2 id="of-material-title">The vessel belongs to the architecture.</h2><p>The catalogue treats planter form, colour, finish and size as specification choices. Final dimensions and materials still belong to the actual site.</p><dl><div><dt>Material</dt><dd>Fiberglass / metal options</dd></div><div><dt>Finish</dt><dd>Colour and finish specification</dd></div><div><dt>Scale</dt><dd>Custom sizing</dd></div></dl></div>
      </section>

      <section className="of-brief" aria-labelledby="of-brief-title">
        <div><p className="sx-kicker">WORKPLACE FIELD NOTES</p><h2 id="of-brief-title">What does the workplace need?</h2><p>This is the one interaction on the page with a job: mark the site questions worth carrying into the project conversation.</p></div>
        <div className="of-brief-sheet"><div role="group" aria-label="Workplace brief considerations">{spacePrompts[1].map((m,i)=><button className="of-note" type="button" key={m.label} aria-pressed={chosen.includes(i)} onClick={()=>toggle(i)}><span>0{i+1}</span><span><strong>{m.label}</strong><small>{m.detail}</small></span><span aria-hidden="true">{chosen.includes(i)?"✓":"+"}</span></button>)}</div><p aria-live="polite">{chosen.length?chosen.length+" of 3 considerations marked":"Nothing marked yet."}</p><a className="sx-underlink of-brief-link" href={href}>Carry these notes into a conversation &#8599;</a></div>
      </section>

      <section className="of-service"><p className="sx-kicker">FROM DESIGN TO CARE</p><h2>Designed. Supplied. Installed. Maintained.</h2><p>The catalogue describes this service chain across corporate, hospitality and commercial environments. The exact maintenance scope and responsibilities are agreed per site.</p></section>
    </main>
    <footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Living spaces</span><a href="/studio">The Studio &#8599;</a></footer>
  </div>;
}
