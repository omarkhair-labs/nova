import SiteIndex from "./SiteIndex";
import { pixelsPrinciples, pixelsSequence } from "./pixels-case-data";
import "./space-explorer.css";
import "./pixels-case.css";

const imageUrl=(name:string)=>"/api/private-arrival/"+name;

export default function PixelsCaseStudy(){
 return <div className="sx-page px-page px-world">
  <a className="sx-skip" href="#px-sequence">Skip to Pixels Mall sequence</a>
  <header className="sx-header px-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=retail">Start a space <span aria-hidden="true">↗</span></a></header>
  <main className="px-main">
   <section className="px-intro" aria-labelledby="px-title">
    <div className="px-intro-id"><a href="/spaces/projects">← Project archive</a><span>CASE STUDY 04 / PIXELS MALL</span></div>
    <div className="px-intro-title"><p className="sx-kicker">COMMERCIAL / NEW CAIRO / DESIGN PROPOSAL</p><h1 id="px-title">A landscape<br/><em>through the building.</em></h1></div>
    <div className="px-intro-note"><p>Eight proposal views form one spatial route. The website keeps every composition intact instead of forcing the source into one fixed image frame.</p><dl><div><dt>Views</dt><dd>8 proposal scenes</dd></div><div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div></dl></div>
   </section>

   <nav className="px-index" aria-label="Pixels Mall proposal scene index">
    <span>SPATIAL ROUTE / 01—08</span>
    <div>{pixelsSequence.map(scene=><a key={scene.id} href={"#px-scene-"+scene.id}><span>{scene.id}</span><strong>{scene.title}</strong><small>P{scene.page}</small></a>)}</div>
   </nav>

   <section id="px-sequence" className="px-sequence" aria-label="Pixels Mall proposal sequence">
    {pixelsSequence.map((scene,index)=><article id={"px-scene-"+scene.id} className="px-scene" data-tone={scene.tone} key={scene.id}>
      <figure>
       <img src={imageUrl(scene.image)} alt={"Pixels Mall proposal visualization, "+scene.title+", page "+scene.page} loading={index<2?"eager":"lazy"}/>
       <figcaption><span>SCENE {scene.id}</span><span>PROPOSAL P{scene.page}</span></figcaption>
      </figure>
      <div className="px-scene-reading"><div><span>{scene.id} / 08</span><small>{scene.tone.toUpperCase()}</small></div><h2>{scene.title}</h2><p>{scene.note}</p><small>Descriptive reading of proposal imagery — not a source chapter title.</small></div>
    </article>)}
   </section>

   <section className="px-source-grid" aria-label="Pixels Mall project context"><article className="px-source-summary"><p className="sx-kicker">SOURCE READING</p><h2>One place.<br/>Several conditions.</h2><p>Planting appears at arrival, along circulation, at thresholds, terraces, facades and inside the commercial volume. The sequence is the project argument; no single image is treated as the whole case.</p></article><article className="px-source-note"><span>VISUAL SOURCE NOTE</span><p>All imagery remains inside its proposal context. Nothing here is presented as evidence of completed construction, installation or client approval.</p></article></section>

   <section className="px-principles" aria-labelledby="px-principles-title"><div><p className="sx-kicker">DESIGN PRINCIPLES / SOURCE P02</p><h2 id="px-principles-title">Five intentions.<br/>One continuous landscape.</h2><p>The source proposal names these principles explicitly. They sit after the visual route so the imagery can be read before the framework.</p></div><ol>{pixelsPrinciples.map(([n,t])=><li key={n}><span>{n}</span><strong>{t}</strong></li>)}</ol></section>

   <section className="px-next"><p className="sx-kicker">COMMERCIAL / NEXT</p><h2>From architecture<br/>to living system.</h2><div className="px-next-links"><a className="sx-underlink" href="/spaces/commercial">Commercial context ↗</a><a className="sx-underlink" href="/living-systems">Living Systems ↗</a><a className="sx-underlink" href="/start-a-space?space=retail">Discuss a commercial project ↗</a></div></section>
  </main>
  <footer className="sx-footer"><a href="/spaces/projects">Project archive</a><span>Plantupia / Pixels Mall</span><a href="/spaces/commercial">Commercial ↗</a></footer>
 </div>;
}
