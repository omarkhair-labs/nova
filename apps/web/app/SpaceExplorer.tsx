"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { spaces, editorialSpaces, spacePrompts } from "./space-contexts";
import { projects } from "./project-archive-data";
import "./space-explorer.css";

const keys = ["home", "work", "gather", "industrial", "retail", "public"] as const;
const shortNames = ["Residential", "Workplaces", "Hospitality", "Industry", "Retail", "Public spaces"] as const;

export default function SpaceExplorer({ privateReview = false }: { privateReview?: boolean }) {
  const [active, setActive] = useState(privateReview ? 2 : 0);
  const order = privateReview ? [2, 4, 1, 0, 3, 5] : [0, 1, 2, 3, 4, 5];
  const position = order.indexOf(active);
  const [failed, setFailed] = useState<number[]>([]);
  const [priorities, setPriorities] = useState<Record<number, number[]>>({});
  const privateStudies = [
    {src:"/api/private-arrival/spaces-residential.webp",subject:"Villa 7 / covered terrace proposal",credit:"Plantupia proposal / p14"},
    {src:"/api/private-arrival/spaces-workplace-palette.webp",subject:"Plantupia workplace planting palette",credit:"Plantupia catalogue / p28"},
    {src:"/api/private-arrival/spaces-hospitality.webp",subject:"El Hallab / restaurant planting study",credit:"Plantupia proposal / p3"},
    {src:"/api/private-arrival/sector-office-vessels.webp",subject:"Plantupia vessel collection study",credit:"Plantupia catalogue / p37"},
    {src:"/api/private-arrival/spaces-commercial.webp",subject:"Pixels Mall / commercial promenade proposal",credit:"MT Architects / Plantupia proposal / p16"},
    {src:"/api/private-arrival/spaces-shared-terrace.webp",subject:"Pixels Mall / shared terrace proposal study",credit:"Plantupia proposal / p21"},
  ] as const;
  const photos = editorialSpaces.map((item,i)=>privateReview?{...item,...privateStudies[i],source:"private-plantupia-study"}:item);
  const photo = photos[active];
  const space = spaces[active];
  const selected = priorities[active] ?? [];
  const briefHref = `/start-a-space?space=${keys[active]}&focus=${selected.slice().sort().join(",")}`;
  function togglePriority(index: number) {
    setPriorities(previous => {
      const values = previous[active] ?? [];
      return { ...previous, [active]: values.includes(index) ? values.filter(value => value !== index) : [...values, index] };
    });
  }
  function select(index: number) {
    setActive(index);
  }
  function move(delta: number) {
    setActive(order[(position + delta + order.length) % order.length]);
  }
  return (
    <div className="sx-page">
      <a className="sx-skip" href="#sx-main">Skip to spaces</a>
      <header className="sx-header">
        <a href="/" className="sx-brand" aria-label="Plantupia home">◕ Plantupia</a>
        <SiteIndex current="spaces" />
        <a href="/start-a-space" className="sx-contact">Get in touch <span aria-hidden="true">↗</span></a>
      </header>
      <main id="sx-main">
        <section className="sx-exhibit" aria-label="Explore living environments">
          <div className="sx-intro"><div><p className="sx-kicker">02 / SPACES</p><h1>Every space <em>has its own life.</em></h1></div><p>{privateReview ? "Hospitality, commercial destinations and workplaces are the current business focus. Other environments remain part of the practice." : "From a quiet home to the scale of a factory. Nature can belong wherever people build."}</p></div>
          <div className="sx-environment-index" data-commercial-focus={privateReview}>
            {privateReview && <p className="sx-index-label">PRIMARY SECTORS</p>}
            <div className="sx-picker" role="group" aria-label={privateReview ? "Primary commercial sectors" : "Choose an environment"}>
              {(privateReview ? order.slice(0,3) : order).map((i) => (
                <button key={keys[i]} type="button" data-space={keys[i]} aria-pressed={active===i} onClick={()=>select(i)}><span>{String(order.indexOf(i)+1).padStart(2,"0")}</span>{privateReview && i===4 ? "Commercial" : privateReview && i===1 ? "Offices & HQs" : shortNames[i]}</button>
              ))}
            </div>
            {privateReview && <><p className="sx-index-label sx-index-secondary">OTHER ENVIRONMENTS</p><div className="sx-picker sx-picker-secondary" role="group" aria-label="Other environments">{order.slice(3).map(i=><button key={keys[i]} data-space={keys[i]} type="button" aria-pressed={active===i} onClick={()=>select(i)}><span>{String(order.indexOf(i)+1).padStart(2,"0")}</span>{shortNames[i]}</button>)}</div></>}
          </div>
          <figure className="sx-figure">
            <div className="sx-frame" data-residential-study={privateReview && active === 0} data-hospitality-study={privateReview && active === 2} data-industry-study={privateReview && active === 3} data-retail-study={privateReview && active === 4} aria-hidden="true">
              {photos.map((item,i)=><div key={item.source} className={"sx-layer"+(active===i?" sx-current":"")}>
                <img src={failed.includes(i)?item.fallback:item.src} alt="" loading={i===order[0]?"eager":"lazy"} fetchPriority={i===order[0]?"high":"auto"}
                  onError={()=>setFailed(previous=>previous.includes(i)?previous:[...previous,i])}/>
              </div>)}
            </div>
            <figcaption className="sx-caption">
              <span>{failed.includes(active) ? "Generated concept fallback - illustrative reference" : privateReview ? photo.subject + " - proposal/catalogue study, not a verified sector installation" : photo.subject + " - illustrative reference, not client work"}</span>
              {!failed.includes(active) && privateReview && <span>{photo.credit}</span>}
              {!failed.includes(active)&&!privateReview&&<a href={photo.source} target="_blank" rel="noreferrer">{photo.credit} &#8599;</a>}
            </figcaption>
          </figure>
          <div className="sx-rail">
            <div className="sx-progress"><span>{String(position+1).padStart(2,"0")} / 06</span><div aria-hidden="true"><i style={{width:((position+1)/spaces.length*100)+"%"}}/></div></div>
            <div className="sx-arrows"><button type="button" aria-label="Previous environment" onClick={()=>move(-1)}>←</button><button type="button" aria-label="Next environment" onClick={()=>move(1)}>→</button></div>
          </div>
          <div key={space.id} className="sx-reading" aria-live="polite">
            <div><p className="sx-kicker">{space.type}</p><h2>{space.headline}</h2></div>
            <div><p>{privateReview && active === 4 ? "For developers, malls and showrooms, planting can become part of the architecture and visitor journey. The Pixels Mall proposal combines signature trees, layered landscapes, distinct experiential zones, custom planters and low-maintenance greenery." : space.text}</p><a className="sx-underlink" href={"/start-a-space?space="+keys[active]}>Discuss a space like this ↗</a></div>
          </div>
          {privateReview && active === 2 && <p className="hs-sector-link"><a className="sx-underlink" href="/spaces/hospitality">Explore the hospitality sector ↗</a></p>}
          {privateReview && active === 1 && <p className="of-sector-link"><a className="sx-underlink" href="/spaces/offices">Explore offices &amp; HQs &#8599;</a></p>}
          {privateReview && active === 1 && <section className="sx-work-palette" aria-labelledby="sx-work-palette-title">
            <div><p className="sx-kicker">MATERIAL STUDY / WORKPLACES</p><h2 id="sx-work-palette-title">A palette for working spaces.</h2><p>The catalogue groups individual plants, grouped vessels and linear planting. The right arrangement depends on available light, circulation and maintenance access.</p></div>
            <figure><img src="/api/private-arrival/spaces-workplace-palette.webp" alt="Catalogue arrangement of individual green plants and rectangular planted vessels in light neutral planters" loading="lazy"/><figcaption>Plantupia Hospitality Greenery Catalogue / page 28 / collection illustration, not an installed office project.</figcaption></figure>
          </section>}
          {privateReview && active === 4 && <p className="cm-sector-link"><a className="sx-underlink" href="/spaces/commercial">Explore the commercial sector &#8599;</a></p>}
          {privateReview && active === 4 && <section className="sx-work-palette" aria-labelledby="sx-commercial-study-title">
            <div><p className="sx-kicker">SHOWROOM STUDY / AUDI CAIRO</p><h2 id="sx-commercial-study-title">An entrance with a living identity.</h2><p>The Living Interiors proposal explores natural planting at the entrance, lounge and display areas. Its care programme proposes selection and installation, smart irrigation, scheduled maintenance and plant warranty; specific terms remain subject to agreement.</p></div>
            <figure><img src="/api/private-arrival/spaces-commercial-showroom.webp" alt="Audi showroom entrance design proposal with planted beds beside the glazed entrance and brand wall" loading="lazy"/><figcaption>Audi Cairo Center / The Entrance / proposal, page 7. Not a verified installation.</figcaption></figure>
          </section>}
          {privateReview && active === 5 && <section className="sx-work-palette" aria-labelledby="sx-shared-study-title">
            <div><p className="sx-kicker">SHARED SPACE STUDY / PIXELS MALL</p><h2 id="sx-shared-study-title">A planted edge, a shared place.</h2><p>The commercial proposal considers plazas, terraces and arrival points as distinct experiential zones. This terrace study explores planting alongside pedestrian circulation. Climate, inclusive access and site safety need to be assessed for each shared space.</p></div>
            <figure><img src="/api/private-arrival/spaces-shared-terrace.webp" alt="Design visualization of a planted curved terrace edge beside a paved circulation area" loading="lazy"/><figcaption>Pixels Mall / MT Architects / Plantupia proposal, page 21. Commercial shared-space study, not a civic commission or verified installation.</figcaption></figure>
          </section>}
          {privateReview&&<section className="sx-case-index" aria-labelledby="sx-case-index-title"><div className="sx-case-index-head"><div><p className="sx-kicker">SELECTED CASE STUDIES / 06</p><h2 id="sx-case-index-title">See the work.<br/>Then read it.</h2></div><div><p>Open the full proposal studies directly. Each case contains the visual alternatives, not just one cover image.</p><a className="sx-underlink" href="/spaces/projects">Open the full project archive &#8599;</a></div></div><div className="sx-case-grid">{projects.map((project,i)=><a key={project.id} href={"/spaces/projects/"+project.id} className={"sx-case-card sx-case-card-"+(i+1)}><figure><img src={"/api/private-arrival/"+project.image} alt={project.alt} loading={i<2?"eager":"lazy"}/><figcaption><span>{String(i+1).padStart(2,"0")} / {project.name}</span><strong>{project.count}</strong></figcaption></figure><div><p>{project.type}</p><h3>{project.title}</h3><span>OPEN CASE STUDY &#8599;</span></div></a>)}</div></section>}
          <section className="sx-brief" aria-labelledby="sx-brief-title">
            <div className="sx-brief-intro">
              <p className="sx-kicker">FIELD NOTES / {String(position+1).padStart(2,"0")}</p>
              <h2 id="sx-brief-title">What matters<br/>in your space?</h2>
              <p>Design prompts for this space. Mark the considerations you would like to discuss with us. These are starting questions, not an assessment of your site.</p>
            </div>
            <div className="sx-brief-sheet">
              <div className="sx-brief-mast"><span>{space.name}</span><span>01 — 03</span></div>
              <div className="sx-brief-notes" role="group" aria-label="Choose considerations for your project brief">
                {spacePrompts[active].map((prompt, i) => <button className="sx-note" key={space.id + prompt.label} type="button" aria-pressed={selected.includes(i)} onClick={() => togglePriority(i)}><span className="sx-note-number" aria-hidden="true">{String(i+1).padStart(2,"0")}</span><span className="sx-note-copy"><strong>{prompt.label}</strong><small>{prompt.detail}</small></span><span className="sx-note-mark" aria-hidden="true">{selected.includes(i) ? "✓" : "+"}</span></button>)}
              </div>
              <div className="sx-brief-foot"><p aria-live="polite">{selected.length ? selected.length + " of 3 considerations marked" : "Mark a consideration to begin."}</p>{selected.length > 0 && <a href={briefHref} className="sx-underlink">Carry these notes into a conversation ↗</a>}</div>
            </div>
          </section>
        </section>
      </main>
      <footer className="sx-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/start-a-space">Contact ↗</a></footer>
    </div>
  );
}
