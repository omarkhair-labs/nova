"use client";
import { useMemo, useState } from "react";
import "./visual-direction-lab.css";

type Mode = "field" | "compare" | "material";
type Study = { src:string; label:string; meta:string; note:string };

const field:Study[]=[
 {src:"pixels-scene-01.webp",label:"Arrival",meta:"PIXELS MALL / P03",note:"The project begins as a place, not a card."},
 {src:"pixels-scene-02.webp",label:"Promenade",meta:"PIXELS MALL / P07",note:"Planting follows circulation and edge conditions."},
 {src:"pixels-scene-03.webp",label:"Terrace",meta:"PIXELS MALL / P16",note:"The same project can change scale without changing its language."},
 {src:"pixels-scene-04.webp",label:"Threshold",meta:"PIXELS MALL / P24",note:"Architecture and living material share the frame."},
 {src:"pixels-scene-05.webp",label:"Upper level",meta:"PIXELS MALL / P28",note:"A sequence can carry more truth than one hero image."},
];
const compare:Study[]=[
 {src:"mazal-option-01.webp",label:"Tropical blend",meta:"MAZAL / OPTION 01",note:"One facade. Different presence."},
 {src:"mazal-option-02.webp",label:"Layered palms",meta:"MAZAL / OPTION 02",note:"Keep architecture fixed; let planting become the variable."},
 {src:"mazal-option-03.webp",label:"Luxury tropical",meta:"MAZAL / OPTION 03",note:"Comparison is the content, not a caption beneath it."},
 {src:"mazal-option-04.webp",label:"Vertical rhythm",meta:"MAZAL / OPTION 04",note:"The eye should decide before the paragraph explains."},
 {src:"mazal-option-05.webp",label:"Dense green",meta:"MAZAL / OPTION 05",note:"Differences remain visible because the camera stays consistent."},
 {src:"mazal-option-06.webp",label:"Soft edge",meta:"MAZAL / OPTION 06",note:"A design decision can be browsed like a contact sheet."},
 {src:"mazal-option-07.webp",label:"Sansevieria",meta:"MAZAL / OPTION 07",note:"Species choice changes character without changing the site."},
 {src:"mazal-option-08.webp",label:"Alternating rhythm",meta:"MAZAL / OPTION 08",note:"The archive records alternatives, not just winners."},
 {src:"mazal-option-09.webp",label:"Evergreen cones",meta:"MAZAL / OPTION 09",note:"Keep the rejected directions visible as design evidence."},
];
const material:Study[]=[
 {src:"shop-augusta-wide.webp",label:"Augusta / setting",meta:"HOSPITALITY CATALOGUE / P27",note:"Object and place belong in the same reading."},
 {src:"shop-augusta-object.webp",label:"Augusta / object",meta:"HOSPITALITY CATALOGUE / P27",note:"Then isolate the object only when inspection matters."},
 {src:"shop-plant-collection.webp",label:"Plant palette",meta:"HOSPITALITY CATALOGUE / P28",note:"A palette is a design material, not merely inventory."},
 {src:"shop-fiberglass-pots.webp",label:"Vessel family",meta:"HOSPITALITY CATALOGUE / P37",note:"Family, finish and scale form a specification layer."},
 {src:"em-option-04.webp",label:"Planting option",meta:"EM SHERIF / OPTION 04",note:"The archive can behave like a specimen drawer."},
 {src:"hallab-option-04.webp",label:"Edge study",meta:"EL HALLAB / OPTION 04",note:"Alternatives sit close enough to compare, not pages apart."},
];

const modes:{id:Mode;index:string;label:string;sub:string}[]=[
 {id:"field",index:"01",label:"FIELD",sub:"move through one place"},
 {id:"compare",index:"02",label:"CONTACT",sub:"hold the frame, change the presence"},
 {id:"material",index:"03",label:"SPECIMEN",sub:"inspect object, plant and condition"},
];

function asset(src:string){return "/api/private-arrival/"+src;}

export default function VisualDirectionLab(){
 const [mode,setMode]=useState<Mode>("field");
 const [active,setActive]=useState(0);
 const list=mode==="field"?field:mode==="compare"?compare:material;
 const current=list[Math.min(active,list.length-1)];
 const currentMode=useMemo(()=>modes.find(item=>item.id===mode)!,[mode]);
 function choose(next:Mode){setMode(next);setActive(0);}
 function step(delta:number){setActive(index=>(index+delta+list.length)%list.length);}
 return <main className={"vdi vdi-"+mode}>
   <header className="vdi-top">
     <a className="vdi-brand" href="/">Plantupia</a>
     <span className="vdi-status">ARCHIVE INSTRUMENT / PRIVATE STUDY</span>
     <a className="vdi-back" href="/spaces/projects">Project archive ↗</a>
   </header>

   <nav className="vdi-modes" aria-label="Ways of reading the Plantupia archive">
     {modes.map(item=><button key={item.id} type="button" aria-pressed={mode===item.id} onClick={()=>choose(item.id)}>
       <span>{item.index}</span><strong>{item.label}</strong><small>{item.sub}</small>
     </button>)}
   </nav>

   <section className="vdi-stage" aria-labelledby="vdi-current-title">
     <figure className="vdi-frame">
       <img key={current.src} src={asset(current.src)} alt={current.label+" Plantupia archive study"}/>
       <figcaption>
         <span>{current.meta}</span>
         <strong id="vdi-current-title">{current.label}</strong>
       </figcaption>
       <div className="vdi-step" aria-label="Move through this study">
         <button type="button" onClick={()=>step(-1)} aria-label="Previous study">←</button>
         <span>{String(active+1).padStart(2,"0")} / {String(list.length).padStart(2,"0")}</span>
         <button type="button" onClick={()=>step(1)} aria-label="Next study">→</button>
       </div>
     </figure>

     <aside className="vdi-reading" aria-live="polite">
       <div className="vdi-reading-top">
         <span>{currentMode.index} / {currentMode.label}</span>
         <span>OWN ARCHIVE / SOURCE-LED</span>
       </div>
       <p>{current.note}</p>
       <div className="vdi-rule"/>
       <p className="vdi-thesis">
         {mode==="field"&&"Do not reduce a spatial proposal to one cover image."}
         {mode==="compare"&&"Do not hide the alternatives that explain the decision."}
         {mode==="material"&&"Do not flatten objects, planting and care into a generic product grid."}
       </p>
     </aside>
   </section>

   <section className="vdi-index" aria-label={currentMode.label+" archive index"}>
     <div className="vdi-index-head"><span>{currentMode.label} INDEX</span><span>{list.length} STUDIES</span></div>
     <div className="vdi-strip" role="list">
       {list.map((item,i)=><button key={item.src} type="button" role="listitem" aria-current={i===active?"true":undefined} onClick={()=>setActive(i)}>
         <figure><img src={asset(item.src)} alt=""/><figcaption><span>{String(i+1).padStart(2,"0")}</span><strong>{item.label}</strong></figcaption></figure>
       </button>)}
     </div>
   </section>

   <footer className="vdi-foot">
     <span>PLACE → PLANT → HUMAN CARE → TIME</span>
     <p>The archive decides the behaviour. The interface stays quiet enough for the work to lead.</p>
     <a href="/study/navigation">Navigation study ↗</a>
   </footer>
 </main>;
}
