"use client";

import { useMemo, useState } from "react";
import SiteIndex from "./SiteIndex";
import { archiveFilters, projects } from "./project-archive-data";
import "./space-explorer.css";
import "./project-archive.css";

type Filter=typeof archiveFilters[number]["id"];
type Mode="projects"|"relations";

const relations=[
  {id:"arrival",label:"ARRIVAL",note:"How living material first meets the visitor.",items:["mazal","pixels-mall","audi-cairo"]},
  {id:"edge",label:"EDGE",note:"Planting as boundary, frontage and planted line.",items:["mazal","el-hallab","pixels-mall"]},
  {id:"vessel",label:"VESSEL",note:"The container becomes part of the spatial composition.",items:["em-sherif","pixels-mall","villa-7"]},
  {id:"threshold",label:"THRESHOLD",note:"Transitions between architecture, movement and planting.",items:["audi-cairo","mazal","villa-7"]},
] as const;

export default function ProjectArchive(){
 const [mode,setMode]=useState<Mode>("projects");
 const [filter,setFilter]=useState<Filter>("all");
 const [relation,setRelation]=useState(0);
 const visible=useMemo(()=>projects.filter(p=>filter==="all"||p.sector===filter),[filter]);
 const currentRelation=relations[relation];
 const related=currentRelation.items.map(id=>projects.find(p=>p.id===id)!).filter(Boolean);

 return <div className="sx-page pa-page pa-world">
  <a href="#pa-main" className="sx-skip">Skip to project archive</a>
  <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space">Start a space <span aria-hidden="true">↗</span></a></header>

  <main className="pa-main" id="pa-main">
   <div className="pa-world-topline"><a href="/spaces" className="sx-underlink">← Spaces</a><span>PLANTUPIA / LIVING SPATIAL ARCHIVE</span></div>

   <section className="pa-intro pa-world-intro" aria-labelledby="pa-title">
    <div><p className="sx-kicker">PROJECT ARCHIVE / 06 SOURCE-LED STUDIES</p><h1 id="pa-title">The work.<br/><em>And the relations between it.</em></h1></div>
    <div><p>Read every proposal on its own terms, or move across the archive by a shared spatial relationship.</p><span className="pa-edition">DESIGN PROPOSALS / INSTALLATION STATUS NOT ASSUMED</span></div>
   </section>

   <div className="pa-mode" role="group" aria-label="Choose archive reading mode">
     <button type="button" aria-pressed={mode==="projects"} onClick={()=>setMode("projects")}><span>01</span><strong>PROJECTS</strong><small>Direct access to the six source studies</small></button>
     <button type="button" aria-pressed={mode==="relations"} onClick={()=>setMode("relations")}><span>02</span><strong>RELATIONS</strong><small>Cross-project readings by spatial association</small></button>
   </div>

   {mode==="projects"?<>
     <nav className="pa-filter" aria-label="Filter project archive">
       {archiveFilters.map(item=><button key={item.id} type="button" aria-pressed={filter===item.id} onClick={()=>setFilter(item.id)}>{item.label}<span>{item.id==="all"?projects.length:projects.filter(p=>p.sector===item.id).length}</span></button>)}
     </nav>
     <section className="pa-wall pa-world-wall" aria-label="Selected proposal studies">
      {visible.map((project,index)=><article className={"pa-project pa-project-"+(projects.indexOf(project)+1)} key={project.id}>
        <a href={"/spaces/projects/"+project.id} className="pa-project-link" aria-label={"Open "+project.name+" case study"}>
          <figure>
            <img src={"/api/private-arrival/"+project.image} alt={project.alt} loading={index<2?"eager":"lazy"}/>
            <figcaption><span>{String(projects.indexOf(project)+1).padStart(2,"0")} / {project.name}</span><span>PROPOSAL P{project.page}</span></figcaption>
          </figure>
          <div className="pa-project-copy">
            <div><p className="sx-kicker">{project.sector.toUpperCase()} / {project.type.toUpperCase()}</p><h2>{project.title}</h2></div>
            <div><p>{project.body}</p><dl><div><dt>Study</dt><dd>{project.count}</dd></div><div><dt>Scope</dt><dd>{project.scope.join(" / ")}</dd></div><div><dt>Status</dt><dd>Design proposal</dd></div></dl><span className="pa-open">OPEN CASE STUDY ↗</span></div>
          </div>
        </a>
      </article>)}
     </section>
   </>:<>
     <section className="pa-relations" aria-labelledby="pa-relation-title">
       <div className="pa-relation-index" role="tablist" aria-label="Choose an archive relation">
         {relations.map((item,index)=><button key={item.id} type="button" role="tab" aria-selected={relation===index} onClick={()=>setRelation(index)}><span>{String(index+1).padStart(2,"0")}</span><strong>{item.label}</strong></button>)}
       </div>
       <header className="pa-relation-head"><p className="sx-kicker">RELATION / {String(relation+1).padStart(2,"0")}</p><h2 id="pa-relation-title">{currentRelation.label}</h2><p>{currentRelation.note}</p><small>Associative reading tool — not a formal company taxonomy.</small></header>
       <div className="pa-relation-field">
         {related.map((project,index)=><a href={"/spaces/projects/"+project.id} className={"pa-relation-item pa-relation-item-"+(index+1)} key={project.id}>
           <figure><img src={"/api/private-arrival/"+project.image} alt={project.alt}/><figcaption><span>{project.sector.toUpperCase()} / {project.type.toUpperCase()}</span><strong>{project.name}</strong><small>{project.count}</small></figcaption></figure>
         </a>)}
       </div>
       <div className="pa-relation-foot"><span>{currentRelation.label}</span><a href="/studio">SEE HOW THE STUDIO READS A PLACE ↗</a></div>
     </section>
   </>}

   <section className="pa-related" aria-labelledby="pa-related-title">
    <div><p className="sx-kicker">OFFICES & HQs / CATALOGUE EVIDENCE</p><h2 id="pa-related-title">Not every sector needs a borrowed case study.</h2></div>
    <div><p>The reviewed catalogue supports workplace planting, custom vessels and maintenance. No named office commission is included in this archive, so the sector stays grounded in catalogue evidence.</p><a href="/spaces/offices" className="sx-underlink">Explore offices & HQs ↗</a></div>
   </section>
  </main>

  <footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Project archive</span><a href="/studio">The Studio ↗</a></footer>
 </div>;
}
