import SiteIndex from "./SiteIndex";
import "./space-explorer.css";
import "./commercial-sector.css";

const principles = [
  ["Signature trees", "Signature Trees as Architectural Landmarks", "Consider where a distinctive tree can anchor an arrival point or help visitors read the architecture."],
  ["Layered landscape", "Layered Landscape Composition", "Compose planting at different scales so greenery relates to the building rather than competing with it."],
  ["Experiential zones", "Distinct Experiential Zones", "Treat plazas, terraces and arrival points as distinct places with their own atmosphere and purpose."],
  ["Custom vessels", "Premium Custom Planters", "Develop the planter form and finish alongside the architectural material palette and site requirements."],
  ["Sustainable greenery", "Sustainable & Low-Maintenance Greenery", "Consider site conditions and ongoing care when selecting planting. Specific water and maintenance performance must be established for the site."],
] as const;
const care = ["Selection & installation","Smart irrigation","Scheduled maintenance","Plant warranty"] as const;

export default function CommercialSector(){
  return <div className="sx-page cm-page">
    <a className="sx-skip" href="#cm-main">Skip to commercial</a>
    <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=retail">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
    <main id="cm-main" className="cm-main">
      <a href="/spaces" className="sx-underlink cm-back">&#8592; Spaces / sector index</a>
      <section aria-labelledby="cm-title">
        <div className="cm-heading"><div><p className="sx-kicker">02 / COMMERCIAL</p><h1 id="cm-title">Beyond retail.<br/><em>A destination.</em></h1></div><p>Biophilic design for developers, malls and showrooms. Bring architecture, landscape and brand identity into the same design conversation.</p></div>
        <div className="cm-hero-layout"><figure className="cm-hero"><img src="/api/private-arrival/world-spaces.webp" width={1448} height={1086} alt="Pixels Mall proposal showing planting integrated across multiple architectural levels" fetchPriority="high"/><figcaption>Pixels Mall / MT Architects / Plantupia / proposal p28. Design visualization; installation not verified.</figcaption></figure><aside className="cm-hero-note" aria-label="Proposal context"><p className="sx-kicker">PROJECT STUDY / 01</p><h2>Pixels Mall.</h2><p>Architecture, atmosphere and greenery designed together, from the arrival point to the terrace.</p><dl><div><dt>Context</dt><dd>New Cairo</dd></div><div><dt>Proposal</dt><dd>MT Architects</dd></div><div><dt>Focus</dt><dd>Biophilic commercial design</dd></div></dl><a className="sx-underlink" href="#cm-framework-title">Explore the five principles &#8595;</a></aside></div>
      </section>
      <section className="cm-framework" aria-labelledby="cm-framework-title">
        <div className="cm-framework-intro"><p className="sx-kicker">PIXELS MALL / THE DESIGN FRAMEWORK</p><h2 id="cm-framework-title">Five architectural intentions.</h2><p>The proposal outlines five design principles. Open each to explore what it could mean for a commercial site.</p><figure><img src="/api/private-arrival/spaces-shared-terrace.webp" alt="Pixels Mall design study of a planted terrace edge" width={1448} height={1086} loading="lazy"/><figcaption>Pixels Mall proposal / p21. Illustrative design study.</figcaption></figure></div>
        <div className="cm-principles">{principles.map(([label,source,body],i)=><details key={label} name="commercial-principles" open={i===0}><summary><span>{String(i+1).padStart(2,"0")}</span><strong>{label}</strong></summary><div><p className="cm-source-term">{source}</p><p>{body}</p></div></details>)}</div>
      </section>
      <section className="cm-brand" aria-labelledby="cm-brand-title"><div className="cm-heading"><div><p className="sx-kicker">AUDI CAIRO / LIVING INTERIORS</p><h2 id="cm-brand-title">Greenery in the language of a brand.</h2></div><p>The proposal explores six interior chapters, from the lounge and entrance to the display spaces and framing of the brand. These images illustrate the proposed design, not a completed showroom.</p></div>
        <div className="cm-brand-pair"><figure><img src="/api/private-arrival/spaces-commercial-showroom.webp" alt="Audi Cairo entrance planting proposal" width={1920} height={1199} loading="lazy"/><figcaption>02 / The Entrance / p7</figcaption></figure><figure><img src="/api/private-arrival/sector-commercial-brand.webp" alt="Audi Cairo design visualization for Framing the Brand" width={1920} height={1050} loading="lazy"/><figcaption>06 / Framing the Brand / p14</figcaption></figure></div>
      </section>
      <section className="cm-care" aria-labelledby="cm-care-title"><div><p className="sx-kicker">TECHNOLOGY & CARE</p><h2 id="cm-care-title">Designed for what follows.</h2><p>The Audi proposal sets out a care programme. Exact specifications, service intervals and warranty terms are agreed for each project.</p></div><ol>{care.map((item,i)=><li key={item}><span>0{i+1}</span>{item}</li>)}</ol></section>
      <section className="cm-contact"><p className="sx-kicker">YOUR COMMERCIAL DESTINATION</p><h2>Begin with the architecture.</h2><a className="sx-underlink" href="/start-a-space?space=retail">Discuss your commercial project &#8599;</a></section>
    </main><footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Design preview</span><a href="/studio">The Studio &#8599;</a></footer>
  </div>;
}
