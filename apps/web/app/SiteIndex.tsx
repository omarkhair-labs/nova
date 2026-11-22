"use client";

import { useEffect, useId, useRef, useState } from "react";
import { editorialSpaces } from "./space-contexts";
import Link from "next/link";
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
  { id: "systems", number: "04", href: "/living-systems", name: "Living Systems", line: "Care, records and what continues after installation." },
  { id: "about", number: "05", href: "/about", name: "About Plantupia", line: "Who we are, what we do, and the work behind it." },
  { id: "contact", number: "06", href: "/start-a-space", name: "Get in touch", line: "Start a project or talk about an existing space." },
] as const;

const studies = [
  { title:"Enter the place.", photo:5, asset:"arrival.webp", word:"THRESHOLD", credit:"Plantupia / arrival study" },
  { title:"Read the environment.", photo:0, asset:"world-spaces.webp", word:"SPACE", credit:"Pixels Mall / proposal study" },
  { title:"Compose with life.", photo:3, asset:"mazal-option-03.webp", word:"INTENTION", credit:"Mazal / proposal study" },
  { title:"Return and observe.", photo:4, asset:"sector-commercial-brand.webp", word:"TIME", credit:"Audi Cairo / proposal study" },
  { title:"Meet the practice.", photo:1, asset:"company-intro.webp", word:"PEOPLE", credit:"Plantupia / company study" },
  { title:"Begin a conversation.", photo:2, asset:"arrival-hero.webp", word:"POSSIBILITY", credit:"Plantupia / proposal study" },
] as const;

export default function SiteIndex({ current, tone = "light" }: { current: SiteSection | "shop" | null; tone?: "light" | "dark" }) {
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
      <Link className="si-shop-entry" href={current === "shop" ? "#shop-collection" : "/shop"} aria-current={current === "shop" ? "page" : undefined}>
        <span>SHOP</span><i aria-hidden="true">↗</i>
      </Link>
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
        <Link className="si-mobile-shop" href="/shop" onClick={dismiss}>
          <span><small>SHOP / COLLECTION</small><strong>Objects for living.</strong></span>
          <i aria-hidden="true">↗</i>
        </Link>
        <div className="si-body">
          <figure className="si-atlas" aria-label="Editorial chapter preview">
            <div className="si-atlas-frame">
              <img key={preview} className="si-atlas-photo" src={failed.includes(preview)?photo.fallback:"/api/private-arrival/"+study.asset} alt="" fetchPriority="low" onError={()=>setFailed(prev=>prev.includes(preview)?prev:[...prev,preview])}/>
              <div className="si-atlas-veil"/>
              <div className="si-atlas-meta"><span>CHAPTER / {item.number}</span><span>{study.word}</span></div>
              <div className="si-atlas-copy" key={item.id}><span>{item.name}</span><p>{study.title}</p></div>
            </div>
            <figcaption>{failed.includes(preview)?<>Fallback editorial reference / {photo.credit}</>:<>{study.credit} / design proposal or catalogue study.</>}</figcaption>
          </figure>
          <div className="si-reading">
            <div className="si-reading-head"><span>THE CHAPTERS</span><span>{currentIndex>=0?"YOU ARE IN / "+siteSections[currentIndex].number:"CHOOSE A CHAPTER"}</span></div>
            <nav aria-label="Explore the website" className="si-links" onPointerLeave={event=>{const focused=event.currentTarget.querySelector<HTMLAnchorElement>("a:focus"); const focusedIndex=siteSections.findIndex(section=>section.href===focused?.getAttribute("href"));setPreview(focusedIndex>=0?focusedIndex:initial);}}>
              {siteSections.map((entry,index) => (
                <Link key={entry.id} href={entry.href}
                  className={(entry.id===current?"si-active ":"")+(preview===index?"si-preview":"")}
                  aria-current={entry.id === current ? "page" : undefined}
                  onPointerEnter={event=>{if(event.pointerType==="mouse"||event.pointerType==="pen")setPreview(index);}}
                  onFocus={()=>setPreview(index)}
                  onClick={event=>{if(event.button===0&&!event.metaKey&&!event.ctrlKey&&!event.shiftKey&&!event.altKey)dismiss();}}>
                  <span className="si-number">{entry.number}</span>
                  <span className="si-link-text"><strong>{entry.name}</strong><small>{entry.line}</small></span>
                  <span className="si-link-arrow" aria-hidden="true">↗</span>
                </Link>
              ))}
            </nav>
          </div>
        </div>
        <div className="si-foot">
          <span>SPACE / DESIGN / TIME / PEOPLE</span>
          <span className="si-global-paths"><Link className="si-sidepath" href="/spaces/projects" onClick={dismiss}>CASE STUDIES / 06 &#8599;</Link><Link className="si-sidepath" href="/shop" onClick={dismiss}>SHOP / COLLECTION &#8599;</Link></span>
          <span>DESIGN / PLANTING / CONTINUING CARE</span>
        </div>
      </dialog>
    </div>
  );
}
