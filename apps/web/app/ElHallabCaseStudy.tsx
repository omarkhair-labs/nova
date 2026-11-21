"use client";
import SiteIndex from "./SiteIndex";
import {hallabOptions,hallabRows} from "./hallab-case-data";
import "./space-explorer.css";
import "./hallab-case.css";

function TerraceOption({item,index}:{item:typeof hallabOptions[number];index:number}){
 return <article className={"eh-direction eh-direction-"+(index+1)}>
  <figure><img src={"/api/private-arrival/"+item.image} alt={"El Hallab terrace option "+item.id+", "+item.name} loading={index<2?"eager":"lazy"}/><figcaption><span>OPTION 0{item.id} / PROPOSAL P{item.page}</span><span>{item.character.toUpperCase()}</span></figcaption></figure>
  <div className="eh-direction-copy"><div><span className="eh-direction-no">0{item.id}</span><p className="sx-kicker">{item.latin}</p><h3>{item.name}</h3><p>{item.description}</p></div><dl><div><dt>Look</dt><dd>{item.look}</dd></div><div><dt>Flowering</dt><dd>{item.flowering}</dd></div><div><dt>Light</dt><dd>{item.light}</dd></div><div><dt>Water</dt><dd>{item.water}</dd></div><div><dt>Maintenance</dt><dd>{item.maintenance}</dd></div></dl></div>
 </article>
}

export default function ElHallabCaseStudy(){
 return <div className="sx-page eh-page eh-world"><a className="sx-skip" href="#eh-main">Skip to El Hallab study</a><header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
<main id="eh-main" className="eh-main"><div className="eh-crumb"><a className="sx-underlink" href="/spaces/projects">&#8592; Project archive</a><span>Hospitality / Terrace / Proposal</span></div>
<section className="eh-hero" aria-labelledby="eh-title"><div><p className="sx-kicker">CASE STUDY 03 / EL HALLAB TERRACE</p><h1 id="eh-title">One line.<br/><em>Five moods.</em></h1></div><div><p>The terrace planters are the first thing guests meet on arrival. The proposal keeps that edge constant and studies five planting directions for Cairo's outdoor conditions.</p><dl><div><dt>Context</dt><dd>Outdoor terrace</dd></div><div><dt>Choices</dt><dd>Five directions / mixable</dd></div><div><dt>Status</dt><dd>Design proposal / installation not verified</dd></div></dl></div></section>

<section className="eh-directions" aria-labelledby="eh-options-title"><div className="eh-section-head"><div><p className="sx-kicker">THE PLANTER LINE / ALL FIVE DIRECTIONS</p><h2 id="eh-options-title">Read the terrace<br/>before the matrix.</h2></div><p>Every direction stays visible at full scale first. The source's technical comparison follows only after the visual differences in colour, density and seasonality can be seen.</p></div>{hallabOptions.map((item,index)=><TerraceOption key={item.id} item={item} index={index}/>)}</section>

<section className="eh-matrix" aria-labelledby="eh-matrix-title"><div className="eh-section-head"><div><p className="sx-kicker">AT A GLANCE / P08</p><h2 id="eh-matrix-title">Now compare the five.</h2></div><p>This matrix transcribes the proposal's comparison categories. It does not add a ranking or recommendation.</p></div><div className="eh-table-wrap"><table><thead><tr><th>Measure</th>{hallabOptions.map(o=><th key={o.id}>0{o.id}<span>{o.name}</span></th>)}</tr></thead><tbody>{hallabRows.map(([label,key])=><tr key={key}><th>{label}</th>{hallabOptions.map(o=><td key={o.id}>{String(o[key])}</td>)}</tr>)}</tbody></table></div><p className="eh-tip"><strong>Mix &amp; match / source note:</strong> options can be combined — for example, Evergreen Classic along the full run with Colour Garden or Romantic Cascade accents beside the entrance.</p></section>
<section className="eh-process" aria-labelledby="eh-process-title"><div><p className="sx-kicker">NEXT STEPS / P09</p><h2 id="eh-process-title">From concept<br/>to terrace.</h2></div><div><ol><li><span>01</span><div><strong>Choose a direction</strong><p>Select one option, or a mix across the planter line.</p></div></li><li><span>02</span><div><strong>Site visit</strong><p>Confirm planter count, sizes and sun exposure on site.</p></div></li><li><span>03</span><div><strong>Detailed quotation</strong><p>Plants, soil, installation and maintenance, priced clearly.</p></div></li><li><span>04</span><div><strong>Installation &amp; care</strong><p>Delivery, planting and an ongoing Plantupia maintenance plan.</p></div></li></ol><a className="sx-underlink" href="/living-systems">Open Living Systems ↗</a></div></section>
<section className="eh-source-note"><span>VISUAL SOURCE NOTE</span><p>All five images are proposal directions for the same terrace edge. Seasonal and care notes are source-proposal descriptions, not performance guarantees for every site.</p></section>
<section className="eh-next"><p className="sx-kicker">YOUR TERRACE</p><h2>Which mood belongs here?</h2><a className="sx-underlink" href="/start-a-space?space=gather">Discuss a hospitality project &#8599;</a></section>
</main><footer className="sx-footer"><a href="/spaces/projects">Project archive</a><span>Plantupia / El Hallab terrace proposal</span><a href="/spaces/hospitality">Hospitality &#8599;</a></footer></div>
}
