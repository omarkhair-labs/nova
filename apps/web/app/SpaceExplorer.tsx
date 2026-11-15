"use client";
import { useState } from "react";
import SiteIndex from "./SiteIndex";
import { spaces, editorialSpaces, spacePrompts } from "./space-contexts";
import "./space-explorer.css";

const keys = ["home", "work", "gather", "industrial", "retail", "public"] as const;
const shortNames = ["Residential", "Workplaces", "Hospitality", "Industry", "Retail", "Public spaces"] as const;

export default function SpaceExplorer() {
  const [active, setActive] = useState(0);
  const [failed, setFailed] = useState<number[]>([]);
  const photo = editorialSpaces[active];
  const space = spaces[active];
  function select(index: number) {
    setActive((index + spaces.length) % spaces.length);
  }
  return (
    <div className="sx-page">
      <a className="sx-skip" href="#sx-main">Skip to spaces</a>
      <header className="sx-header">
        <a href="/" className="sx-brand" aria-label="Plantupia home">◕ Plantupia</a>
        <SiteIndex current="spaces" />
        <a href="/start-a-space" className="sx-contact">Get in touch <span aria-hidden="true">↗</span></a>
      </header>
      <main id="sx-main">
        <section className="sx-exhibit" aria-label="Explore living environments">
          <div className="sx-intro"><div><p className="sx-kicker">02 / SPACES</p><h1>Every space <em>has its own life.</em></h1></div><p>From a quiet home to the scale of a factory. Nature can belong wherever people build.</p></div>
          <div className="sx-picker" role="group" aria-label="Choose an environment">
            {shortNames.map((name, i) => (
              <button key={name} type="button" aria-pressed={active===i} onClick={()=>select(i)}><span>{String(i+1).padStart(2,"0")}</span>{name}</button>
            ))}
          </div>
          <figure className="sx-figure">
            <div className="sx-frame" aria-hidden="true">
              {editorialSpaces.map((item,i)=><div key={item.source} className={"sx-layer"+(active===i?" sx-current":"")}>
                <img src={failed.includes(i)?item.fallback:item.src} alt="" loading={i===0?"eager":"lazy"} fetchPriority={i===0?"high":"auto"}
                  onError={()=>setFailed(previous=>previous.includes(i)?previous:[...previous,i])}/>
              </div>)}
            </div>
            <figcaption className="sx-caption">
              <span>{failed.includes(active)?"Generated concept fallback":photo.subject} · Illustrative reference, not client work</span>
              {!failed.includes(active)&&<a href={photo.source} target="_blank" rel="noreferrer">{photo.credit} ↗</a>}
            </figcaption>
          </figure>
          <div className="sx-rail">
            <div className="sx-progress"><span>{space.id} / 06</span><div aria-hidden="true"><i style={{width:((active+1)/spaces.length*100)+"%"}}/></div></div>
            <div className="sx-arrows"><button type="button" aria-label="Previous environment" onClick={()=>select(active-1)}>←</button><button type="button" aria-label="Next environment" onClick={()=>select(active+1)}>→</button></div>
          </div>
          <div key={space.id} className="sx-reading" aria-live="polite">
            <div><p className="sx-kicker">{space.type}</p><h2>{space.headline}</h2></div>
            <div><p>{space.text}</p><a className="sx-underlink" href={"/start-a-space?space="+keys[active]}>Discuss a space like this ↗</a></div>
          </div>
          <details className="sx-considerations"><summary>Design prompts for this space <span aria-hidden="true">+</span></summary>
            <div>{spacePrompts[active].map(prompt=><article key={prompt.label}><h3>{prompt.label}</h3><p>{prompt.detail}</p></article>)}</div>
          </details>
        </section>
        <section className="sx-next"><p className="sx-kicker">FROM SPACE TO PRACTICE</p><h2>Ideas take root through design.</h2><a href="/studio" className="sx-underlink">Explore the studio ↗</a><a href="/about#selected-works" className="sx-underlink">Discover our approach &amp; project archive ↗</a></section>
      </main>
      <footer className="sx-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/start-a-space">Contact ↗</a></footer>
    </div>
  );
}
