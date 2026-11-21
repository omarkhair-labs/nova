"use client";

import { useState } from "react";
import SiteIndex from "./SiteIndex";
import "./world-study.css";

type Lens="space"|"life"|"time";
type PlaceScene={
  asset:string;
  label:string;
  source:string;
  readings:Record<Lens,string>;
  cues:[string,string];
};
type RelationItem={asset:string;label:string;meta:string;href:string};
type Relation={id:string;label:string;items:RelationItem[]};

const lenses:[Lens,string][]=[["space","SPACE"],["life","LIFE"],["time","TIME"]];

const placeScenes:PlaceScene[]=[
 {asset:"home-pixels-courtyard.webp",label:"Arrival",source:"PIXELS MALL / PROPOSAL P03",readings:{
   space:"A planted landmark gives the arrival a centre.",
   life:"Planting reads as architecture before it reads as decoration.",
   time:"The landmark only stays legible if access and care continue after handover."
 },cues:["LANDMARK","GATHER"]},
 {asset:"home-pixels-promenade.webp",label:"Movement",source:"PIXELS MALL / PROPOSAL P16",readings:{
   space:"Repetition follows the pedestrian route and gives movement a rhythm.",
   life:"Greenery becomes a recurring spatial layer instead of a collection of isolated pots.",
   time:"A repeated system needs a repeatable maintenance route."
 },cues:["PATH","REPETITION"]},
 {asset:"home-pixels-architecture.webp",label:"Edge",source:"PIXELS MALL / PROPOSAL P24",readings:{
   space:"The planting line extends the architectural edge.",
   life:"Living material softens the boundary without erasing it.",
   time:"Edges reveal change first: density, replacement and seasonal condition remain visible."
 },cues:["BOUNDARY","HORIZON"]},
 {asset:"audi-chapter-03.webp",label:"Interior",source:"AUDI CAIRO / PROPOSAL STUDY",readings:{
   space:"The planting changes scale and atmosphere without becoming the room.",
   life:"The plant palette works with furniture, light and circulation.",
   time:"Indoor life continues through observation, access and routine care."
 },cues:["SCALE","ATMOSPHERE"]},
];

const alternatives=[
 ["mazal-option-01.webp","01","Tropical blend"],
 ["mazal-option-03.webp","03","Luxury tropical"],
 ["mazal-option-05.webp","05","Dense green"],
 ["mazal-option-07.webp","07","Sansevieria"],
 ["mazal-option-09.webp","09","Evergreen cones"],
] as const;

const specimens=[
 ["shop-augusta-object.webp","VESSEL","Augusta planter","Hospitality catalogue / p27"],
 ["shop-plant-collection.webp","PLANT","Plant collection","Hospitality catalogue / p28"],
 ["shop-fiberglass-pots.webp","FINISH","Fiberglass family","Hospitality catalogue / p37"],
] as const;

const relations:Relation[]=[
 {id:"arrival",label:"ARRIVAL",items:[
   {asset:"home-pixels-courtyard.webp",label:"Pixels Mall",meta:"COMMERCIAL / ARRIVAL",href:"/spaces/projects/pixels-mall"},
   {asset:"audi-chapter-01.webp",label:"Audi Cairo",meta:"INTERIOR / ENTRY",href:"/spaces/projects/audi-cairo"},
   {asset:"villa-choice-07.webp",label:"Villa 7",meta:"RESIDENTIAL / APPROACH",href:"/spaces/projects/villa-7"},
 ]},
 {id:"edge",label:"EDGE",items:[
   {asset:"home-pixels-architecture.webp",label:"Pixels Mall",meta:"PLANTING / BOUNDARY",href:"/spaces/projects/pixels-mall"},
   {asset:"mazal-option-09.webp",label:"Mazal",meta:"FACADE / FRONTAGE",href:"/spaces/projects/mazal"},
   {asset:"hallab-option-04.webp",label:"El Hallab",meta:"PLANTING / PERIMETER",href:"/spaces/projects/el-hallab"},
 ]},
 {id:"vessel",label:"VESSEL",items:[
   {asset:"shop-augusta-object.webp",label:"Augusta",meta:"CATALOGUE / OBJECT",href:"/shop#sr-material-title"},
   {asset:"em-option-04.webp",label:"EM Sherif",meta:"PLANTER / OPTION",href:"/spaces/projects/em-sherif"},
   {asset:"villa-choice-07.webp",label:"Villa 7",meta:"OBJECT / LANDSCAPE",href:"/spaces/projects/villa-7"},
 ]},
 {id:"threshold",label:"THRESHOLD",items:[
   {asset:"pixels-scene-05.webp",label:"Pixels Mall",meta:"UPPER LEVEL / TRANSITION",href:"/spaces/projects/pixels-mall"},
   {asset:"audi-chapter-03.webp",label:"Audi Cairo",meta:"ROOM / TRANSITION",href:"/spaces/projects/audi-cairo"},
   {asset:"shop-indoor-greenery.webp",label:"Plantupia Shop",meta:"SETTING / INTERIOR",href:"/shop"},
 ]},
];

const chapters=[
 ["place","01","PLACE"],["alternatives","02","ALTERNATIVES"],["object","03","OBJECT"],
 ["project","04","RELATIONS"],["shop","05","SHOP"],["system","06","SYSTEM"],
] as const;

const asset=(name:string)=>"/api/private-arrival/"+name;

export default function WorldExperience(){
 const [place,setPlace]=useState(0);
 const [lens,setLens]=useState<Lens>("space");
 const [alternative,setAlternative]=useState(0);
 const [specimen,setSpecimen]=useState(0);
 const [relation,setRelation]=useState(0);
 const currentPlace=placeScenes[place];
 const currentAlternative=alternatives[alternative];
 const currentSpecimen=specimens[specimen];
 const currentRelation=relations[relation];

 function shift(update:()=>void){
   const doc=document as Document & {startViewTransition?:(callback:()=>void)=>unknown};
   const reduced=window.matchMedia("(prefers-reduced-motion: reduce)").matches;
   if(doc.startViewTransition&&!reduced) doc.startViewTransition(update);
   else update();
 }

 return <div className="ws">
   <a className="ws-skip" href="#place">Skip to the world study</a>

   <header className="ws-top">
     <a className="ws-brand" href="/">Plantupia</a>
     <span className="ws-study-label">LIVING SPATIAL ARCHIVE / PLANTUPIA</span>
     <SiteIndex current={null} tone="dark"/>
     <a className="ws-shop-link" href="/shop">SHOP ↗</a>
   </header>

   <nav className="ws-spine" aria-label="World study chapters">
     {chapters.map(([id,n,label])=><a key={id} href={"#"+id}><span>{n}</span><strong>{label}</strong></a>)}
   </nav>

   <main>
     <section className="ws-threshold" aria-labelledby="world-title">
       <img src={asset("arrival-hero.webp")} alt="Plantupia architectural greenery study"/>
       <div className="ws-threshold-veil"/>
       <div className="ws-threshold-copy">
         <p>SPATIAL DESIGN / LIVING MATERIAL / GREEN TECHNOLOGY</p>
         <h1 id="world-title">Space.<br/>Life.<br/><em>What continues.</em></h1>
       </div>
       <div className="ws-threshold-note"><span>SPATIAL DESIGN → LIVING MATERIAL → GREEN TECHNOLOGY</span></div>
       <a className="ws-enter" href="#place"><span>ENTER</span><i>↓</i></a>
     </section>

     <section id="place" className="ws-chapter ws-place" aria-labelledby="place-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>01 / PLACE</span>
         <h2 id="place-title">Read the condition.</h2>
         <div className="ws-lenses" aria-label="Read the current place through a lens">
           {lenses.map(([id,label])=><button key={id} type="button" aria-pressed={lens===id} onClick={()=>shift(()=>setLens(id))}>{label}</button>)}
         </div>
       </header>

       <div className="ws-place-stage">
         <figure>
           <img key={currentPlace.asset} src={asset(currentPlace.asset)} alt={currentPlace.label+" spatial study"}/>
           <div className="ws-scene-cues" aria-hidden="true"><span>{currentPlace.cues[0]}</span><span>{currentPlace.cues[1]}</span></div>
           <figcaption><span>{currentPlace.source}</span><strong>{currentPlace.label}</strong></figcaption>
         </figure>
         <aside>
           <div className="ws-reading-key"><span>{lens.toUpperCase()}</span><span>DESCRIPTIVE STUDY</span></div>
           <p key={currentPlace.asset+lens}>{currentPlace.readings[lens]}</p>
           <div className="ws-place-index">
             {placeScenes.map((item,index)=><button key={item.asset} type="button" aria-pressed={place===index} onClick={()=>shift(()=>setPlace(index))}>
               <span>{String(index+1).padStart(2,"0")}</span><strong>{item.label}</strong>
             </button>)}
           </div>
         </aside>
       </div>
       <div className="ws-bridge"><span>PLACE</span><i>→</i><a href="/spaces">EXPLORE SPACES ↗</a></div>
     </section>

     <section id="alternatives" className="ws-chapter ws-alternatives" aria-labelledby="alternatives-title">
       <header className="ws-chapter-head ws-chapter-head-tight ws-head-dark">
         <span>02 / ALTERNATIVES</span>
         <h2 id="alternatives-title">Same frame.<br/>Different presence.</h2>
         <small>MAZAL / FIVE OF NINE SOURCE OPTIONS</small>
       </header>
       <div className="ws-contact">
         <figure className="ws-contact-main">
           <img key={currentAlternative[0]} src={asset(currentAlternative[0])} alt={"Mazal planting alternative "+currentAlternative[1]}/>
           <figcaption><span>OPTION {currentAlternative[1]}</span><strong>{currentAlternative[2]}</strong></figcaption>
         </figure>
         <div className="ws-contact-sheet">
           {alternatives.map((item,index)=><button key={item[0]} type="button" aria-current={alternative===index?"true":undefined} onClick={()=>shift(()=>setAlternative(index))}>
             <img src={asset(item[0])} alt=""/>
             <span>{item[1]}</span><strong>{item[2]}</strong>
           </button>)}
         </div>
       </div>
       <div className="ws-bridge ws-bridge-dark"><span>ALTERNATIVE</span><i>→</i><a href="/spaces/projects/mazal">OPEN MAZAL ↗</a></div>
     </section>

     <section id="object" className="ws-chapter ws-object" aria-labelledby="object-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>03 / OBJECT</span>
         <h2 id="object-title">Setting → vessel → plant → finish.</h2>
         <small>CHOOSE ONE / IT WILL FOLLOW YOU</small>
       </header>
       <div className="ws-object-setting">
         <figure className="ws-object-space">
           <img src={asset("shop-augusta-wide.webp")} alt="Augusta planter in a hospitality setting"/>
           <figcaption><span>SETTING</span><strong>Augusta / in space</strong></figcaption>
         </figure>
         <div className="ws-specimen-drawer" role="listbox" aria-label="Choose a material trace">
           <div className="ws-drawer-mast"><span>MATERIAL DRAWER / 03</span><span>CATALOGUE STUDY</span></div>
           {specimens.map(([src,type,name,source],index)=><button key={src} type="button" role="option" aria-selected={specimen===index} onClick={()=>shift(()=>setSpecimen(index))}>
             <img src={asset(src)} alt={name}/>
             <span className="ws-specimen-copy"><b>{type}</b><strong>{name}</strong><small>{source}</small></span>
           </button>)}
         </div>
       </div>
       <div className="ws-bridge"><span>MATERIAL</span><i>→</i><a href="/shop#sr-material-title">MATERIAL LIBRARY ↗</a></div>
     </section>

     <section id="project" className="ws-chapter ws-project" aria-labelledby="project-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>04 / RELATIONS</span>
         <h2 id="project-title">Projects meet<br/>by association.</h2>
         <small>ASSOCIATIVE INDEX / CROSS-PROJECT READING</small>
       </header>

       <div className="ws-relation-tabs" role="tablist" aria-label="Cross-project relations">
         {relations.map((item,index)=><button key={item.id} type="button" role="tab" aria-selected={relation===index} onClick={()=>shift(()=>setRelation(index))}>
           <span>{String(index+1).padStart(2,"0")}</span><strong>{item.label}</strong>
         </button>)}
       </div>

       <div className="ws-relations" key={currentRelation.id}>
         {currentRelation.items.map((item,index)=><a href={item.href} key={item.asset} className={"ws-relation-item ws-relation-"+(index+1)}>
           <figure>
             <img src={asset(item.asset)} alt={item.label+" / "+currentRelation.label.toLowerCase()+" relation"}/>
             <figcaption><span>{item.meta}</span><strong>{item.label}</strong></figcaption>
           </figure>
         </a>)}
       </div>

       <div className="ws-relation-word" aria-hidden="true">{currentRelation.label}</div>
       <div className="ws-bridge"><span>ASSOCIATION</span><i>→</i><a href="/spaces/projects">PROJECT ARCHIVE ↗</a></div>
     </section>

     <section id="shop" className="ws-chapter ws-shop" aria-labelledby="shop-title">
       <div className="ws-shop-space">
         <img src={asset("shop-indoor-greenery.webp")} alt="Plantupia indoor greenery catalogue setting"/>
         <div><span>05 / SHOP</span><h2 id="shop-title">See it in a place.<br/><em>Then choose the object.</em></h2></div>
       </div>
       <div className="ws-shop-order">
         <div><span>DIRECT COMMERCE</span><p>Plants. Planters. Care. No detour.</p></div>
         <nav aria-label="Shop entry points">
           <a href="/shop#shop-collection"><span>01</span><strong>SHOP THE COLLECTION</strong><i>↗</i></a>
           <a href="/shop#sr-material-title"><span>02</span><strong>MATERIAL LIBRARY</strong><i>↗</i></a>
           <a href="/start-a-space?topic=project"><span>03</span><strong>SPECIFY FOR A SPACE</strong><i>↗</i></a>
         </nav>
       </div>
     </section>

     <section id="system" className="ws-chapter ws-system" aria-labelledby="system-title">
       <header className="ws-chapter-head ws-chapter-head-tight ws-head-dark">
         <span>06 / SYSTEM</span>
         <h2 id="system-title">A living space<br/>needs a system.</h2>
         <small>PHYSICAL CARE + DIGITAL CONTINUITY / TWO EVIDENCE TYPES</small>
       </header>

       <div className="ws-system-field">
         <aside className="ws-system-lane ws-system-physical">
           <div className="ws-system-lane-head"><span>PHYSICAL</span><strong>CARE PROGRAMME</strong></div>
           <ol>
             <li><span>01</span><strong>INSTALL</strong><small>selection + installation</small></li>
             <li><span>02</span><strong>IRRIGATE</strong><small>smart irrigation</small></li>
             <li><span>03</span><strong>RETURN</strong><small>scheduled maintenance</small></li>
             <li><span>04</span><strong>PROTECT</strong><small>plant warranty</small></li>
           </ol>
           <p>Named in supplied Plantupia proposals. Exact terms stay project-specific.</p>
         </aside>

         <figure className="ws-system-core">
           <img src={asset("sector-commercial-brand.webp")} alt="Plantupia commercial interior proposal study"/>
           <div className="ws-system-axis" aria-hidden="true"><span>PLACE</span><i/><span>PLANT</span><i/><span>PERSON</span><i/><span>TIME</span></div>
           <figcaption><span>GREEN TECHNOLOGY / CONTINUITY</span><strong>The system begins after the image.</strong></figcaption>
         </figure>

         <aside className="ws-system-lane ws-system-digital">
           <div className="ws-system-lane-head"><span>DIGITAL</span><strong>LIVING RECORD</strong></div>
           <ol>
             <li><span>01</span><strong>HANDOVER</strong><small>authorized QR confirmation</small></li>
             <li><span>02</span><strong>OWN</strong><small>My Plants</small></li>
             <li><span>03</span><strong>PLAN</strong><small>Care Schedule</small></li>
             <li><span>04</span><strong>REMEMBER</strong><small>Living History</small></li>
           </ol>
           <p>Current product direction. No live sensor feed, automated diagnosis or fabricated telemetry.</p>
         </aside>
       </div>

       <div className="ws-system-memory">
         <div className="ws-system-memory-head"><span>YOUR TRACE / THIS SESSION</span><span>NOT SAVED</span></div>
         <div className="ws-memory" aria-label="Your current world trace">
           <figure>
             <img src={asset(currentPlace.asset)} alt=""/>
             <figcaption><span>PLACE / {lens.toUpperCase()}</span><strong>{currentPlace.label}</strong></figcaption>
           </figure>
           <i aria-hidden="true">→</i>
           <figure>
             <img src={asset(currentAlternative[0])} alt=""/>
             <figcaption><span>ALTERNATIVE / {currentAlternative[1]}</span><strong>{currentAlternative[2]}</strong></figcaption>
           </figure>
           <i aria-hidden="true">→</i>
           <figure>
             <img src={asset(currentSpecimen[0])} alt=""/>
             <figcaption><span>{currentSpecimen[1]}</span><strong>{currentSpecimen[2]}</strong></figcaption>
           </figure>
           <i className="ws-memory-to-system" aria-hidden="true">→</i>
           <div className="ws-memory-record"><span>HANDOVER</span><strong>becomes a living record</strong></div>
         </div>
       </div>

       <div className="ws-system-foot">
         <span>DESIGN</span><i>→</i><span>INSTALL</span><i>→</i><span>HANDOVER</span><i>→</i><span>RETURN</span><i>→</i><span>REMEMBER</span>
       </div>
     </section>

     <section className="ws-end">
       <span>PLANTUPIA / LIVING ARCHITECTURE</span>
       <p>Spatial design.<br/>Living systems.</p>
       <div><a href="/">SPACES</a><a href="/spaces/projects">PROJECT ARCHIVE ↗</a></div>
     </section>
   </main>
 </div>;
}
