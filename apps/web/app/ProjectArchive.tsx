"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { projects, archiveFilters } from "./project-archive-data";
import "./space-explorer.css";
import "./project-archive.css";
type Filter = typeof archiveFilters[number]["id"];
export default function ProjectArchive(){
 const [filter,setFilter]=useState<Filter>("all");
 const [selected,setSelected]=useState<string>(projects[0].id);
 const visible=projects.filter(p=>filter==="all"||p.sector===filter);
 const project=visible.find(p=>p.id===selected)??visible[0];
 const index=projects.findIndex(p=>p.id===project.id);
 function changeFilter(next:Filter){
  setFilter(next);
  if(next!=="all"&&!projects.some(p=>p.id===selected&&p.sector===next))setSelected(projects.find(p=>p.sector===next)!.id);
 }
 return <div className="sx-page pa-page">
  <a href="#pa-main" className="sx-skip">Skip to project archive</a>
  <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
  <main className="pa-main" id="pa-main">
   <a href="/spaces" className="sx-underlink pa-back">&#8592; Spaces / sector index</a>
   <section className="pa-intro"><div><p className="sx-kicker">FIELD ARCHIVE / SELECTED PROPOSALS</p><h1>Ideas take<br/><em>root here.</em></h1></div><div><p>Six design studies. Different scales, materials and possibilities. Explore the questions behind the proposals, rather than a catalogue of finished installations.</p><span className="pa-edition">06 STUDIES / 03 CONTEXTS</span></div></section>
   <section aria-label="Project archive">
    <div className="pa-filters" role="group" aria-label="Filter project studies">{archiveFilters.map(f=><button key={f.id} type="button" aria-pressed={filter===f.id} onClick={()=>changeFilter(f.id)}>{f.label}<span>{String(f.id==="all"?projects.length:projects.filter(p=>p.sector===f.id).length).padStart(2,"0")}</span></button>)}</div>
    <div className="pa-exhibit"><div className="pa-index" role="group" aria-label="Choose a project">{visible.map(p=><button type="button" key={p.id} aria-pressed={project.id===p.id} onClick={()=>setSelected(p.id)} data-project={p.id}><span className="pa-num">{String(projects.indexOf(p)+1).padStart(2,"0")}</span><span><strong>{p.name}</strong><small>{p.type}</small></span><span aria-hidden="true">&#8599;</span></button>)}</div>
    <article className="pa-study" key={project.id} aria-live="polite" data-active-project={project.id}><figure><img src={"/api/private-arrival/"+project.image} alt={project.alt} /><figcaption><span>{project.name} / proposal page {project.page}</span><span>DESIGN VISUALIZATION</span></figcaption></figure><div className="pa-study-copy"><div><p className="sx-kicker">STUDY {String(index+1).padStart(2,"0")} / {project.count}</p><h2>{project.title}</h2></div><div><p>{project.body}</p><ul>{project.scope.map(s=><li key={s}>{s}</li>)}</ul><p className="pa-status">Proposal record. Installation and publication status not verified.</p>{<><a className="sx-underlink" href={"/spaces/projects/"+project.id}>Open the case study &#8599;</a>{project.sector==="residential"?<a className="sx-underlink" href="/start-a-space?space=home">Discuss residential space &#8599;</a>:<a className="sx-underlink" href={project.sector==="commercial"?"/spaces/commercial":"/spaces/hospitality"}>Explore {project.sector} &#8599;</a>}</>}</div></div></article></div>
    <div className="pa-related"><p className="sx-kicker">RELATED CONTEXT / OFFICES &amp; HQs</p><p>Explore material and planting possibilities for workspaces. The reviewed catalogue supports this service; no named office commission is included in this archive.</p><a href="/spaces/offices" className="sx-underlink">Explore workspaces &#8599;</a></div>
   </section>
  </main><footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Design preview</span><a href="/studio">The Studio &#8599;</a></footer>
 </div>;
}
