"use client";
import { useEffect, useRef, useState } from "react";
import "./arrival-opening.css";

const destinations = [
  { href: "/spaces", label: "Spaces" },
  { href: "/studio", label: "Studio" },
  { href: "/living-systems", label: "Living Systems" },
  { href: "/about", label: "About" },
  { href: "/shop", label: "Shop" },
] as const;

export default function ArrivalOpening({ exactImage }: { exactImage: boolean }) {
  const [menu, setMenu] = useState(false);
  const [imageFailed, setImageFailed] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const firstLink = useRef<HTMLAnchorElement>(null);
  const nav = useRef<HTMLElement>(null);
  const asset = exactImage ? "arrival-hero.webp" : "hospitality.webp";
  useEffect(() => {
    if (!menu) return;
    firstLink.current?.focus();
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") { setMenu(false); trigger.current?.focus(); }
    };
    const closeOnOutside = (event: PointerEvent) => {
      if (event.target instanceof Node && !nav.current?.contains(event.target) && !trigger.current?.contains(event.target)) setMenu(false);
    };
    window.addEventListener("keydown", closeOnEscape);
    window.addEventListener("pointerdown", closeOnOutside);
    return () => {
      window.removeEventListener("keydown", closeOnEscape);
      window.removeEventListener("pointerdown", closeOnOutside);
    };
  }, [menu]);
  return <section className="ao-opening" aria-labelledby="ao-title" data-image-source={exactImage?"catalog-p6":"catalog-p1-alternate"}>
    <a className="ao-skip" href="#ao-title">Skip to introduction</a>
    <header className="ao-header">
      <a className="ao-brand" href="/" aria-label="Plantupia Green Technology home"><span>PLANTUPIA</span><small>GREEN TECHNOLOGY</small></a>
      <button className="ao-menu" ref={trigger} type="button" aria-expanded={menu} aria-controls="ao-navigation" onClick={()=>setMenu(v=>!v)}>{menu?"Close":"Menu"}<span aria-hidden="true">{menu?"\u2212":"+"}</span></button>
      <nav ref={nav} id="ao-navigation" className="ao-navigation" data-open={menu} aria-label="Main navigation">{destinations.map((item,i)=><a key={item.href} href={item.href} ref={i===0?firstLink:undefined}>{item.label}</a>)}</nav>
      <a className="ao-contact" href="/start-a-space">Start a space <span aria-hidden="true">↗</span></a>
    </header>
    <div className="ao-content">
      <p className="ao-eyebrow">LIVING ARCHITECTURE <span aria-hidden="true">/</span> 01</p>
      <h1 id="ao-title" tabIndex={-1}>Nature belongs<br/>in the <em>architecture.</em></h1>
      <figure className="ao-figure">
        <div className="ao-photo">{imageFailed?<p role="status">Catalogue image unavailable.</p>:<img src={"/api/private-arrival/"+asset} alt="Living greenery integrated into the columns and boundaries of a hospitality interior" width={exactImage?1537:1672} height={exactImage?1023:941} fetchPriority="high" onError={()=>setImageFailed(true)}/>}</div>
        <figcaption><span>Hospitality greenery catalogue · illustrative reference{!exactImage?" · alternate image":""}</span><span>01 / Arrival</span></figcaption>
      </figure>
      <p className="ao-deck">Planting design for homes, hospitality and commercial spaces — from the first concept to ongoing care.</p>
      <div className="ao-actions"><a className="ao-primary" href="/spaces">Explore our spaces <span aria-hidden="true">↗</span></a><a className="ao-secondary" href="/studio">Our approach <span aria-hidden="true">↗</span></a></div>
    </div>
    <div className="ao-bottom"><span>PLANTUPIA / EGYPT</span><a href="#pa-study">Discover the practice <span aria-hidden="true">↓</span></a></div>
  </section>;
}