"use client";
import {useState} from "react";
import SiteIndex from "./SiteIndex";
import {editorialSpaces} from "./space-contexts";
import "./practice-chapters.css";

const stages=[
  {name:"Read",line:"The conditions of a place.",text:"Start with the architecture, not a plant list. Notice where daylight enters, how people move and which areas can be maintained.",note:"Daylight and movement",output:"A site understanding",photo:5},
  {name:"Compose",line:"Find a relationship.",text:"Explore the placement and scale of living material while preserving clear circulation. Plant choices depend on the conditions of the actual site.",note:"Planting zones and clear routes",output:"A planting concept",photo:0},
  {name:"Introduce",line:"Make the intention physical.",text:"Consider the route for installation, container positions and access. The final solution requires site review, material choices and practical coordination.",note:"Placement and access",output:"An installation plan",photo:1},
  {name:"Return",line:"Design for the next visit.",text:"A living space changes. Leave room for maintenance, observation and the people who will look after the planting.",note:"Continuing care access",output:"A care approach",photo:4},
] as const;
const zones=[{x:164,y:147,r:43},{x:238,y:149,r:28},{x:530,y:175,r:43},{x:544,y:331,r:35},{x:166,y:336,r:28}];

function Plan({stage}:{stage:number}){
  return <svg viewBox="0 0 730 490" role="img" aria-labelledby="plan-title plan-desc" className="pc-plan">
    <title id="plan-title">Illustrative architectural plan: {stages[stage].name}</title>
    <desc id="plan-desc">A hypothetical rectangular interior with northern windows, a central clear route and example planting placements. Layers change with the selected design phase. This is not a surveyed plan.</desc>
    <defs><pattern id="pc-grid" width="24" height="24" patternUnits="userSpaceOnUse"><path d="M24 0H0V24" fill="none" stroke="#c1c9bc" strokeWidth=".5"/></pattern><pattern id="pc-light" width="13" height="13" patternUnits="userSpaceOnUse" patternTransform="rotate(28)"><path d="M0 0V13" stroke="#b69d55" strokeWidth="2"/></pattern></defs>
    <rect width="730" height="490" fill="#f0f1e8"/><rect x="28" y="28" width="674" height="434" fill="url(#pc-grid)"/>
    <g fill="none" stroke="#9eae9e" strokeWidth="1"><path d="M110 75V50H610V75M110 55H610M75 95H52V395H75M58 95V395"/><path d="M105 50L115 60M605 50L615 60M47 90L63 100M47 390L63 400"/></g>
    <g fill="#546d59" fontSize="11" fontFamily="monospace" letterSpacing="1"><text x="360" y="45" textAnchor="middle">CONCEPT / NOT TO SCALE</text><text x="48" y="245" transform="rotate(-90 48 245)" textAnchor="middle">ILLUSTRATIVE INTERIOR</text><text x="658" y="67" textAnchor="middle">N</text><path d="M658 93V72l-5 9h10z" fill="#546d59"/></g>
    <rect x="108" y="95" width="502" height="300" fill="#f8f8f1" stroke="#263d30" strokeWidth="3"/>
    <g stroke="#668779" strokeWidth="5"><path d="M143 95H272M300 95H429M457 95H578"/></g>
    <g stroke="#aebbb0" strokeWidth="1" fill="none"><path d="M143 87H272M300 87H429M457 87H578"/><rect x="287" y="147" width="155" height="60" rx="2"/><rect x="306" y="225" width="116" height="60" rx="30"/><path d="M108 266H152V305H108M610 238H566V286H610"/><path d="M330 395H401" stroke="#f8f8f1" strokeWidth="7"/><path d="M330 395V324A71 71 0 0 1 401 395" stroke="#627a68"/></g>
    <g className={stage===0?"pc-layer pc-on":"pc-layer"}><path d="M143 101H578L500 225H218Z" fill="url(#pc-light)" opacity=".48"/><path d="M366 381V306L475 264V215" stroke="#526e60" strokeWidth="2" strokeDasharray="6 7" fill="none"/><circle cx="366" cy="381" r="5" fill="#526e60"/><text x="362" y="126" textAnchor="middle" fill="#705f34" fontSize="13">DAYLIGHT</text><text x="425" y="311" fill="#425d4b" fontSize="12">CIRCULATION</text></g>
    <g className={stage>=1?"pc-layer pc-on":"pc-layer"}>{zones.map(z=><g key={z.x}><circle cx={z.x} cy={z.y} r={z.r} fill="#b9cfab" stroke="#476c4d" strokeWidth="1.3"/><circle cx={z.x} cy={z.y} r={z.r*.66} fill="none" stroke="#789977" strokeDasharray="3 3"/><path d={"M"+(z.x-z.r*.45)+" "+z.y+"h"+z.r*.9+"m"+(-z.r*.45)+" "+(-z.r*.45)+"v"+z.r*.9} stroke="#557755" fill="none"/></g>)}</g>
    <g className={stage===2?"pc-layer pc-on":"pc-layer"} stroke="#9b6e3d" fill="none" strokeWidth="2"><path d="M365 382V311H239V185M365 311H497V175" strokeDasharray="7 5"/><circle cx="239" cy="185" r="7"/><circle cx="497" cy="175" r="7"/><text x="371" y="356" fontSize="12" stroke="none" fill="#875d31">INSTALL ACCESS</text></g>
    <g className={stage===3?"pc-layer pc-on":"pc-layer"} fill="none" stroke="#427d68" strokeWidth="2"><path d="M365 378V315H218V210H495V315H565" strokeDasharray="6 6"/>{zones.map(z=><circle key={z.x} cx={z.x} cy={z.y} r={z.r+9} strokeDasharray="4 5"/>)}<text x="375" y="343" stroke="none" fill="#326c57" fontSize="12">CARE ACCESS</text></g>
    <g fill="#546d59" fontFamily="monospace" fontSize="11"><text x="108" y="431">01 / CONCEPT STUDY</text><text x="610" y="431" textAnchor="end">LAYER {String(stage+1).padStart(2,"0")} / 04</text></g>
  </svg>
}

export default function StudioAtelier(){
  const [active,setActive]=useState(0);const [failed,setFailed]=useState<number[]>([]);
  const step=stages[active],photo=editorialSpaces[step.photo];
  return <div className="pc-page pc-studio"><header className="pc-header"><a href="/" className="pc-brand">◕ Plantupia</a><SiteIndex current="studio"/><a href="/start-a-space" className="pc-contact">Get in touch ↗</a></header>
    <main>
      <section className="pc-intro"><div><p className="pc-kicker">03 / THE STUDIO</p><h1>Designed <em>to belong.</em></h1></div><p>A thoughtful relationship with the place, from the first observation to the next visit.</p></section>
      <section className="pc-workspace" aria-label="Explore the design process">
        <div className="pc-sheet"><div className="pc-sheet-head"><span>THE DESIGN TABLE</span><span>CONCEPT / 01—04</span></div><Plan stage={active}/><div className="pc-sheet-foot"><span>DESIGN DIAGRAM / CONCEPT, NOT A CLIENT DRAWING</span><span>{step.note}</span></div></div>
        <div className="pc-controls">
          <p className="pc-kicker">A WAY OF WORKING</p>
          <div className="pc-stage-list" role="group" aria-label="Design layers">{stages.map((item,i)=><button key={item.name} aria-pressed={active===i} type="button" onClick={()=>setActive(i)}><span>0{i+1}</span><strong>{item.name}</strong><span aria-hidden="true">↗</span></button>)}</div>
          <div className="pc-reading" key={active} aria-live="polite"><h2>{step.line}</h2><p>{step.text}</p><small>DESIGN OUTPUT / {step.output}</small></div>
        </div>
      </section>
      <section className="pc-material"><figure><img key={step.photo} src={failed.includes(step.photo)?photo.fallback:photo.src} alt={failed.includes(step.photo)?"Generated architectural concept":photo.subject} loading="lazy" onError={()=>setFailed(prev=>prev.includes(step.photo)?prev:[...prev,step.photo])}/><figcaption>{failed.includes(step.photo)?"Concept illustration":photo.credit} · Editorial reference, not client work</figcaption></figure><div><p className="pc-kicker">FROM THE DRAWING TO THE PLACE</p><h2>Life is not a finishing detail.</h2><p>Planting, placement and care belong in the conversation from the beginning. Conditions differ across homes, hotels, workplaces and industrial sites.</p><a className="pc-link" href="/spaces">Explore the environments ↗</a></div></section>
      <section className="pc-next"><div><p className="pc-kicker">WHAT HAPPENS NEXT / 04</p><h2>Beyond installation.</h2></div><a href="/living-systems">Explore Living Systems ↗</a></section>
    </main><footer className="pc-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/start-a-space">Contact ↗</a></footer>
  </div>
}
