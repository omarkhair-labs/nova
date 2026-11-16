"use client";

import { useEffect, useId, useRef, useState } from "react";
import { editorialSpaces } from "./space-contexts";
import "./site-index.css";

export type SiteSection = "experience" | "spaces" | "studio" | "systems" | "about" | "contact";

export const siteSections: ReadonlyArray<{
  id: SiteSection;
  number: string;
  href: string;
  name: string;
  line: string;
}> = [
  { id: "experience", number: "01", href: "/", name: "The Experience", line: "Enter the world where space and life meet." },
  { id: "spaces", number: "02", href: "/spaces", name: "Spaces", line: "Explore the questions behind different environments." },
  { id: "studio", number: "03", href: "/studio", name: "The Studio", line: "Understand how our thinking becomes a living place." },
  { id: "systems", number: "04", href: "/living-systems", name: "Living Systems", line: "Discover what continues after installation." },
  { id: "about", number: "05", href: "/about", name: "About Plantupia", line: "Who we are, what we do, and the work behind it." },
  { id: "contact", number: "06", href: "/start-a-space", name: "Get in touch", line: "Start a project or talk about an existing space." },
] as const;

const studies = [
  { title:"Enter the place.", photo:5, word:"THRESHOLD", alignment:"center" },
  { title:"Read the environment.", photo:0, word:"SPACE", alignment:"center" },
  { title:"Compose with life.", photo:3, word:"INTENTION", alignment:"center" },
  { title:"Return and observe.", photo:4, word:"TIME", alignment:"center" },
  { title:"Meet the practice.", photo:1, word:"PEOPLE", alignment:"center" },
  { title:"Begin a conversation.", photo:2, word:"POSSIBILITY", alignment:"center" },
] as const;

// A common drawing vocabulary; not an actual floor plan or business location.
function IndexStudy({active}:{active:number}){
  return <svg className="si-study-drawing" viewBox="0 0 320 190" aria-hidden="true" focusable="false">
    <g stroke="currentColor" fill="none" strokeWidth=".85">
      <path d="M12 170H308M160 8V181" opacity=".45" strokeDasharray="2 5"/>
      <path d="M40 165V52L160 19L280 52V165M40 52L160 86L280 52M160 86V165" opacity=".7"/>
      <path d="M78 165V76L160 52L242 76V165" opacity=".38"/>
      {active===0&&<path d="M125 165V95L160 85L195 95V165M125 165L160 147L195 165" strokeWidth="1.4"/>}
      {active===1&&<g strokeWidth="1.15"><path d="M40 165L160 124L280 165M78 165L160 143L242 165"/><circle cx="160" cy="124" r="5" fill="currentColor"/></g>}
      {active===2&&<g strokeWidth="1.15"><path d="M94 98H226V153H94Z"/>{[0,1,2].map(i=><circle cx={116+i*44} cy="126" r="16" key={i} strokeDasharray="2 3"/>)}</g>}
      {active===3&&<g strokeWidth="1.2"><path d="M160 165Q149 109 164 46M156 132Q104 124 102 87Q143 90 156 132M159 94Q211 84 215 56Q176 54 159 94M162 65Q140 49 145 25Q166 34 162 65"/><circle cx="160" cy="100" r="66" opacity=".55" strokeDasharray="2 5"/></g>}
      {active===4&&<g strokeWidth="1.2"><path d="M83 143L128 98L160 127L202 71L243 104"/>{[[83,143],[128,98],[160,127],[202,71],[243,104]].map(([x,y],i)=><circle key={i} cx={x} cy={y} r="5"/>)}</g>}
      {active===5&&<g strokeWidth="1.3"><path d="M114 165V89L160 76L206 89V165M114 89L160 103L206 89M160 103V165M114 165L160 145L206 165"/><path d="M160 76L206 89V165L160 145Z" fill="currentColor" fillOpacity=".13"/></g>}
    </g>
  </svg>
}

export default function SiteIndex({ current, tone = "light" }: { current: SiteSection | null; tone?: "light" | "dark" }) {
  const [open, setOpen] = useState(false);
  const [preview, setPreview] = useState(0);
  const [failed, setFailed] = useState<number[]>([]);
  const trigger = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDialogElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  const titleId = useId();
  const currentIndex = siteSections.findIndex(item => item.id === current);
  const initial = Math.max(0, currentIndex);
  const item = siteSections[preview];
  const study = studies[preview];
  const photo = editorialSpaces[study.photo];

  function reveal(){
    if(!panel.current || panel.current.open) return;
    setPreview(initial);
    panel.current.showModal();
    setOpen(true);
    close.current?.focus();
  }

  function dismiss(){
    if(panel.current?.open) panel.current.close();
  }

  useEffect(()=>{
    if(!open)return;
    const previous=document.body.style.overflow;
    document.body.style.overflow="hidden";
    return()=>{document.body.style.overflow=previous;};
  },[open]);

  return (
    <div className={"si-root si-tone-" + tone}>
      <button type="button" className="si-trigger" ref={trigger} aria-haspopup="dialog"
        aria-expanded={open} aria-label="Open Plantupia world index" onClick={reveal}>
        <span aria-hidden="true" className="si-plus"/>
      </button>
      <dialog className="si-overlay" ref={panel} aria-labelledby={titleId}
        onClose={()=>{setOpen(false);trigger.current?.focus({preventScroll:true});}}>
        <div className="si-top">
          <span>PLANTUPIA / THE WORLD INDEX</span>
          <h2 id={titleId}>Six chapters. One living world.</h2>
          <button type="button" ref={close} onClick={dismiss} className="si-close" aria-label="Close world index">
            <span>CLOSE</span><i aria-hidden="true"/>
          </button>
        </div>
        <div className="si-body">
          <figure className="si-atlas" aria-label="Editorial chapter preview">
            <div className="si-atlas-frame">
              {open&&<img key={preview} className="si-atlas-photo" src={failed.includes(preview)?photo.fallback:photo.src} alt="" onError={()=>setFailed(prev=>prev.includes(preview)?prev:[...prev,preview])}/>}
              <div className="si-atlas-veil"/>
              <div className="si-atlas-meta"><span>CHAPTER / {item.number}</span><span>{study.word}</span></div>
              <div className="si-atlas-art"><IndexStudy active={preview}/></div>
              <div className="si-atlas-copy" key={item.id}><span>{item.name}</span><p>{study.title}</p></div>
            </div>
            <figcaption>{failed.includes(preview)?"Generated concept illustration":<a href={photo.source} target="_blank" rel="noreferrer">{photo.credit} ↗</a>} · Editorial reference, not client work.</figcaption>
          </figure>
          <div className="si-reading">
            <div className="si-reading-head"><span>THE CHAPTERS</span><span>{currentIndex>=0?"YOU ARE IN / "+siteSections[currentIndex].number:"CHOOSE A CHAPTER"}</span></div>
            <nav aria-label="Explore the website" className="si-links" onPointerLeave={event=>{const focused=event.currentTarget.querySelector<HTMLAnchorElement>("a:focus"); const focusedIndex=siteSections.findIndex(section=>section.href===focused?.getAttribute("href"));setPreview(focusedIndex>=0?focusedIndex:initial);}}>
              {siteSections.map((entry,index) => (
                <a key={entry.id} href={entry.href}
                  className={(entry.id===current?"si-active ":"")+(preview===index?"si-preview":"")}
                  aria-current={entry.id === current ? "page" : undefined}
                  onPointerEnter={event=>{if(event.pointerType==="mouse"||event.pointerType==="pen")setPreview(index);}}
                  onFocus={()=>setPreview(index)}
                  onClick={event=>{if(event.button===0&&!event.metaKey&&!event.ctrlKey&&!event.shiftKey&&!event.altKey)dismiss();}}>
                  <span className="si-number">{entry.number}</span>
                  <span className="si-link-text"><strong>{entry.name}</strong><small>{entry.line}</small></span>
                  <span className="si-link-arrow" aria-hidden="true">↗</span>
                </a>
              ))}
            </nav>
          </div>
        </div>
        <div className="si-foot">
          <span>SPACE / DESIGN / TIME / PEOPLE</span>
          <span>DESIGN STUDY · NOT THE OFFICIAL LIVE SITE</span>
        </div>
      </dialog>
    </div>
  );
}
