"use client";
import {useState} from "react";
import SiteIndex from "./SiteIndex";
import {mazalOptions} from "./mazal-case-data";
import "./space-explorer.css";
import "./mazal-case.css";

const families=["Tropical","Statement","Evergreen"] as const;

function FacadeOption({item}:{item:typeof mazalOptions[number]}){
 return <figure className="mz-wall-item">
  <img src={"/api/private-arrival/"+item.image} alt={"Mazal facade proposal, option "+item.id+", "+item.name} loading={item.id<=2?"eager":"lazy"}/>
  <figcaption><span>{String(item.id).padStart(2,"0")}</span><div><p className="sx-kicker">{item.family.toUpperCase()} / {item.mood.toUpperCase()} / P{item.page}</p><h3>{item.name}</h3><p>{item.description}</p><small>{item.plants.join(" · ")}</small></div></figcaption>
 </figure>
}

export default function MazalCaseStudy(){
 const[left,setLeft]=useState(1);const[right,setRight]=useState(7);
 const a=mazalOptions.find(o=>o.id===left)!;const b=mazalOptions.find(o=>o.id===right)!;
 return <div className="sx-page mz-page mz-world">
  <a className="sx-skip" href="#mz-main">Skip to Mazal study</a>
  <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
  <main id="mz-main" className="mz-main">
   <div className="mz-breadcrumb"><a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a><span>Hospitality / Facade planting / Proposal</span></div>
   <section className="mz-hero" aria-labelledby="mz-title"><div><p className="sx-kicker">CASE STUDY 01 / MAZAL RESTAURANT</p><h1 id="mz-title">Nine ways<br/>to <em>welcome.</em></h1></div><div><p>One facade. Nine planting designs across three visual families. The proposal studies how the first impression can shift from lush tropical planting to a minimal architectural line.</p><dl><div><dt>Site</dt><dd>Ivory Business Park</dd></div><div><dt>Proposal</dt><dd>November 2026</dd></div><div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div></dl></div></section>

   <section className="mz-wall" aria-labelledby="mz-wall-title">
    <div className="mz-section-head"><div><p className="sx-kicker">THE FACADE / ALL NINE DIRECTIONS</p><h2 id="mz-wall-title">Same architecture.<br/>Nine presences.</h2></div><p>The facade stays fixed so the planting can be read as the variable. All nine proposal visuals are shown before any comparison controls.</p></div>
    {families.map((family,index)=>{const items=mazalOptions.filter(o=>o.family===family);return <section className={"mz-family-field mz-family-field--"+items.length} key={family} aria-label={family+" facade directions"}><header><span>0{index+1}</span><div><p className="sx-kicker">{family.toUpperCase()}</p><h3>{family==="Tropical"?"Layered / lush":family==="Statement"?"Single-language / architectural":"Formal / persistent"}</h3></div><strong>{items.length} directions</strong></header><div>{items.map(item=><FacadeOption key={item.id} item={item}/>)}</div></section>})}
   </section>

   <section className="mz-compare" aria-labelledby="mz-compare-title"><div className="mz-section-head"><div><p className="sx-kicker">COMPARE / TWO DIRECTIONS</p><h2 id="mz-compare-title">Change the planting.<br/>Change the welcome.</h2></div><p>After seeing the full wall, isolate any two directions. This comparison changes only the proposal planting and adds no performance ranking.</p></div>
    <div className="mz-compare-controls"><label>Left<select value={left} onChange={e=>setLeft(Number(e.target.value))}>{mazalOptions.map(o=><option key={o.id} value={o.id}>0{o.id} / {o.name}</option>)}</select></label><button type="button" onClick={()=>{setLeft(right);setRight(left)}} aria-label="Swap comparison options">&#8646;</button><label>Right<select value={right} onChange={e=>setRight(Number(e.target.value))}>{mazalOptions.map(o=><option key={o.id} value={o.id}>0{o.id} / {o.name}</option>)}</select></label></div>
    <div className="mz-compare-grid"><Comparison item={a}/><Comparison item={b}/></div>
   </section>

   <section className="mz-care" aria-labelledby="mz-care-title"><div><p className="sx-kicker">INCLUDED WITH EVERY OPTION / P13</p><h2 id="mz-care-title">Planted, watered,<br/>always in shape.</h2></div><div><ol><li><span>01</span>Supply &amp; installation</li><li><span>02</span>Smart irrigation</li><li><span>03</span>Scheduled maintenance</li><li><span>04</span>Plant warranty</li></ol><a className="sx-underlink" href="/living-systems">Open Living Systems ↗</a></div></section>
   <section className="mz-source-note"><span>VISUAL SOURCE NOTE</span><p>All nine images are proposal visualizations. They are presented as design directions, not completed Mazal installations or confirmed client selections.</p></section>
   <section className="mz-next"><p className="sx-kicker">NEXT STEP / P14</p><h2>Choose your facade.</h2><p>The source proposal asks the client to select an option before a detailed quotation is sent.</p><a className="sx-underlink" href="/start-a-space?space=gather">Discuss a hospitality project &#8599;</a></section>
  </main><footer className="sx-footer"><a href="/spaces/projects">Project archive</a><span>Plantupia / Mazal proposal study</span><a href="/spaces/hospitality">Hospitality &#8599;</a></footer>
 </div>
}
function Comparison({item}:{item:typeof mazalOptions[number]}){return <article className="mz-compare-item" data-compare-option={item.id}><figure><img src={"/api/private-arrival/"+item.image} alt={"Mazal comparison option "+item.id+", "+item.name}/><figcaption>0{item.id} / {item.family}</figcaption></figure><div><p className="sx-kicker">{item.mood}</p><h3>{item.name}</h3><p>{item.description}</p><ul>{item.plants.map(x=><li key={x}>{x}</li>)}</ul></div></article>}
