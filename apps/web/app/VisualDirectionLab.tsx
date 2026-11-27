"use client";
import "./visual-direction-lab.css";

const pixels=[
 ["pixels-scene-01.webp","Arrival / commercial landscape"],
 ["pixels-scene-02.webp","Promenade / layered planting"],
 ["pixels-scene-03.webp","Terrace / social edge"],
 ["pixels-scene-04.webp","Architecture / planting threshold"],
 ["pixels-scene-05.webp","Upper level / planted destination"],
] as const;
const mazal=[
 ["mazal-option-01.webp","Tropical Blend"],
 ["mazal-option-03.webp","Luxury Tropical"],
 ["mazal-option-07.webp","Sansevieria"],
 ["mazal-option-09.webp","Slim Evergreen Cones"],
] as const;
const objects=[
 ["spaces-workplace-palette.webp","Planting palette / catalogue"],
 ["sector-office-vessels.webp","Vessel family / catalogue"],
 ["em-option-04.webp","Custom navy vessel / planting option"],
 ["shop-bridge.webp","Plant / vessel collection study"],
] as const;

export default function VisualDirectionLab(){
 return <main className="vdl">
  <header className="vdl-top"><a href="/">Plantupia</a><span>PRIVATE VISUAL DIRECTION LAB / 19 NOV 2026</span><a href="/spaces/projects">Project archive &#8599;</a></header>
  <section className="vdl-hero"><div><p>RESEARCH TRANSLATION / 01</p><h1>Images lead.<br/><em>Systems explain.</em></h1></div><div><p>This lab tests three principles against Plantupia's own proposal material. It is not a new public page or a final design system.</p><nav><a href="#field">01 / PROJECT FIELD</a><a href="#compare">02 / COMPARISON</a><a href="#system">03 / SYSTEM + CARE</a></nav></div></section>

  <section id="field" className="vdl-section vdl-field"><header><div><p>DIRECTION A / PROJECT FIELD</p><h2>Show the place<br/>before explaining it.</h2></div><p>Pixels Mall has enough visual breadth to behave like a spatial sequence rather than a single cover image plus text.</p></header><div className="vdl-field-grid">{pixels.map(([src,label],i)=><figure key={src} className={"vdl-field-"+(i+1)}><img src={"/api/private-arrival/"+src} alt={label}/><figcaption><span>{String(i+1).padStart(2,"0")}</span>{label}</figcaption></figure>)}</div><footer><span>REFERENCE PRINCIPLE</span><p>Repeated project imagery / concise captions / technical explanation later.</p><a href="/spaces/projects/pixels-mall">Open current Pixels case &#8599;</a></footer></section>

  <section id="compare" className="vdl-section vdl-compare"><header><div><p>DIRECTION B / SAME PLACE, DIFFERENT PRESENCE</p><h2>Comparison is<br/>the content.</h2></div><p>Mazal does not need a generic “case study” layout. One facade, many planting identities: the source itself suggests a wall of alternatives.</p></header><div className="vdl-compare-strip">{mazal.map(([src,label],i)=><figure key={src}><img src={"/api/private-arrival/"+src} alt={label}/><figcaption><span>0{i+1}</span><strong>{label}</strong></figcaption></figure>)}</div><div className="vdl-compare-note"><p>One architectural frame.</p><p>Change only the planting language.</p><p>Let the difference be visible before it is described.</p></div><a className="vdl-link" href="/spaces/projects/mazal">Open the nine-option study &#8599;</a></section>

  <section id="system" className="vdl-section vdl-system"><header><div><p>DIRECTION C / MATERIAL + SYSTEM + CARE</p><h2>Objects need<br/>their own language.</h2></div><p>The Hospitality catalogue can become a material and vessel library. It should sit between project imagery and care—not as a conventional ecommerce grid.</p></header><div className="vdl-object-grid">{objects.map(([src,label],i)=><figure key={src}><img src={"/api/private-arrival/"+src} alt={label}/><figcaption><span>{String(i+1).padStart(2,"0")}</span><strong>{label}</strong></figcaption></figure>)}</div><div className="vdl-care"><div><p>DESIGN OBJECT</p><h3>Plant + vessel + condition.</h3></div><ol><li><span>01</span>Selection &amp; installation</li><li><span>02</span>Smart irrigation</li><li><span>03</span>Scheduled maintenance</li><li><span>04</span>Plant warranty</li></ol></div></section>

  <section className="vdl-decision"><p>WORKING DECISION</p><h2>One website.<br/>Different visual behaviours.</h2><div><article><span>PROJECTS</span><p>Large image sequences, quiet captions, project-specific interactions.</p></article><article><span>OPTIONS</span><p>Contact walls and same-frame comparison when the source is comparative.</p></article><article><span>MATERIALS</span><p>Object-led library with specifications and conditions close to the image.</p></article><article><span>CARE</span><p>Operational continuity after the image story, not an abstract dashboard.</p></article></div></section>
 </main>
}
