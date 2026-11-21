"use client";

import { useState } from "react";
import SiteIndex from "./SiteIndex";
import "./world-study.css";

type PlaceScene={asset:string;label:string;source:string;note:string};
const placeScenes:PlaceScene[]=[
 {asset:"home-pixels-courtyard.webp",label:"Arrival",source:"PIXELS MALL / PROPOSAL P03",note:"Planting establishes a landmark before it becomes a detail."},
 {asset:"home-pixels-promenade.webp",label:"Movement",source:"PIXELS MALL / PROPOSAL P16",note:"Greenery follows circulation rather than sitting beside it."},
 {asset:"home-pixels-architecture.webp",label:"Edge",source:"PIXELS MALL / PROPOSAL P24",note:"The planting line becomes part of the architecture."},
 {asset:"audi-chapter-03.webp",label:"Interior",source:"AUDI CAIRO / PROPOSAL STUDY",note:"Living material changes the atmosphere without taking over the room."},
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
 ["place","01","PLACE"],
 ["alternatives","02","ALTERNATIVES"],
 ["object","03","OBJECT"],
 ["project","04","PROJECT"],
 ["shop","05","SHOP"],
 ["care","06","CARE"],
] as const;

const asset=(name:string)=>"/api/private-arrival/"+name;

export default function WorldStudy(){
 const [place,setPlace]=useState(0);
 const [alternative,setAlternative]=useState(0);
 const currentPlace=placeScenes[place];
 const currentAlternative=alternatives[alternative];

 return <div className="ws">
   <a className="ws-skip" href="#place">Skip to the world study</a>

   <header className="ws-top">
     <a className="ws-brand" href="/">Plantupia</a>
     <span className="ws-study-label">LIVING SPATIAL ARCHIVE / PRIVATE SYSTEM STUDY</span>
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
         <p>PRIVATE WORLD STUDY / NOT A PUBLIC CLAIM</p>
         <h1 id="world-title">Space.<br/>Life.<br/><em>What continues.</em></h1>
       </div>
       <div className="ws-threshold-note">
         <span>READ THE SITE AS MATERIAL</span>
         <p>Projects, alternatives, objects, shop and care belong to one world. They should not behave like six unrelated templates.</p>
       </div>
       <a className="ws-enter" href="#place"><span>ENTER</span><i>↓</i></a>
     </section>

     <section id="place" className="ws-chapter ws-place" aria-labelledby="place-title">
       <header className="ws-chapter-head">
         <span>01 / PLACE</span>
         <h2 id="place-title">The project starts<br/>with a condition.</h2>
         <p>One place can be read through arrival, movement, edge and interior. Navigation changes what the visitor is looking for, not just which image is visible.</p>
       </header>
       <div className="ws-place-stage">
         <figure>
           <img key={currentPlace.asset} src={asset(currentPlace.asset)} alt={currentPlace.label+" spatial study"}/>
           <figcaption><span>{currentPlace.source}</span><strong>{currentPlace.label}</strong></figcaption>
         </figure>
         <aside>
           <p>{currentPlace.note}</p>
           <div className="ws-place-index">
             {placeScenes.map((item,index)=><button key={item.asset} type="button" aria-pressed={place===index} onClick={()=>setPlace(index)}>
               <span>{String(index+1).padStart(2,"0")}</span><strong>{item.label}</strong>
             </button>)}
           </div>
         </aside>
       </div>
     </section>

     <section id="alternatives" className="ws-chapter ws-alternatives" aria-labelledby="alternatives-title">
       <header className="ws-chapter-head ws-head-dark">
         <span>02 / ALTERNATIVES</span>
         <h2 id="alternatives-title">Keep the frame.<br/>Change the presence.</h2>
         <p>The archive should not hide the options that explain a design decision. Mazal already contains the interaction: one facade, different planting identities.</p>
       </header>
       <div className="ws-contact">
         <figure className="ws-contact-main">
           <img key={currentAlternative[0]} src={asset(currentAlternative[0])} alt={"Mazal planting alternative "+currentAlternative[1]}/>
           <figcaption><span>MAZAL / OPTION {currentAlternative[1]}</span><strong>{currentAlternative[2]}</strong></figcaption>
         </figure>
         <div className="ws-contact-sheet">
           {alternatives.map((item,index)=><button key={item[0]} type="button" aria-current={alternative===index?"true":undefined} onClick={()=>setAlternative(index)}>
             <img src={asset(item[0])} alt=""/>
             <span>{item[1]}</span><strong>{item[2]}</strong>
           </button>)}
         </div>
       </div>
       <p className="ws-contact-rule">The visitor sees the difference before the interface explains it.</p>
     </section>

     <section id="object" className="ws-chapter ws-object" aria-labelledby="object-title">
       <header className="ws-chapter-head">
         <span>03 / OBJECT</span>
         <h2 id="object-title">An object is never<br/>only an object.</h2>
         <p>Plant, vessel, finish and spatial condition are one specification problem. The shop can inherit this language without becoming a decorative catalogue.</p>
       </header>
       <div className="ws-object-setting">
         <figure className="ws-object-space">
           <img src={asset("shop-augusta-wide.webp")} alt="Augusta planter in a hospitality setting"/>
           <figcaption><span>SETTING</span><strong>Augusta / in space</strong></figcaption>
         </figure>
         <div className="ws-specimen-drawer">
           <div className="ws-drawer-mast"><span>MATERIAL DRAWER / 03</span><span>PLANTUPIA CATALOGUE STUDY</span></div>
           {specimens.map(([src,type,name,source])=><figure key={src}>
             <img src={asset(src)} alt={name}/>
             <figcaption><span>{type}</span><strong>{name}</strong><small>{source}</small></figcaption>
           </figure>)}
         </div>
       </div>
     </section>

     <section id="project" className="ws-chapter ws-project" aria-labelledby="project-title">
       <header className="ws-chapter-head">
         <span>04 / PROJECT</span>
         <h2 id="project-title">Different projects.<br/>One visual memory.</h2>
         <p>A project archive does not need identical cards. It needs enough consistency to feel like one practice, and enough difference to preserve what each source actually contains.</p>
       </header>
       <div className="ws-film" role="list">
         {projectFilm.map(([src,title,meta],index)=><figure key={src} role="listitem">
           <img src={asset(src)} alt={title+" proposal study"}/>
           <figcaption><span>{String(index+1).padStart(2,"0")} / {meta}</span><strong>{title}</strong></figcaption>
         </figure>)}
       </div>
       <a className="ws-project-link" href="/spaces/projects">OPEN PROJECT ARCHIVE ↗</a>
     </section>

     <section id="shop" className="ws-chapter ws-shop" aria-labelledby="shop-title">
       <div className="ws-shop-space">
         <img src={asset("shop-indoor-greenery.webp")} alt="Plantupia indoor greenery catalogue setting"/>
         <div><span>05 / SHOP</span><h2 id="shop-title">See it in a place.<br/><em>Then choose the object.</em></h2></div>
       </div>
       <div className="ws-shop-order">
         <div>
           <span>TRANSACTION WITHOUT LEAVING THE WORLD</span>
           <p>Commerce stays direct: plants, planters and care. The spatial story gives context; it does not hide price, stock or the route to buy.</p>
         </div>
         <nav aria-label="Shop entry points">
           <a href="/shop#shop-collection"><span>01</span><strong>SHOP THE COLLECTION</strong><i>↗</i></a>
           <a href="/shop#sr-material-title"><span>02</span><strong>MATERIAL LIBRARY</strong><i>↗</i></a>
           <a href="/start-a-space?topic=project"><span>03</span><strong>SPECIFY FOR A SPACE</strong><i>↗</i></a>
         </nav>
       </div>
     </section>

     <section id="care" className="ws-chapter ws-care" aria-labelledby="care-title">
       <header className="ws-chapter-head ws-head-dark">
         <span>06 / CARE</span>
         <h2 id="care-title">The image is not<br/>the end of the project.</h2>
         <p>Plantupia already has a stronger idea than a static portfolio: installation becomes ownership, observation, maintenance and living history.</p>
       </header>
       <div className="ws-care-sequence">
         <ol>
           <li><span>01</span><strong>DESIGN</strong><p>Read the place and specify living material.</p></li>
           <li><span>02</span><strong>INSTALL</strong><p>Plant, vessel and location become a real handover.</p></li>
           <li><span>03</span><strong>RETURN</strong><p>Maintenance and observations continue after the photograph.</p></li>
           <li><span>04</span><strong>REMEMBER</strong><p>History records what changed, what was done and what belongs to the space.</p></li>
         </ol>
         <figure>
           <img src={asset("sector-commercial-brand.webp")} alt="Plantupia commercial interior proposal study"/>
           <figcaption><span>CONTINUITY</span><strong>Space → living material → care → time</strong></figcaption>
         </figure>
       </div>
     </section>

     <section className="ws-end">
       <span>WORKING SYSTEM / NOT A FINAL WEBSITE</span>
       <p>One world. Different behaviours. The source decides how it should be read.</p>
       <div><a href="/">BACK TO CURRENT EXPERIENCE</a><a href="/study/visual-direction">OPEN ARCHIVE INSTRUMENT ↗</a></div>
     </section>
   </main>
 </div>;
}
