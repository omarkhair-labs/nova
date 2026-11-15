"use client";
import {useState} from "react";
import SiteIndex from "./SiteIndex";
import {editorialSpaces} from "./space-contexts";
import "./practice-chapters.css";

const events=[
  {day:"01",label:"Handover",title:"A plant becomes part of a place.",text:"The handover connects the actual plant identity to its recipient. An authorized QR handover is reviewed and confirmed before the plant appears in My Plants.",state:"Identity & ownership",screen:"My Plants",item:"Example plant / PL-001",description:"A plant linked to a space after confirmed handover.",next:"Explore its care context."},
  {day:"08",label:"Observation",title:"Someone notices.",text:"A person records a real observation or care action. The record belongs to the plant and its place, rather than disappearing into an informal conversation.",state:"Recorded observation",screen:"Living History",item:"Leaves checked",description:"Example note: foliage observed during a visit.",next:"A personal follow-up can be planned."},
  {day:"15",label:"Follow-up",title:"An intention is not an outcome.",text:"A planned check-in can appear on the care schedule. The date is a reminder to look again, not evidence of completed care or an automatic watering prescription.",state:"Planned / not completed",screen:"Care Schedule",item:"Return to the plant",description:"Example: a check-in seven days after observation.",next:"Record what actually happened."},
  {day:"15",label:"Return",title:"The record continues.",text:"A person returns and records an observed outcome. In the private living history, the new record can be connected to the previous check-in.",state:"Recorded outcome",screen:"Living History",item:"Follow-up recorded",description:"Example note: condition reviewed; no diagnosis inferred.",next:"Keep caring, observing and returning."},
] as const;

export default function LivingContinuity(){
  const [active,setActive]=useState(0);
  const [failed,setFailed]=useState(false);
  const event=events[active],photo=editorialSpaces[1];
  return <div className="pc-page pc-living"><header className="pc-header"><a href="/" className="pc-brand">◕ Plantupia</a><SiteIndex current="systems" tone="dark"/><a href="/start-a-space" className="pc-contact">Get in touch ↗</a></header>
    <main>
      <section className="pc-living-hero"><div><p className="pc-kicker">04 / LIVING SYSTEMS</p><h1>Day one <em>is not the end.</em></h1><p>A living space is more than the day it is installed. Care, people and time are part of its story.</p></div><figure><img src={failed?photo.fallback:photo.src} alt={failed?"Generated concept illustration":photo.subject} onError={()=>setFailed(true)}/><figcaption>{failed?"Concept illustration":photo.credit} · Editorial reference, not a Plantupia project</figcaption></figure></section>
      <section className="pc-life-story" aria-label="An illustrated care journey">
        <div className="pc-life-heading"><div><p className="pc-kicker">THE RELATIONSHIP / AN EXAMPLE</p><h2>Follow one plant.</h2></div><p>Four moments in an illustrative record. Explore them in any order.</p></div>
        <div className="pc-life-steps" role="group" aria-label="Timeline of the example plant">{events.map((e,i)=><button key={e.label} type="button" aria-pressed={i===active} onClick={()=>setActive(i)}><span>DAY {e.day}</span><strong>{e.label}</strong><i aria-hidden="true"/></button>)}</div>
        <div className="pc-life-detail">
          <div key={active} className="pc-life-copy" aria-live="polite"><p className="pc-kicker">{event.state}</p><h3>{event.title}</h3><p>{event.text}</p><button className="pc-link pc-next-step" type="button" onClick={()=>setActive((active+1)%events.length)}>{active===3?"Return to handover":"Next moment"} ↗</button></div>
          <div className="pc-record" aria-label="Illustrative private plant record">
            <div className="pc-record-head"><span>PLANTUPIA / APP CONCEPT</span><span>EXAMPLE ONLY</span></div>
            <div key={active} className="pc-record-body"><p className="pc-kicker">{event.screen}</p><h3>{event.item}</h3><span className="pc-record-status"><i/> {event.state}</span><p>{event.description}</p><div className="pc-record-next"><span>WHAT COMES NEXT</span><p>{event.next}</p></div></div>
            <div className="pc-record-foot">Illustrative content, not a live account, real plant record or actual app screen.</div>
          </div>
        </div>
      </section>
      <section className="pc-principles"><p className="pc-kicker">WHAT WE WANT TO PRESERVE</p><h2>The difference between a plan and a memory.</h2><div><article><h3>Place</h3><p>Plants have a real environment and identity.</p></article><article><h3>Care</h3><p>Actions and observations are recorded by people.</p></article><article><h3>Time</h3><p>A follow-up date is distinct from its recorded outcome.</p></article></div><p className="pc-fine">The experience explains the QA product direction for handover, My Plants, care schedule and living history. No live sensors, customer information or automated diagnosis are represented.</p></section>
      <section className="pc-next"><div><p className="pc-kicker">KEEP THE CONVERSATION ALIVE</p><h2>Care for a place of your own.</h2></div><a href="/start-a-space?topic=care">Talk to Plantupia ↗</a></section>
    </main><footer className="pc-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/about">About us ↗</a></footer>
  </div>
}
