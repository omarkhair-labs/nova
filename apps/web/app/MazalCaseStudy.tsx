"use client";
import {useMemo,useState} from "react";
import SiteIndex from "./SiteIndex";
import {mazalOptions,mazalFamilies} from "./mazal-case-data";
import "./space-explorer.css";
import "./mazal-case.css";
type Family=typeof mazalFamilies[number];
export default function MazalCaseStudy(){
 const [family,setFamily]=useState<Family>("All");
 const [active,setActive]=useState(3);
 const [left,setLeft]=useState(1);
 const [right,setRight]=useState(7);
 const visible=useMemo(()=>family==="All"?mazalOptions:mazalOptions.filter(o=>o.family===family),[family]);
 const current=mazalOptions.find(o=>o.id===active)??mazalOptions[0];
 const a=mazalOptions.find(o=>o.id===left)!;const b=mazalOptions.find(o=>o.id===right)!;
 function chooseFamily(next:Family){setFamily(next);const first=next==="All"?mazalOptions[0]:mazalOptions.find(o=>o.family===next)!;setActive(first.id);}
 return <div className="sx-page mz-page">
  <a className="sx-skip" href="#mz-main">Skip to Mazal study</a>
  <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
  <main id="mz-main" className="mz-main">
   <div className="mz-breadcrumb"><a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a><span>Hospitality / Facade planting / Proposal</span></div>
   <section className="mz-hero" aria-labelledby="mz-title"><div><p className="sx-kicker">CASE STUDY 01 / MAZAL RESTAURANT</p><h1 id="mz-title">Nine ways<br/>to <em>welcome.</em></h1></div><div><p>One facade. Nine planting designs across three visual families. The proposal studies how the first impression can shift from lush tropical planting to a minimal architectural line.</p><dl><div><dt>Site</dt><dd>Ivory Business Park</dd></div><div><dt>Proposal</dt><dd>November 2026</dd></div><div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div></dl></div></section>
   <section aria-labelledby="mz-explore-title">
    <div className="mz-section-head"><div><p className="sx-kicker">THE FACADE / OPTION STUDY</p><h2 id="mz-explore-title">Same architecture.<br/>Different presence.</h2></div><p>Filter by design family, then examine each proposal at full scale. Names, descriptions and planting lists below follow the source proposal.</p></div>
    <div className="mz-families" role="group" aria-label="Filter facade design family">{mazalFamilies.map(f=><button key={f} type="button" aria-pressed={family===f} onClick={()=>chooseFamily(f)}>{f}<span>{String(f==="All"?9:mazalOptions.filter(o=>o.family===f).length).padStart(2,"0")}</span></button>)}</div>
    <div className="mz-options" role="group" aria-label="Choose a facade option">{visible.map(o=><button key={o.id} type="button" data-option={o.id} aria-pressed={current.id===o.id} onClick={()=>setActive(o.id)}><span>{String(o.id).padStart(2,"0")}</span><span><strong>{o.name}</strong><small>{o.mood}</small></span></button>)}</div>
    <article className="mz-current" key={current.id} data-active-option={current.id} aria-live="polite"><figure><img src={"/api/private-arrival/"+current.image} alt={"Mazal facade proposal, option "+current.id+", "+current.name}/><figcaption><span>OPTION {String(current.id).padStart(2,"0")} / {current.family.toUpperCase()}</span><span>Proposal p{current.page} / visualization</span></figcaption></figure><div className="mz-current-copy"><div><p className="sx-kicker">{current.mood.toUpperCase()} / {current.family.toUpperCase()}</p><h3>{current.name}</h3></div><div><p>{current.description}</p><p className="mz-label">Planting / finish</p><ul>{current.plants.map(x=><li key={x}>{x}</li>)}</ul></div></div></article>
   </section>
   <section className="mz-compare" aria-labelledby="mz-compare-title"><div className="mz-section-head"><div><p className="sx-kicker">COMPARE / TWO DIRECTIONS</p><h2 id="mz-compare-title">Change the planting.<br/>Change the welcome.</h2></div><p>Compare two options without changing the facade. This is a visual comparison of proposal directions, not a technical performance ranking.</p></div>
    <div className="mz-compare-controls"><label>Left<select value={left} onChange={e=>setLeft(Number(e.target.value))}>{mazalOptions.map(o=><option key={o.id} value={o.id}>0{o.id} / {o.name}</option>)}</select></label><button type="button" onClick={()=>{setLeft(right);setRight(left)}} aria-label="Swap comparison options">&#8646;</button><label>Right<select value={right} onChange={e=>setRight(Number(e.target.value))}>{mazalOptions.map(o=><option key={o.id} value={o.id}>0{o.id} / {o.name}</option>)}</select></label></div>
    <div className="mz-compare-grid"><Comparison item={a}/><Comparison item={b}/></div>
   </section>
   <section className="mz-glance" aria-labelledby="mz-glance-title"><div><p className="sx-kicker">AT A GLANCE / P12</p><h2 id="mz-glance-title">The nine moods.</h2></div><ol>{mazalOptions.map(o=><li key={o.id}><span>{String(o.id).padStart(2,"0")}</span><strong>{o.name}</strong><em>{o.mood}</em></li>)}</ol></section>
   <section className="mz-care" aria-labelledby="mz-care-title"><div><p className="sx-kicker">INCLUDED WITH EVERY OPTION / P13</p><h2 id="mz-care-title">Planted, watered,<br/>always in shape.</h2></div><ol><li><span>01</span>Supply &amp; installation</li><li><span>02</span>Smart irrigation</li><li><span>03</span>Scheduled maintenance</li><li><span>04</span>Plant warranty</li></ol></section>
   <section className="mz-next"><p className="sx-kicker">NEXT STEP / P14</p><h2>Choose your facade.</h2><p>The source proposal asks the client to select an option before a detailed quotation is sent.</p><a className="sx-underlink" href="/start-a-space?space=gather">Discuss a hospitality project &#8599;</a></section>
  </main><footer className="sx-footer"><a href="/spaces/projects">Project archive</a><span>Plantupia / Mazal proposal study</span><a href="/spaces/hospitality">Hospitality &#8599;</a></footer>
 </div>
}
function Comparison({item}:{item:typeof mazalOptions[number]}){return <article className="mz-compare-item" data-compare-option={item.id}><figure><img src={"/api/private-arrival/"+item.image} alt={"Mazal comparison option "+item.id+", "+item.name}/><figcaption>0{item.id} / {item.family}</figcaption></figure><div><p className="sx-kicker">{item.mood}</p><h3>{item.name}</h3><p>{item.description}</p><ul>{item.plants.map(x=><li key={x}>{x}</li>)}</ul></div></article>}
