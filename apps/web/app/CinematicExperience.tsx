"use client";
import { useEffect, useRef, useState } from "react";
import { privateSceneOverrides } from "./arrival-private-scenes";
import "./study/scene/scene.css";
import "./arrival-photo-direction.css";
import SiteIndex from "./SiteIndex";
import WorldThreshold from "./WorldThreshold";
import { CompanyIntroduction } from "./CompanyStory";
import ShopBridge from "./ShopBridge";
import HomeVisualField from "./HomeVisualField";
type Stage = 0 | 1 | 2;

const originalScenes = [
  {
    eyebrow: "01 / THE SPACE",
    label: "The Space",
    line: "Architecture and light establish a sense of place.",
    detail: "An architectural reference for light and material. This photo shows an independent place, not a future or previous stage of another scene.",
    visual: "https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/space.webp",
    alt: "Editorial photograph of a tree within a contemporary concrete courtyard; one of three independent reference locations, not a Plantupia project.",
    markerLabels: ["The light", "The threshold", "The materials"],
    markerDescriptions: [
      "Light and the architectural opening establish the atmosphere of the place.",
      "A threshold tells us where life could belong within the built space.",
      "Concrete, geometry and daylight establish the conditions we begin with.",
    ],
  },
  {
    eyebrow: "02 / WITH LIFE",
    label: "With Life",
    line: "Living material brings another layer to the room.",
    detail: "A photographic reference for the relationship between architecture and planted interiors; not a documented Plantupia intervention.",
    visual: "https://images.pexels.com/photos/28461045/pexels-photo-28461045.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/life.webp",
    alt: "Editorial photograph of a bright contemporary office with greenery; an independent location, not a second stage of the previous room.",
    markerLabels: ["Living material", "Filtered light", "A softer boundary"],
    markerDescriptions: [
      "Planting becomes part of the room rather than an object placed on top.",
      "Leaves interrupt and filter the light, bringing another rhythm into the atmosphere.",
      "An architectural edge can hold a living, changing layer.",
    ],
  },
  {
    eyebrow: "03 / OVER TIME",
    label: "Over Time",
    line: "A living place invites us to return and observe.",
    detail: "A third photographic reference for the attention living places deserve. This is not a real time-lapse or care record.",
    visual: "https://images.pexels.com/photos/36776751/pexels-photo-36776751.jpeg?auto=compress&cs=tinysrgb&w=2560",
    fallback: "/scene/hd/time.webp",
    alt: "Editorial photograph of a modern retail atrium with indoor trees; an independent location illustrating observation rather than biological growth.",
    markerLabels: ["Notice", "Return", "Remember"],
    markerDescriptions: [
      "Observation turns the living space into an ongoing relationship.",
      "Care is a repeated human action, not an automatic green checkmark.",
      "Documented moments can become the honest memory of a place over time.",
    ],
  },
] as const;

export default function SceneStudy({ privateReview = false }: { privateReview?: boolean }) {
  const scenes = privateReview ? originalScenes.map((scene, i) => ({ ...scene, ...privateSceneOverrides[i] })) : originalScenes;
  const [stage,setStage]=useState<Stage>(0);
  const [failed,setFailed]=useState<number[]>([]);
  const rootRef=useRef<HTMLDivElement>(null);
  // Follow the pointer only while Selected Work is visible. Scrolling must
  // never replace or cover the content with a transient full-screen index.
  useEffect(()=>{
    const field=rootRef.current?.querySelector<HTMLElement>(".hvf");
    if(!field) return;
    const reduced=window.matchMedia("(prefers-reduced-motion: reduce)");
    const fine=window.matchMedia("(pointer: fine)");
    let visible=false;
    let raf=0;
    let lastUpdate=0;
    let x="72%",y="38%";
    const observer=new IntersectionObserver(([entry])=>{
      visible=entry?.isIntersecting ?? false;
    },{threshold:0});
    observer.observe(field);
    const follow=(event:PointerEvent)=>{
      if(!visible||reduced.matches||!fine.matches) return;
      const now=performance.now();
      if(now-lastUpdate<90||raf) return;
      lastUpdate=now;
      x=(event.clientX/window.innerWidth*100).toFixed(1)+"%";
      y=(event.clientY/window.innerHeight*100).toFixed(1)+"%";
      raf=window.requestAnimationFrame(()=>{
        field.style.setProperty("--pl-px",x);
        field.style.setProperty("--pl-py",y);
        raf=0;
      });
    };
    window.addEventListener("pointermove",follow,{passive:true});
    return ()=>{
      window.removeEventListener("pointermove",follow);
      observer.disconnect();
      if(raf) window.cancelAnimationFrame(raf);
    };
  },[]);
  const scene=scenes[stage];
  function move(direction:number){setStage(previous=>((previous+direction+scenes.length)%scenes.length) as Stage)}
  return <div ref={rootRef} className="ps-site" data-stage={stage} data-private-review={privateReview}>
    <a href="#ps-main" className="ps-skip">Skip to experience</a>
    <section className="ps-opening" aria-labelledby="ps-title">
      <div className="ps-stage-images" role="img" aria-label={failed.includes(stage)?"Generated architectural concept illustration":scene.alt}>
        {scenes.map((item,i)=><div key={item.label} className={"ps-image"+(i===stage?" ps-current":"")} aria-hidden="true"><img src={failed.includes(i)?item.fallback:item.visual} alt="" loading="eager" decoding="async" fetchPriority={i===0?"high":"auto"} onError={()=>setFailed(old=>old.includes(i)?old:[...old,i])}/></div>)}
        <div className="ps-grading" aria-hidden="true"/>
      </div>
      <header className="ps-topbar"><a href="/" className="ps-brand" aria-label="Plantupia home">◕ Plantupia</a><SiteIndex current="experience" tone="dark"/><a className="ps-contact" href="/start-a-space">Get in touch ↗</a></header>
      <main className="ps-main" id="ps-main">
        <div className="ps-story"><p className="ps-kicker"><span className="ps-kicker-rule"/> {privateReview?"GREEN TECHNOLOGY / BIOPHILIC INTEGRATION":"LIVING ARCHITECTURE"}</p><h1 id="ps-title">A Space<br/>Becoming <em>Alive.</em></h1><p>{privateReview?"Plantupia integrates living material into the built environment through design, installation and continuing care — with technology and sustainability treated as part of the system, not decoration.":"Plantupia brings nature into the built environment — from a single room to the places an entire city shares."}</p>{privateReview&&<div className="ps-system-strip" aria-label="Plantupia design system"><span><b>01</b> SITE CONDITIONS</span><span><b>02</b> LIVING MATERIAL</span><span><b>03</b> CONTINUING CARE</span></div>}<a className="ps-cta" href="#world">CONTINUE INTO PLANTUPIA <span aria-hidden="true">↓</span></a><nav className="ps-mobile-paths" aria-label="Explore Plantupia on mobile"><a href="/spaces/projects">Explore the work <span aria-hidden="true">↗</span></a><a href="/start-a-space">Start a space <span aria-hidden="true">↗</span></a></nav></div>
        <div className="ps-rail">
          <div className="ps-scene-caption" key={stage} aria-live="polite"><p className="ps-kicker">{String(stage+1).padStart(2,"0")} / 03 · {scene.label.toUpperCase()}</p><p>{scene.line}</p></div>
          <div className="ps-sequence" role="group" aria-label="Choose a photographic study">{scenes.map((item,i)=><button type="button" key={item.label} className="ps-chapter" aria-pressed={stage===i} onClick={()=>setStage(i as Stage)}><span aria-hidden="true"/>{item.label}</button>)}</div>
          <div className="ps-arrows"><button type="button" className="ps-circle-button" aria-label="Previous study" onClick={()=>move(-1)}>←</button><button type="button" className="ps-circle-button" aria-label="Next study" onClick={()=>move(1)}>→</button></div>
        </div>
        <div className="ps-utility-row ps-source-row"><span>THREE INDEPENDENT ARCHITECTURAL STUDIES</span><a href="#scene-notes">About these images ↗</a></div>
      </main>
    </section>
    <section className="ps-fieldnotes" id="scene-notes"><details><summary>Three photographic studies <span aria-hidden="true">+</span></summary><p>{privateReview ? "Independent studies from supplied Plantupia proposals and catalogue. Not evidence of completed installation, before-and-after or growth. Generated concept images are used if a source image is unavailable." : "Independent editorial references, not before-and-after images or completed Plantupia projects. Images: DAIPI, Alpha En and mingche lee via Pexels. Generated concept images are used if a photograph is unavailable."}</p><h2>{scene.label}</h2><p>{scene.detail}</p><div className="ps-note-grid">{scene.markerLabels.map((label,i)=><article key={label}><h3>{label}</h3><p>{scene.markerDescriptions[i]}</p></article>)}</div></details></section>
    {privateReview&&<HomeVisualField/>}<WorldThreshold privateReview={privateReview}/><CompanyIntroduction privateReview={privateReview}/><ShopBridge privateReview={privateReview}/>
  </div>;
}
