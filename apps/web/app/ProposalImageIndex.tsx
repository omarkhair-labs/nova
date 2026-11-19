"use client";
import "./proposal-image-index.css";
type Item={id:string|number;title:string;image:string;meta?:string};
export default function ProposalImageIndex({items,active,onSelect,label="Proposal visual archive"}:{items:readonly Item[];active:string|number;onSelect:(id:string|number)=>void;label?:string}){
 return <section className="pvi" aria-label={label}><div className="pvi-head"><p>VISUAL ARCHIVE / {String(items.length).padStart(2,"0")} FRAMES</p><span>Open any image to continue the study.</span></div><div className="pvi-grid" data-count={items.length}>{items.map((item,i)=><button type="button" key={String(item.id)} aria-pressed={String(active)===String(item.id)} onClick={()=>onSelect(item.id)} className={"pvi-item pvi-item-"+((i%7)+1)}><img src={"/api/private-arrival/"+item.image} alt={item.title} loading={i<3?"eager":"lazy"}/><span><b>{String(i+1).padStart(2,"0")}</b><strong>{item.title}</strong>{item.meta&&<small>{item.meta}</small>}</span></button>)}</div></section>
}
