"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { projects, archiveFilters } from "./project-archive-data";
import "./space-explorer.css";
import "./project-archive.css";

export default function ProjectArchive(){
 const [sector,setSector] = useState<(typeof archiveFilters)[number]["id"]>("all");
 const visible = sector === "all" ? projects : projects.filter(project=>project.sector===sector);
 return <div className="sx-page pa-page">
  <a href="#pa-main" className="sx-skip">Skip to project archive</a>
  <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
  <main className="pa-main" id="pa-main">
   <a href="/spaces" className="sx-underlink pa-back">&#8592; Spaces / sector index</a>

   <section className="pa-intro" aria-labelledby="pa-title">
    <div><p className="sx-kicker">FIELD ARCHIVE / 06 SOURCE-LED STUDIES</p><h1 id="pa-title">Ideas take<br/><em>root here.</em></h1></div>
    <div><p>Six proposal studies, shown as six distinct bodies of work. The archive keeps their scale, status and source context visible instead of reducing them to one active preview.</p><span className="pa-edition">DESIGN PROPOSALS / INSTALLATION STATUS NOT ASSUMED</span></div>
   </section>

   <nav className="pa-contexts" aria-label="Filter project studies">
    {archiveFilters.map(filter => <button key={filter.id} type="button" aria-pressed={sector===filter.id} onClick={()=>setSector(filter.id)}>
      <span>{String(filter.id==="all"?projects.length:projects.filter(project=>project.sector===filter.id).length).padStart(2,"0")}</span>
      <strong>{filter.label}</strong>
    </button>)}
   </nav>
   <p className="pa-filter-line" role="status">SHOWING {String(visible.length).padStart(2,"0")} / {String(projects.length).padStart(2,"0")} DESIGN STUDIES<span aria-hidden="true">—</span> {sector==="all"?"ALL ENVIRONMENTS":sector.toUpperCase()}</p>

   <section className="pa-wall" aria-label="Selected proposal studies">
    {visible.map((project,index)=><article className={"pa-project pa-project-"+(index+1)} key={project.id}>
      <a href={"/spaces/projects/"+project.id} className="pa-project-link" aria-label={"Open "+project.name+" case study"}>
        <span className="pa-project-folio" aria-hidden="true">{String(index+1).padStart(2,"0")} / {String(visible.length).padStart(2,"0")} <i>PLANTUPIA FIELD WORK</i></span>
        <figure>
          <img src={"/api/private-arrival/"+project.image} alt={project.alt} loading={index<2?"eager":"lazy"}/>
          <figcaption><span>{String(index+1).padStart(2,"0")} / {project.name}</span><span>PROPOSAL P{project.page}</span></figcaption>
        </figure>
        <div className="pa-project-copy">
          <div><p className="sx-kicker">{project.sector.toUpperCase()} / {project.type.toUpperCase()}</p><h2>{project.title}</h2></div>
          <div><p>{project.body}</p><dl><div><dt>Study</dt><dd>{project.count}</dd></div><div><dt>Scope</dt><dd>{project.scope.join(" / ")}</dd></div><div><dt>Status</dt><dd>Design proposal</dd></div></dl><span className="pa-open">ENTER THIS STUDY <span aria-hidden="true">&#8599;</span></span></div>
        </div>
      </a>
    </article>)}
   </section>

   <section className="pa-related" aria-labelledby="pa-related-title">
    <div><p className="sx-kicker">RELATED PRACTICE / OFFICES &amp; HQs</p><h2 id="pa-related-title">A service context without a borrowed project.</h2></div>
    <div><p>The reviewed catalogue supports workplace planting, custom vessels and maintenance, but no named office commission is included in this archive. The sector page therefore uses catalogue evidence rather than presenting a fictional case study.</p><a href="/spaces/offices" className="sx-underlink">Explore offices &amp; HQs &#8599;</a></div>
   </section>
  </main>
  <footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Living spaces</span><a href="/studio">The Studio &#8599;</a></footer>
 </div>;
}
