"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { spacePrompts } from "./space-contexts";
import "./space-explorer.css";
import "./offices-sector.css";

const arrangements = [
  {title:"Individual vessels",source:"Catalogue pp21–30",body:"Place distinct plant forms where the available light, floor area and access allow. The indoor collection includes Aglaonema Silver Queen, Areca palm, Ficus and Dracaena."},
  {title:"Grouped planting",source:"Catalogue pp28, 33",body:"Explore combinations of plants and vessels as a single composition. Confirm mature plant sizes and the room's conditions before specifying a combination."},
  {title:"Linear boundaries",source:"Catalogue p36",body:"The catalogue shows metal planters at 200 × 40 × 40 cm. Consider where a linear arrangement could support a spatial boundary while preserving circulation and access."},
] as const;
export default function OfficesSector(){
  const [chosen,setChosen]=useState<number[]>([]);
  const [active,setActive]=useState(0);
  const href="/start-a-space?space=work"+(chosen.length?"&focus="+[...chosen].sort().join(","):"");
  function toggle(i:number){setChosen(old=>old.includes(i)?old.filter(x=>x!==i):[...old,i]);}
  return <div className="sx-page of-page">
    <a className="sx-skip" href="#of-main">Skip to workplaces</a>
    <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=work">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
    <main id="of-main" className="of-main">
      <a className="sx-underlink of-back" href="/spaces">&#8592; Spaces / sector index</a>
      <section className="of-intro" aria-labelledby="of-title">
        <div><p className="sx-kicker">03 / OFFICES &amp; HQs</p><h1 id="of-title">Room for<br/><em>working life.</em></h1><p>Biophilic design, premium greenery and professional maintenance for offices and corporate headquarters. Begin with the way the workplace is used, not a fixed plant package.</p><a className="sx-underlink" href="#of-brief-title">Build your workplace brief &#8595;</a></div>
        <figure><img src="/api/private-arrival/spaces-workplace-palette.webp" width={1402} height={1122} alt="Plantupia catalogue illustration of individual and grouped planting vessels" fetchPriority="high"/><figcaption>Plantupia Hospitality Greenery Catalogue / p28. Collection illustration, not an installed office project.</figcaption></figure>
      </section>
      <section className="of-arrangements" aria-labelledby="of-arrangements-title"><div><p className="sx-kicker">THE PLANTING PALETTE</p><h2 id="of-arrangements-title">A composition, not a decoration.</h2><p>Three possible arrangements drawn from the company's catalogue. These are design starting points, not a technical assessment of your building.</p></div>
        <div className="of-options" role="group" aria-label="Explore planting arrangements">{arrangements.map((m,i)=><button type="button" key={m.title} aria-pressed={active===i} onClick={()=>setActive(i)}><span>0{i+1}</span>{m.title}</button>)}</div><div className="of-arrangement-study"><figure className="of-plan of-plan-photo" data-arrangement={active}><img src={active===2?"/api/private-arrival/sector-office-vessels.webp":"/api/private-arrival/spaces-workplace-palette.webp"} alt="Plantupia catalogue planting and planter study"/><figcaption>Plantupia catalogue study / arrangement reference, not an installed office project.</figcaption></figure><div className="of-option-detail" aria-live="polite"><p className="sx-kicker">{arrangements[active].source}</p><h3>{arrangements[active].title}</h3><p>{arrangements[active].body}</p></div></div>
      </section>
      <section className="of-material" aria-labelledby="of-material-title"><figure><img src="/api/private-arrival/sector-office-vessels.webp" width={1536} height={1024} alt="Fiberglass planter collection illustration from the Plantupia catalogue" loading="lazy"/><figcaption>Plantupia catalogue / p37. Planter collection illustration.</figcaption></figure><div><p className="sx-kicker">MATERIAL &amp; SPECIFICATION</p><h2 id="of-material-title">Belong to the architecture.</h2><p>The catalogue offers planter shape and colour customisation, RAL colours, gloss finishes and custom sizing. Final material and dimensions are specified for each project.</p><dl><div><dt>Material</dt><dd>Fiberglass / metal options</dd></div><div><dt>Finish</dt><dd>Colour and finish specification</dd></div><div><dt>Scale</dt><dd>Custom sizing</dd></div></dl></div></section>
      <section className="of-brief" aria-labelledby="of-brief-title"><div><p className="sx-kicker">WORKPLACE FIELD NOTES</p><h2 id="of-brief-title">What does your workplace need?</h2><p>Mark the questions that matter to you. Carry them into the existing project enquiry; nothing is submitted from this page.</p></div><div className="of-brief-sheet"><div role="group" aria-label="Workplace brief considerations">{spacePrompts[1].map((m,i)=><button className="of-note" type="button" key={m.label} aria-pressed={chosen.includes(i)} onClick={()=>toggle(i)}><span>0{i+1}</span><span><strong>{m.label}</strong><small>{m.detail}</small></span><span aria-hidden="true">{chosen.includes(i)?"✓":"+"}</span></button>)}</div><p aria-live="polite">{chosen.length} of 3 considerations marked</p><a className="sx-underlink of-brief-link" href={href}>Discuss your workplace project &#8599;</a></div></section>
      <section className="of-service"><p className="sx-kicker">FROM DESIGN TO CARE</p><h2>Designed. Supplied. Installed. Maintained.</h2><p>Plantupia's catalogue describes these four services for corporate, hospitality and commercial environments. Agree the exact maintenance scope and responsibilities for your site.</p></section>
    </main><footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Design preview</span><a href="/studio">The Studio &#8599;</a></footer>
  </div>;
}
