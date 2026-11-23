"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import "./mobile-photo-reader.css";

/**
 * The Pocket Field / Source-photo reading room.
 * This enhancement uses only existing case-study images and their native captions.
 * It never replaces project imagery, alters comparison controls, or takes over
 * ordinary navigation in the archive.
 */
type Plate = { src: string; alt: string; caption: string; name: string };
const caseSelector =
  ".mz-wall-item>img, .emc-wall-item>img, .eh-direction figure>img, " +
  ".au-chapter figure>img, .px-frame-media>img, .vl-option-media>img";

function chapterName(img: HTMLImageElement) {
  const page=img.closest<HTMLElement>(".mz-page,.emc-page,.eh-page,.au-page,.px-page,.vl-page");
  return page?.querySelector("h1")?.textContent?.replace(/\s+/g," ").trim() || "Plantupia / source study";
}

function collectPlates() {
  return Array.from(document.querySelectorAll<HTMLImageElement>(caseSelector))
    .filter(img=>img.getAttribute("src")?.startsWith("/api/private-arrival/"))
    .map(img=>({
      src:img.getAttribute("src") || "",
      alt:img.alt,
      caption:img.closest("figure")?.querySelector("figcaption")?.textContent?.replace(/\s+/g," ").trim()
        || img.alt,
      name:chapterName(img)
    }));
}

export default function MobilePhotoReader() {
  const [plates,setPlates]=useState<Plate[]>([]);
  const [index,setIndex]=useState(0);
  const [open,setOpen]=useState(false);
  const modal=useRef<HTMLDialogElement>(null);
  const originalFocus=useRef<HTMLElement|null>(null);
  const gesture=useRef<{x:number;y:number}|null>(null);
  const closeButton=useRef<HTMLButtonElement>(null);
  const active=plates[index];

  const dismiss=useCallback(()=>modal.current?.close(),[]);
  const advance=useCallback((direction:number)=>setIndex(i=>
    plates.length ? (i+direction+plates.length)%plates.length : 0
  ),[plates.length]);

  useEffect(()=>{
    if(!open)return;
    const dialog=modal.current;
    if(!dialog)return;
    if(!dialog.open)dialog.showModal();
    closeButton.current?.focus({preventScroll:true});
  },[open]);

  useEffect(()=>{
    function mobile(){return window.matchMedia("(max-width: 740px)").matches}
    function markImageTargets(){
      const enabled=mobile();
      for(const img of document.querySelectorAll<HTMLImageElement>(caseSelector)){
        if(!img.src.includes("/api/private-arrival/"))continue;
        if(enabled){
          img.dataset.pocketPhoto="true";
          img.setAttribute("role","button");
          img.tabIndex=0;
          img.setAttribute("aria-label","View full-size photo: "+img.alt);
        }else{
          delete img.dataset.pocketPhoto;
          img.removeAttribute("role");
          img.removeAttribute("tabindex");
          img.removeAttribute("aria-label");
        }
      }
    }
    function openImage(img:HTMLImageElement){
      if(!mobile())return;
      const images=Array.from(document.querySelectorAll<HTMLImageElement>(caseSelector))
        .filter(node=>node.getAttribute("src")?.startsWith("/api/private-arrival/"));
      const found=images.indexOf(img);
      if(found<0)return;
      const gallery=collectPlates();
      if(!gallery.length)return;
      originalFocus.current=img;
      setPlates(gallery);
      setIndex(found);
      setOpen(true);
    }
    function onClick(event:MouseEvent){
      const img=(event.target as Element)?.closest<HTMLImageElement>("img[data-pocket-photo]");
      if(!img)return;
      event.preventDefault();
      openImage(img);
    }
    function onKeyDown(event:KeyboardEvent){
      if(event.target instanceof HTMLImageElement && event.target.dataset.pocketPhoto==="true"
        && (event.key==="Enter" || event.key===" ")){
        event.preventDefault();
        openImage(event.target);
      }
    }
    const observer=new MutationObserver(records=>{
      if(records.some(record=>record.type==="childList"))markImageTargets();
    });
    observer.observe(document.body,{subtree:true,childList:true});
    const query=window.matchMedia("(max-width: 740px)");
    query.addEventListener("change",markImageTargets);
    document.addEventListener("click",onClick);
    document.addEventListener("keydown",onKeyDown);
    markImageTargets();
    return()=>{
      observer.disconnect();
      query.removeEventListener("change",markImageTargets);
      document.removeEventListener("click",onClick);
      document.removeEventListener("keydown",onKeyDown);
    };
  },[]);

  useEffect(()=>{
    if(!open)return;
    function onKeyDown(event:KeyboardEvent){
      if(event.key==="ArrowRight"){event.preventDefault();advance(1)}
      if(event.key==="ArrowLeft"){event.preventDefault();advance(-1)}
    }
    window.addEventListener("keydown",onKeyDown);
    return()=>window.removeEventListener("keydown",onKeyDown);
  },[open,advance]);

  return <dialog
    className="pf-photo-room"
    ref={modal}
    aria-label="Plantupia full-size study photographs"
    onClose={()=>{setOpen(false);originalFocus.current?.focus({preventScroll:true});}}
    onClick={event=>{if(event.target===event.currentTarget)dismiss();}}
  >
    {active&&<>
      <div className="pf-room-head">
        <div><small>SOURCE PHOTOGRAPH / FULL VIEW</small><strong>{active.name}</strong></div>
        <button type="button" className="pf-room-close" ref={closeButton} onClick={dismiss} aria-label="Close full-screen photo">CLOSE <span aria-hidden="true">×</span></button>
      </div>
      <div className="pf-room-frame"
        onTouchStart={event=>{const t=event.changedTouches[0];gesture.current={x:t.clientX,y:t.clientY};}}
        onTouchEnd={event=>{const from=gesture.current;gesture.current=null;if(!from)return;const t=event.changedTouches[0];const dx=t.clientX-from.x,dy=t.clientY-from.y;if(Math.abs(dx)>55 && Math.abs(dx)>Math.abs(dy)*1.6)advance(dx<0?1:-1);}}
      >
        {/* The source image is never cropped, recoloured or reconstructed. */}
        <img src={active.src} alt={active.alt} key={active.src} decoding="async"/>
      </div>
      <div className="pf-room-detail">
        <div className="pf-room-progress"><span>{String(index+1).padStart(2,"0")} / {String(plates.length).padStart(2,"0")}</span><span>SWIPE TO STUDY ↔</span></div>
        <p>{active.caption}</p>
        <nav aria-label="Full-size source photograph navigation">
          <button type="button" onClick={()=>advance(-1)} aria-label="Previous photograph">← <span>PREVIOUS</span></button>
          <button type="button" onClick={()=>advance(1)} aria-label="Next photograph"><span>NEXT</span> →</button>
        </nav>
      </div>
    </>}
  </dialog>;
}
