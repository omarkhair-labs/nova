"use client";
import SiteIndex from "./SiteIndex";
import {emOptions} from "./em-sherif-case-data";
import "./space-explorer.css";
import "./em-sherif-case.css";

function PlantingOption({item,index}:{item:typeof emOptions[number];index:number}){
 return <figure className={"emc-wall-item emc-wall-item-"+(index+1)}>
  <img src={"/api/private-arrival/"+item.image} alt={"EM Sherif planting option "+item.id+", "+item.name} loading={index<2?"eager":"lazy"}/>
  <figcaption><span>{item.id}</span><div><p className="sx-kicker">{item.mood.toUpperCase()} / PROPOSAL P{item.page}</p><h3>{item.name}</h3><p>{item.description}</p><small>{item.latin} · White pebble</small></div></figcaption>
 </figure>
}

export default function EMSherifCaseStudy(){
 return <div className="sx-page emc-page"><a className="sx-skip" href="#emc-main">Skip to EM Sherif study</a>
 <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
 <main id="emc-main" className="emc-main"><div className="emc-crumb"><a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a><span>Hospitality / Custom vessel / Proposal</span></div>
 <section className="emc-hero" aria-labelledby="emc-title"><div><p className="sx-kicker">CASE STUDY 02 / EM SHERIF SEA CAFE</p><h1 id="emc-title">One vessel.<br/><em>Seven personalities.</em></h1></div><div><p>The custom navy planter stays constant while seven planting characters change height, density and the relationship to the sea view.</p><dl><div><dt>Object</dt><dd>Custom planter box</dd></div><div><dt>Choices</dt><dd>Seven planting options</dd></div><div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div></dl></div></section>

 <section className="emc-object" aria-labelledby="emc-object-title"><div><p className="sx-kicker">THE CONSTANT / P02</p><h2 id="emc-object-title">Design the vessel first.</h2><p>The proposal defines the planter as a spatial object before planting is selected. The seven visuals below hold that object still so the planting character can be compared directly.</p></div><dl><div><dt>Finish</dt><dd>Navy metal box, logo engraved on front</dd></div><div><dt>Mechanism</dt><dd>Folding mechanism imported from Turkey</dd></div><div><dt>Inside</dt><dd>Fiberglass inner planter</dd></div><div><dt>Drainage</dt><dd>Self-drainage system</dd></div><div><dt>Sizes</dt><dd>140 × 50 × 50 cm / 150 × 50 × 50 cm</dd></div></dl></section>

 <section className="emc-wall" aria-labelledby="emc-options-title"><div className="emc-section-head"><div><p className="sx-kicker">A—G / SAME VESSEL</p><h2 id="emc-options-title">Change the planting.<br/>Read the difference.</h2></div><p>No selector is needed to understand this study. All seven proposal directions stay visible together so height, density, leaf character and openness can be compared at a glance.</p></div><div className="emc-visual-field">{emOptions.map((item,index)=><PlantingOption key={item.id} item={item} index={index}/>)}</div></section>

 <section className="emc-glance" aria-labelledby="emc-glance-title"><div><p className="sx-kicker">AT A GLANCE / P10</p><h2 id="emc-glance-title">Seven visual characters.</h2></div><ol>{emOptions.map(o=><li key={o.id}><span>{o.id}</span><strong>{o.name}</strong><em>{o.mood}</em></li>)}</ol></section>
 <section className="emc-care" aria-labelledby="emc-care-title"><div><p className="sx-kicker">INCLUDED WITH EVERY OPTION / P11</p><h2 id="emc-care-title">Planted for the coast.</h2></div><ol><li><span>01</span>Supply &amp; installation</li><li><span>02</span>Plant selection for sea air</li><li><span>03</span>Scheduled maintenance</li><li><span>04</span>Plant warranty</li></ol></section>
 <section className="emc-source-note"><span>VISUAL SOURCE NOTE</span><p>All seven images are proposal directions for the same planter object. They are not presented as installed work or confirmed client selections.</p></section>
 <section className="emc-next"><p className="sx-kicker">NEXT STEP / P12</p><h2>Choose your planting.</h2><p>The proposal asks the client to select an option before the detailed quotation.</p><a className="sx-underlink" href="/start-a-space?space=gather">Discuss a hospitality project &#8599;</a></section>
 </main><footer className="sx-footer"><a href="/spaces/projects">Project archive</a><span>Plantupia / EM Sherif proposal study</span><a href="/spaces/hospitality">Hospitality &#8599;</a></footer></div>
}
