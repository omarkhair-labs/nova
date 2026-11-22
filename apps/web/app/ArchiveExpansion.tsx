"use client";
import { useState } from "react";
import "./archive-expansion.css";

const asset=(file:string)=>"/api/private-arrival/"+file;
const source="DESIGN PROPOSAL / SOURCE PHOTO — NOT VERIFIED AS INSTALLED";

const pixelPairs=[
  {name:"Facade and planted columns",description:"A facade reads differently when planting is drawn into the lower edge or alongside structural columns.",items:[
    {file:"archive-pixels-p14.webp",label:"Facade / planted base",page:14},
    {file:"archive-pixels-p17.webp",label:"Columns / planting rhythm",page:17}
  ]},
  {name:"A threshold and its terrace",description:"A sequence of vessels gives way to a longer planting line as people move between architectural zones.",items:[
    {file:"archive-pixels-p20.webp",label:"Architectural threshold",page:20},
    {file:"archive-pixels-p22.webp",label:"Terrace planting line",page:22}
  ]},
  {name:"The edge of movement",description:"Two separate proposal frames explore a glazed frontage and a planted stair edge. They are not before-and-after evidence.",items:[
    {file:"archive-pixels-p25.webp",label:"Glazed frontage",page:25},
    {file:"archive-pixels-p27.webp",label:"Stair terrace",page:27}
  ]}
] as const;

export function PixelsArchiveExpansion(){
  const [active,setActive]=useState(0);
  const choices=[
    {file:"archive-pixels-p08.webp",name:"Planted foreground",page:8},
    {file:"archive-pixels-p11.webp",name:"Flowering tree",page:11}
  ] as const;
  return <section className="ax-pixels" aria-labelledby="ax-pixels-title">
    <div className="ax-head"><p className="ax-mark">PIXELS MALL / FURTHER SOURCE STUDIES</p><h2 id="ax-pixels-title">The place stays.<br/><em>The planting changes.</em></h2><p>More images from the same architectural proposal. These are alternative visual treatments and additional viewpoints, not eight separate completed projects.</p></div>
    <div className="ax-two-views">
      <div className="ax-two-views-image" aria-live="off">
        {choices.map((choice,i)=><img key={choice.file} src={asset(choice.file)} alt={i===active?"Pixels Mall proposed commercial promenade, "+choice.name:""} aria-hidden={i!==active} className={i===active?"is-current":""} loading="lazy" decoding="async"/>)}
        <span className="ax-image-count">01 / THE SAME APPROACH</span>
      </div>
      <div className="ax-two-views-reading">
        <p className="ax-mark">ALTERNATIVES / SOURCE P08 &amp; P11</p>
        <h3>One promenade.<br/>Two planting languages.</h3>
        <p>Compare the presence of foliage in the foreground with the additional flowering tree. Neither image documents an approved final choice.</p>
        <div className="ax-options" role="group" aria-label="Compare Pixels Mall proposal versions">
          {choices.map((choice,i)=><button key={choice.file} type="button" aria-pressed={i===active} onClick={()=>setActive(i)}><span>0{i+1}</span>{choice.name}</button>)}
        </div>
        <small>PIXELS MALL / SOURCE PAGE {choices[active].page} / PROPOSAL VISUAL</small>
      </div>
    </div>
    <div className="ax-pixel-essays">
      {pixelPairs.map((pair,i)=><article className="ax-pixel-essay" key={pair.name}>
        <div className="ax-pixel-essay-copy"><span className="ax-mark">SPATIAL NOTE / 0{i+2}</span><h3>{pair.name}</h3><p>{pair.description}</p></div>
        <div className="ax-pixel-double">{pair.items.map(item=><figure key={item.file}><img src={asset(item.file)} alt={"Pixels Mall proposal showing "+item.label} loading="lazy" decoding="async"/><figcaption>{item.label} / PROPOSAL P{item.page}</figcaption></figure>)}</div>
      </article>)}
    </div>
    <p className="ax-disclosure">{source}. Source: Pixels Mall / MT Architects proposal. Planting variants are not client approvals.</p>
  </section>;
}

const audiAdditional:Record<number,{file:string;page:number;label:string}>={
  1:{file:"archive-audi-p06.webp",page:6,label:"Another view of the lounge"},
  2:{file:"archive-audi-p08.webp",page:8,label:"A second view of the entrance"},
  4:{file:"archive-audi-p11.webp",page:11,label:"Another view between the displays"},
  5:{file:"archive-audi-p13.webp",page:13,label:"An alternative grouping of cylinders"},
  6:{file:"archive-audi-p15.webp",page:15,label:"A darker view of the brand wall"}
};
export function AudiAdditionalAngle({chapter}:{chapter:number}){
  const detail=audiAdditional[chapter];if(!detail)return null;
  return <div className="ax-audi-angle"><div className="ax-audi-angle-media"><img src={asset(detail.file)} alt={"Audi Cairo design proposal, "+detail.label} loading="lazy" decoding="async"/></div><p>SECOND VIEW / P{detail.page}<span>{detail.label}</span></p></div>;
}

const hospitalitySequences=[
 {id:"entrance",index:"01 / OUTDOOR APPROACH",heading:"One entrance. Three plant characters.",intro:"The same vessel line is shown with alternative planting. Compare foliage structure rather than treating these as separate sites.",options:[
  {file:"archive-hospitality-p09.webp",label:"Ficus",page:9},
  {file:"archive-hospitality-p10.webp",label:"Lady palm",page:10},
  {file:"archive-hospitality-p12.webp",label:"Softer foliage",page:12}]},
 {id:"terrace",index:"02 / PLANTER EDGE",heading:"What a change in colour can do.",intro:"The same outdoor planter condition is explored through silvery foliage and brighter flowering treatments. These are catalogue concepts, not documented seasonal outcomes.",options:[
  {file:"archive-hospitality-p14.webp",label:"Silver foliage",page:14},
  {file:"archive-hospitality-p15.webp",label:"Vibrant flowering",page:15},
  {file:"archive-hospitality-p16.webp",label:"Red flowering",page:16}]}
] as const;
function HospitalityComparison({study}:{study:(typeof hospitalitySequences)[number]}){
  const [active,setActive]=useState(0);
  return <article className="ax-hospitality-set">
    <div className="ax-hospitality-reading"><span className="ax-mark">{study.index}</span><h3>{study.heading}</h3><p>{study.intro}</p><div className="ax-options" role="group" aria-label={study.heading}>
      {study.options.map((option,i)=><button key={option.file} type="button" aria-pressed={i===active} onClick={()=>setActive(i)}><span>0{i+1}</span>{option.label}</button>)}
    </div><p className="ax-source-caption">HOSPITALITY GREENERY CATALOG / P{study.options[active].page} / PROPOSAL STUDY</p></div>
    <div className="ax-hospitality-visual">{study.options.map((option,i)=><img key={option.file} className={i===active?"is-current":""} aria-hidden={i!==active} alt={i===active?"Catalog proposal showing "+option.label+" planting in an outdoor vessel line":""} loading="lazy" decoding="async" src={asset(option.file)}/>)}</div>
  </article>;
}
export function HospitalitySourceAlternatives(){
  return <section className="ax-hospitality" aria-labelledby="ax-hospitality-title"><header className="ax-head"><span className="ax-mark">HOSPITALITY GREENERY CATALOG / CHOICES</span><h2 id="ax-hospitality-title">Same place.<br/>Different life.</h2><p>Six extra source photos are arranged as two actual comparisons: foliage at arrival, then planting at the outdoor edge.</p></header>
  {hospitalitySequences.map(study=><HospitalityComparison key={study.id} study={study}/>)}
  <p className="ax-disclosure">{source}. Material studies may use repeated or illustrative catalogue environments.</p>
  </section>;
}

const materialStudies=[
  {file:"archive-hospitality-p22.webp",label:"Sculptural foliage",question:"Form and silhouette",page:22},
  {file:"archive-hospitality-p25.webp",label:"Tree and vessel",question:"Scale in an interior",page:25},
  {file:"archive-hospitality-p26.webp",label:"Dracaena grouping",question:"The character of a leaf",page:26},
  {file:"archive-hospitality-p36.webp",label:"Metal planter line",question:"Privacy and boundaries",page:36}
] as const;
export function MaterialSourceStudies(){
  return <section className="ax-material" aria-labelledby="ax-material-title">
    <header className="ax-head"><span className="ax-mark">MATERIAL STUDIES / HOSPITALITY GREENERY CATALOG</span><h2 id="ax-material-title">Not everything<br/><em>is a product card.</em></h2><p>Different planting silhouettes, proportions and vessels change how a place is understood. These four catalogue illustrations are for material selection, not current prices, stock or checkout.</p></header>
    <div className="ax-material-sequence">{materialStudies.map((item,i)=><figure key={item.file} className={"ax-material-figure ax-material-figure-"+(i+1)}><img src={asset(item.file)} alt={item.label+", design catalogue illustration"} loading="lazy" decoding="async"/><figcaption><small>0{i+1} / SOURCE P{item.page}</small><strong>{item.label}</strong><span>{item.question}</span></figcaption></figure>)}</div>
    <div className="ax-material-bottom"><p>{source}. Catalogue illustrations are not published sellable SKUs.</p><a href="/start-a-space?topic=project">Discuss a planting specification ↗</a></div>
  </section>;
}
