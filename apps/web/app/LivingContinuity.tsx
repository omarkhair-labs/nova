"use client";
import {useRef,useState} from "react";
import SiteIndex from "./SiteIndex";
import {editorialSpaces} from "./space-contexts";
import "./practice-chapters.css";

const events=[
  {day:"01",label:"Handover",title:"A plant becomes part of a place.",text:"The handover connects the actual plant identity to its recipient. An authorized QR handover is reviewed and confirmed before the plant appears in My Plants.",state:"Identity & ownership",screen:"My Plants",item:"Example plant / PL-001",description:"A plant linked to a space after confirmed handover.",next:"Explore its care context."},
  {day:"08",label:"Observation",title:"Someone notices.",text:"A person records a real observation or care action. The record belongs to the plant and its place, rather than disappearing into an informal conversation.",state:"Recorded observation",screen:"Living History",item:"Leaves checked",description:"Example note: foliage observed during a visit.",next:"A personal follow-up can be planned."},
  {day:"15",label:"Follow-up",title:"An intention is not an outcome.",text:"A planned check-in can appear on the care schedule. The date is a reminder to look again, not evidence of completed care or an automatic watering prescription.",state:"Planned / not completed",screen:"Care Schedule",item:"Return to the plant",description:"Example: a check-in seven days after observation.",next:"Record what actually happened."},
  {day:"15",label:"Return",title:"The record continues.",text:"A person returns and records an observed outcome. In the private living history, the new record can be connected to the previous check-in.",state:"Recorded outcome",screen:"Living History",item:"Follow-up recorded",description:"Example note: condition reviewed; no diagnosis inferred.",next:"Keep caring, observing and returning."},
] as const;

const recordKinds=["Confirmed handover","Recorded observation","Planned check-in","Recorded outcome"] as const;
const recordNotes=["An identity linked after confirmation.","A note made by a person.","A future intention, not completed care.","A later observation, linked to the check-in."] as const;

function LivingSpecimen({active}:{active:number}){
  return <svg className="lc-specimen" viewBox="0 0 340 330" role="img" aria-labelledby="lc-specimen-title lc-specimen-description">
    <title id="lc-specimen-title">One illustrative plant identity across four moments</title>
    <desc id="lc-specimen-description">An original botanical line drawing held constant throughout the example. Numbers indicate the selected record, not simulated growth or plant health.</desc>
    <defs><pattern id="lc-paper-grid" patternUnits="userSpaceOnUse" width="20" height="20"><path d="M20 0H0V20" fill="none" stroke="#a9bba7" strokeWidth=".55"/></pattern></defs>
    <rect width="340" height="330" fill="#edf1e6"/>
    <rect x="18" y="18" width="304" height="294" fill="url(#lc-paper-grid)" stroke="#c2cdbd"/>
    <g fill="none" stroke="#67846a" strokeWidth=".8"><path d="M36 48V282M31 48H41M31 282H41M41 270H305"/><circle cx="173" cy="162" r="109" strokeDasharray="2 6"/></g>
    <g fill="#cbdac2" stroke="#365e41" strokeWidth="1.4" strokeLinejoin="round">
      <path d="M174 247C170 184 163 118 175 53" fill="none" strokeWidth="2"/>
      <path d="M172 190C117 191 78 157 76 116C124 116 153 138 172 190Z"/>
      <path d="M170 153C212 152 252 114 254 79C209 85 179 116 170 153Z"/>
      <path d="M173 117C137 104 121 70 130 37C162 48 181 74 173 117Z"/>
      <path d="M173 223C218 230 262 207 282 172C229 166 196 187 173 223Z"/>
      <path d="M175 79C195 64 207 43 203 27C181 33 170 58 175 79Z"/>
      <g fill="none" strokeWidth=".85"><path d="M172 190Q120 158 76 116M170 153Q212 113 254 79M173 117Q151 75 130 37M173 223Q225 193 282 172M175 79L203 27"/></g>
      <path d="M133 246H214L204 293H143Z" fill="#d8ded0"/>
      <path d="M130 246H217V255H130Z" fill="#edf1e6"/>
    </g>
    <g fontFamily="Arial,sans-serif" fontSize="9" letterSpacing="1" fill="#426349"><text x="46" y="34">SPECIMEN / 001</text><text x="46" y="299">IDENTITY HELD CONSTANT</text></g>
    <g transform="translate(264 274)"><rect width="43" height="28" fill="#28523a"/><text x="21.5" y="18" textAnchor="middle" fontSize="12" fill="#f6f8f0" fontFamily="monospace">{String(active+1).padStart(2,"0")}/4</text></g>
  </svg>
}

export default function LivingContinuity(){
  const [active,setActive]=useState(0);
  const [failed,setFailed]=useState(false);
  const entryButtons=useRef<Array<HTMLButtonElement|null>>([]);
  function advance(index:number){
    const next=(index+1)%events.length;
    setActive(next);
    entryButtons.current[next]?.focus();
  }
  const event=events[active],photo=editorialSpaces[1];
  return <div className="pc-page pc-living"><header className="pc-header"><a href="/" className="pc-brand">◕ Plantupia</a><SiteIndex current="systems" tone="dark"/><a href="/start-a-space" className="pc-contact">Get in touch ↗</a></header>
    <main>
      <section className="pc-living-hero"><div><p className="pc-kicker">04 / LIVING SYSTEMS</p><h1>Day one <em>is not the end.</em></h1><p>A living space is more than the day it is installed. Care, people and time are part of its story.</p></div><figure><img src={failed?photo.fallback:photo.src} alt={failed?"Generated concept illustration":photo.subject} onError={()=>setFailed(true)}/><figcaption>{failed?"Concept illustration":photo.credit} · Editorial reference, not a Plantupia project</figcaption></figure></section>
      <section className="pc-life-story lc-story" aria-label="An illustrated care journey">
        <div className="pc-life-heading"><div><p className="pc-kicker">THE RELATIONSHIP / AN EXAMPLE</p><h2>Follow one plant.</h2></div><p>One identity, four moments. The record gathers what people confirm, observe and plan. Explore each entry to see what is known at that moment.</p></div>
        <div className="lc-workspace">
          <aside className="lc-object" aria-label="Illustrative specimen and selected entry">
            <div className="lc-object-top"><span>THE LIVING RECORD</span><span>EXAMPLE ONLY</span></div>
            <LivingSpecimen active={active}/>
            <div className="lc-object-foot"><span>Example plant / PL-001</span><span>Not a health reading</span></div>
            <div className="lc-entry" key={active} aria-live="polite">
              <p className="pc-kicker">ENTRY {String(active+1).padStart(2,"0")} / {event.screen}</p>
              <h3>{event.item}</h3>
              <span className={"lc-state"+(active===2?" lc-state-planned":"")}>{event.state}</span>
              <p>{event.description}</p>
              <div className="lc-entry-next"><span>WHAT COMES NEXT</span><p>{event.next}</p></div>
            </div>
          </aside>
          <div className="lc-ledger">
            <div className="lc-ledger-head"><span>Timeline of the example plant</span><span>DAY / ENTRY</span></div>
            <ol className="lc-timeline" aria-label="Timeline of the example plant">
              {events.map((e,i)=><li key={e.label} data-record-state={i===2?"planned":"recorded"} className={(i===active?"lc-current ":"")+(i>active?"lc-later":"")}>
                <button type="button" aria-pressed={i===active} aria-controls={"lc-reading-"+i} className="lc-time-button" ref={node=>{entryButtons.current[i]=node;}} onClick={()=>setActive(i)}>
                  <span className="lc-day">DAY {e.day}</span><span className="lc-time-label"><strong>{e.label}</strong><small>{recordKinds[i]}</small></span><span className="lc-time-mark" aria-hidden="true">{String(i+1).padStart(2,"0")}</span>
                </button>
                <div className="lc-reading" id={"lc-reading-"+i} hidden={i!==active}>
                  <h3>{e.title}</h3><p>{e.text}</p>
                  <p className="lc-record-note">{recordNotes[i]}</p>
                  {i===2&&<p className="lc-planned-note">A scheduled check-in does not prove a visit happened.</p>}
                  {i===3&&<div className="lc-link-back"><span aria-hidden="true">↳</span><span>Connected to entry 03 / planned check-in</span></div>}
                  <button className="pc-link" type="button" onClick={()=>advance(i)}>{i===3?"Return to handover":"Read the next entry"} ↗</button>
                </div>
              </li>)}
            </ol>
            <div className="lc-key" aria-label="Record legend"><span><i/> Confirmed or recorded</span><span><i className="lc-key-planned"/> Planned, not completed</span></div>
          </div>
        </div>
        <p className="lc-disclosure">An illustrative sequence, not a live account, actual app screenshot, plant growth simulation or medical/botanical diagnosis. The same drawing represents the same example identity throughout.</p>
      </section>
      <section className="pc-principles"><p className="pc-kicker">WHAT WE WANT TO PRESERVE</p><h2>The difference between a plan and a memory.</h2><div><article><h3>Place</h3><p>Plants have a real environment and identity.</p></article><article><h3>Care</h3><p>Actions and observations are recorded by people.</p></article><article><h3>Time</h3><p>A follow-up date is distinct from its recorded outcome.</p></article></div><p className="pc-fine">The experience explains the QA product direction for handover, My Plants, care schedule and living history. No live sensors, customer information or automated diagnosis are represented.</p></section>
      <section className="pc-next"><div><p className="pc-kicker">KEEP THE CONVERSATION ALIVE</p><h2>Care for a place of your own.</h2></div><a href="/start-a-space?topic=care">Talk to Plantupia ↗</a></section>
    </main><footer className="pc-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/about">About us ↗</a></footer>
  </div>
}
