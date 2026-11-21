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

const projectFilm=[
 ["audi-chapter-01.webp","AUDI CAIRO","ROOM / 01"],
 ["villa-choice-07.webp","VILLA 7","CHOICE / 07"],
 ["em-option-04.webp","EM SHERIF","OPTION / 04"],
 ["hallab-option-04.webp","EL HALLAB","OPTION / 04"],
 ["pixels-scene-05.webp","PIXELS MALL","UPPER LEVEL / 05"],
] as const;

const specimens=[
 ["shop-augusta-object.webp","VESSEL","Augusta planter","Hospitality catalogue / p27"],
 ["shop-plant-collection.webp","PLANT","Plant collection","Hospitality catalogue / p28"],
 ["shop-fiberglass-pots.webp","FINISH","Fiberglass family","Hospitality catalogue / p37"],
] as const;

const chapters=[
 ["place","01","PLACE"],["alternatives","02","ALTERNATIVES"],["object","03","OBJECT"],
 ["project","04","PROJECT"],["shop","05","SHOP"],["care","06","CARE"],
] as const;

const asset=(name:string)=>"/api/private-arrival/"+name;

export default function WorldStudy(){
 const [place,setPlace]=useState(0);
 const [lens,setLens]=useState<Lens>("space");
 const [alternative,setAlternative]=useState(0);
 const currentPlace=placeScenes[place];
 const currentAlternative=alternatives[alternative];

 return <div className="ws">
   <a className="ws-skip" href="#place">Skip to the world study</a>

   <header className="ws-top">
     <a className="ws-brand" href="/">Plantupia</a>
     <span className="ws-study-label">LIVING SPATIAL ARCHIVE / PRIVATE STUDY</span>
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
         <p>PRIVATE WORLD STUDY / SOURCE-LED</p>
         <h1 id="world-title">Space.<br/>Life.<br/><em>What continues.</em></h1>
       </div>
       <div className="ws-threshold-note">
         <span>PLACE → LIVING MATERIAL → CARE → TIME</span>
       </div>
       <a className="ws-enter" href="#place"><span>ENTER</span><i>↓</i></a>
     </section>

     <section id="place" className="ws-chapter ws-place" aria-labelledby="place-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>01 / PLACE</span>
         <h2 id="place-title">Read the condition.</h2>
         <div className="ws-lenses" aria-label="Read the current place through a lens">
           {lenses.map(([id,label])=><button key={id} type="button" aria-pressed={lens===id} onClick={()=>setLens(id)}>{label}</button>)}
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
             {placeScenes.map((item,index)=><button key={item.asset} type="button" aria-pressed={place===index} onClick={()=>setPlace(index)}>
               <span>{String(index+1).padStart(2,"0")}</span><strong>{item.label}</strong>
             </button>)}
           </div>
         </aside>
       </div>
       <div className="ws-bridge"><span>PLACE</span><i>→</i><span>CHANGE ONE THING</span></div>
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
           {alternatives.map((item,index)=><button key={item[0]} type="button" aria-current={alternative===index?"true":undefined} onClick={()=>setAlternative(index)}>
             <img src={asset(item[0])} alt=""/>
             <span>{item[1]}</span><strong>{item[2]}</strong>
           </button>)}
         </div>
       </div>
       <div className="ws-bridge ws-bridge-dark"><span>ALTERNATIVE</span><i>→</i><span>MATERIAL</span></div>
     </section>

     <section id="object" className="ws-chapter ws-object" aria-labelledby="object-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>03 / OBJECT</span>
         <h2 id="object-title">Setting → vessel → plant → finish.</h2>
         <small>ONE SPECIFICATION, READ AT DIFFERENT SCALES</small>
       </header>
       <div className="ws-object-setting">
         <figure className="ws-object-space">
           <img src={asset("shop-augusta-wide.webp")} alt="Augusta planter in a hospitality setting"/>
           <figcaption><span>SETTING</span><strong>Augusta / in space</strong></figcaption>
         </figure>
         <div className="ws-specimen-drawer">
           <div className="ws-drawer-mast"><span>MATERIAL DRAWER / 03</span><span>CATALOGUE STUDY</span></div>
           {specimens.map(([src,type,name,source])=><figure key={src}>
             <img src={asset(src)} alt={name}/>
             <figcaption><span>{type}</span><strong>{name}</strong><small>{source}</small></figcaption>
           </figure>)}
         </div>
       </div>
       <div className="ws-bridge"><span>MATERIAL</span><i>→</i><span>MEMORY</span></div>
     </section>

     <section id="project" className="ws-chapter ws-project" aria-labelledby="project-title">
       <header className="ws-chapter-head ws-chapter-head-tight">
         <span>04 / PROJECT</span>
         <h2 id="project-title">One practice.<br/>Different evidence.</h2>
         <small>PROJECTS KEEP THEIR OWN SOURCE BEHAVIOUR</small>
       </header>
       <div className="ws-film" role="list">
         {projectFilm.map(([src,title,meta],index)=><figure key={src} role="listitem">
           <img src={asset(src)} alt={title+" proposal study"}/>
           <figcaption><span>{String(index+1).padStart(2,"0")} / {meta}</span><strong>{title}</strong></figcaption>
         </figure>)}
       </div>
       <a className="ws-project-link" href="/spaces/projects">OPEN PROJECT ARCHIVE ↗</a>
       <div className="ws-bridge"><span>PROJECT</span><i>→</i><span>CHOOSE SOMETHING REAL</span></div>
     </section>

     <section id="shop" className="ws-chapter ws-shop" aria-labelledby="shop-title">
       <div className="ws-shop-space">
         <img src={asset("shop-indoor-greenery.webp")} alt="Plantupia indoor greenery catalogue setting"/>
         <div><span>05 / SHOP</span><h2 id="shop-title">See it in a place.<br/><em>Then choose the object.</em></h2></div>
       </div>
       <div className="ws-shop-order">
         <div>
           <span>DIRECT COMMERCE</span>
           <p>Plants. Planters. Care. No detour.</p>
         </div>
         <nav aria-label="Shop entry points">
           <a href="/shop#shop-collection"><span>01</span><strong>SHOP THE COLLECTION</strong><i>↗</i></a>
           <a href="/shop#sr-material-title"><span>02</span><strong>MATERIAL LIBRARY</strong><i>↗</i></a>
           <a href="/start-a-space?topic=project"><span>03</span><strong>SPECIFY FOR A SPACE</strong><i>↗</i></a>
         </nav>
       </div>
     </section>

     <section id="care" className="ws-chapter ws-care" aria-labelledby="care-title">
       <header className="ws-chapter-head ws-chapter-head-tight ws-head-dark">
         <span>06 / CARE</span>
         <h2 id="care-title">Day one is not<br/>the whole story.</h2>
         <small>THE CONTINUITY LAYER / NOT LIVE TELEMETRY</small>
       </header>
       <div className="ws-care-sequence">
         <ol>
           <li><span>01</span><strong>DESIGN</strong><i>place + living material</i></li>
           <li><span>02</span><strong>INSTALL</strong><i>handover + ownership</i></li>
           <li><span>03</span><strong>RETURN</strong><i>care + observation</i></li>
           <li><span>04</span><strong>REMEMBER</strong><i>history + change</i></li>
         </ol>
         <figure>
           <img src={asset("sector-commercial-brand.webp")} alt="Plantupia commercial interior proposal study"/>
           <figcaption><span>CONTINUITY</span><strong>Space → life → care → time</strong></figcaption>
         </figure>
       </div>
     </section>

     <section className="ws-end">
       <span>WORKING SYSTEM / PRIVATE STUDY</span>
       <p>Less explanation.<br/>More evidence.</p>
       <div><a href="/">CURRENT EXPERIENCE</a><a href="/study/visual-direction">ARCHIVE INSTRUMENT ↗</a></div>
     </section>
   </main>
 </div>;
}
