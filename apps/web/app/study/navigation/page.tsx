"use client";
import { useEffect, useRef, useState } from "react";
import "./navigation.css";

type Mode = "SPACE" | "LIFE" | "TIME";
const modes: Mode[] = ["SPACE", "LIFE", "TIME"];
const info: Record<Mode, string> = {
  SPACE: "Architecture sets the frame.",
  LIFE: "Living material changes what the room feels like.",
  TIME: "Day one is not the whole story.",
};
const points = [
  ["Light", "Light and architecture set the conditions."],
  ["Plant Life", "Living material belongs to the place, not on top of it."],
  ["Materials", "Stone and leaves change how a boundary feels."],
] as const;

export default function NavigationStudy() {
  const [mode, setMode] = useState<Mode>("LIFE");
  const [point, setPoint] = useState(1);
  const [menu, setMenu] = useState(false);
  const [chapter, setChapter] = useState(0);
  const trigger = useRef<HTMLButtonElement>(null);
  const first = useRef<HTMLButtonElement>(null);
  useEffect(() => { if (menu) first.current?.focus(); }, [menu]);
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape" && menu) { setMenu(false); trigger.current?.focus(); }
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [menu]);
  const close = () => { setMenu(false); trigger.current?.focus(); };
  const chapters = [
    ["THE EXPERIENCE", "Enter a living space"],
    ["OUR WORK", "The architecture and the living material"],
    ["LIVING SYSTEMS", "What continues after installation"],
  ];
  return (
    <div className={"cn-stage cn-" + mode.toLowerCase()}>
      <a className="cn-skip" href="#cn-main">Skip to experience</a>
      <div className="cn-backdrop" aria-hidden="true"><span className="cn-arch"/><span className="cn-leaf"/></div>
      <header className="cn-header">
        <a className="cn-brand" href="/study/navigation/" aria-label="Plantupia study home"><span>◧</span> Plantupia</a>
        <button ref={trigger} type="button" className="cn-plus"
          aria-expanded={menu} aria-controls="cn-index" aria-label={menu?"Close index":"Open experience index"}
          onClick={()=>setMenu(value=>!value)}>{menu?"×":"+"}</button>
        <nav aria-label="Three perspectives" className="cn-modes">
          {modes.map(value=><button type="button" key={value}
            aria-pressed={mode===value} onClick={()=>setMode(value)}>{value}</button>)}
        </nav>
      </header>
      <main id="cn-main">
        <div className="cn-editorial">
          <span className="cn-eyebrow">LIVING ARCHITECTURE / NAVIGATION STUDY 01</span>
          <h1>A Space<br/>Becoming <em>Alive.</em></h1>
          <p>The space and what lives inside it belong to one story.</p>
        </div>
        <div className="cn-points" role="group" aria-label="Three points to inspect">
          {points.map(([label],i)=><button key={label} type="button"
            className={"cn-point cn-point-"+i} aria-pressed={point===i} onClick={()=>setPoint(i)}>
            <span>{String(i+1).padStart(2,"0")}</span>{label}</button>)}
        </div>
        <div className="cn-caption" aria-live="polite">
          <span>VIEW / {mode}</span>
          <strong>{info[mode]}</strong>
          <small>POINT 0{point+1} / {points[point][1]}</small>
        </div>
        <p className="cn-flag">DESKTOP INTERACTION STUDY · SCENE IS A TEMPORARY ABSTRACT PLACEHOLDER, NOT THE APPROVED CINEMATIC RENDER</p>
      </main>
      <footer className="cn-footer"><span>PLANTUPIA / 01</span><span>A SPACE BECOMING ALIVE</span><span>STUDY ONLY</span></footer>
      {menu&&<div id="cn-index" className="cn-overlay" role="dialog" aria-modal="true" aria-label="Experience index">
        <div className="cn-overlay-header"><span>PLANTUPIA / INDEX</span>
          <button type="button" onClick={close}>CLOSE ×</button></div>
        <div className="cn-overlay-content">
          <h2>One world.<br/><em>Different ways in.</em></h2>
          <div className="cn-chapters" role="group" aria-label="Choose an experience chapter">
            {chapters.map(([label,desc],i)=><button ref={i===0?first:undefined} type="button"
              key={label} aria-pressed={chapter===i}
              onClick={()=>{setChapter(i);close();}}>
              <span>0{i+1}</span><strong>{label}</strong><small>{desc}</small><span aria-hidden="true">↗</span>
            </button>)}
            <p>Chapter selection is a navigation prototype, not an invented finished page.</p>
          </div>
        </div>
        <div className="cn-overlay-footer">ESC TO RETURN TO THE SPACE</div>
      </div>}
    </div>
  );
}
