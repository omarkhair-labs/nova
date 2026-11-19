"use client";

import { useState } from "react";
import ArrivalOpening from "./ArrivalOpening";
import WorldThreshold from "./WorldThreshold";
import { CompanyIntroduction } from "./CompanyStory";
import ShopBridge from "./ShopBridge";
import ChapterThreshold from "./ChapterThreshold";
import "./private-arrival.css";

const options = [
  {image:"terrace-lavender.webp", label:"Lavender Bloom", line:"Silver-grey foliage and seasonal lavender flowers.",alt:"Terrace planter line with purple flowering foliage."},
  {image:"terrace-evergreen.webp",label:"Evergreen Classic",line:"A quieter green boundary with a more consistent appearance.",alt:"The same terrace with an evergreen planter composition."}
] as const;
const source = (name:string) => "/api/private-arrival/" + name;
export default function PrivateArrival({ exactImage }: { exactImage: boolean }){
  const [variant,setVariant]=useState(0);
  const [failed,setFailed]=useState<string[]>([]);
  const option=options[variant];
  function fail(name:string){setFailed(items=>items.includes(name)?items:[...items,name]);}
  return <div className="pa-world">
    <main id="pa-main"><ArrivalOpening exactImage={exactImage}/>
      <section className="pa-study" id="pa-study" aria-labelledby="pa-study-title">
        <div className="pa-study-head"><p className="pa-micro">A STUDY IN DESIGN / TWO DIRECTIONS</p><h2 id="pa-study-title">One place.<br/><em>Different living moods.</em></h2><p>Planting design is a decision about atmosphere, not just a list of species. Compare two proposed directions for the same terrace.</p></div>
        <div className="pa-choice">
          <figure>{!failed.includes(option.image)?<img key={option.image} src={source(option.image)} alt={option.alt} onError={()=>fail(option.image)} loading="lazy"/>:<div className="pa-missing">Private visual not installed.</div>}<figcaption>Planting concept · not evidence of a completed installation</figcaption></figure>
          <div className="pa-choice-control">
            {options.map((item,i)=><button key={item.image} type="button" onClick={()=>setVariant(i)} aria-pressed={variant===i}><span className="pa-micro">0{i+1}</span><strong>{item.label}</strong><span>{item.line}</span></button>)}
          </div>
        </div>
        <a className="pa-link" href="/spaces">Explore spaces <span aria-hidden="true">↗</span></a>
      </section>
    </main>
    <WorldThreshold/><CompanyIntroduction/><ShopBridge/><ChapterThreshold from="experience"/>
    <footer className="pa-review">Private design review · supplied proposals and catalogue imagery · publication and installation status not yet verified.</footer>
  </div>;
}
