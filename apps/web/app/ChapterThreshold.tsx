import "./chapter-threshold.css";

type Chapter = "experience" | "spaces" | "studio" | "systems" | "about";
const passages = {
  experience: {number:2,name:"Spaces",href:"/spaces",line:"Every place begins with a question.",note:"From the atmosphere of a place to the environments we share.",previous:null},
  spaces: {number:3,name:"The Studio",href:"/studio",line:"A place becomes an intention.",note:"From observing architecture to composing living material.",previous:{href:"/",name:"The Experience"}},
  studio: {number:4,name:"Living Systems",href:"/living-systems",line:"The drawing becomes a relationship.",note:"From the moment of installation to a continuity of care.",previous:{href:"/spaces",name:"Spaces"}},
  systems: {number:5,name:"About Plantupia",href:"/about",line:"Behind every living place, people.",note:"From the living record to the practice behind it.",previous:{href:"/studio",name:"The Studio"}},
  about: {number:6,name:"Get in touch",href:"/start-a-space",line:"The next place could be yours.",note:"From an understanding of the practice to a real conversation.",previous:{href:"/living-systems",name:"Living Systems"}},
} as const;

function PassageDrawing({number}:{number:number}){
  return <svg viewBox="0 0 480 285" className="ch-drawing" aria-hidden="true" focusable="false">
    <g fill="none" stroke="currentColor" strokeWidth=".9" opacity=".32">
      <path d="M16 259H464M240 14V270" strokeDasharray="3 5"/>
      <path d="M60 245V72L240 26L420 72V245M60 72L240 118L420 72M240 118V245"/>
      <path d="M112 245V110L240 77L368 110V245"/>
    </g>
    {number===2&&<g fill="none" stroke="currentColor"><path className="ch-reveal" d="M60 245L240 185L420 245M112 245L240 204L368 245M240 118L240 185" strokeWidth="1.3"/><path d="M144 119L240 94L338 119L240 146Z" fill="currentColor" opacity=".13"/></g>}
    {number===3&&<g stroke="currentColor" fill="none"><path className="ch-reveal" d="M112 245L240 194L368 245M146 155H334V230H146Z" strokeWidth="1.3"/>{[0,1,2].map(i=><circle key={i} cx={182+i*58} cy="189" r="21" fill="currentColor" fillOpacity=".10" strokeDasharray="3 3"/>)}</g>}
    {number===4&&<g stroke="currentColor" fill="none"><path className="ch-reveal" d="M240 245Q218 174 245 70M235 187Q160 170 151 114Q211 119 235 187M239 145Q306 138 332 91Q273 83 239 145M243 101Q209 70 222 41Q252 62 243 101" strokeWidth="1.5"/><circle cx="240" cy="155" r="107" strokeDasharray="2 6" opacity=".55"/></g>}
    {number===5&&<g stroke="currentColor" fill="none"><path className="ch-reveal" d="M105 220L183 167L240 198L301 133L375 73" strokeWidth="1.3"/>{[[105,220],[183,167],[240,198],[301,133],[375,73]].map(([x,y],i)=><circle key={i} cx={x} cy={y} r="7" fill="currentColor" fillOpacity=".13"/>)}</g>}
    {number===6&&<g stroke="currentColor" fill="none"><path className="ch-reveal" d="M171 245V110L240 83L310 110V245M171 110L240 137L310 110M240 137V245" strokeWidth="1.5"/><path d="M171 245L240 221L310 245" opacity=".5"/><path d="M240 83L310 110V245L240 221Z" fill="currentColor" opacity=".1"/></g>}
    <text x="18" y="276" fill="currentColor" fontSize="8" letterSpacing="2">STUDY / 0{number}</text>
  </svg>
}

export default function ChapterThreshold({from}:{from:Chapter}){
  const p=passages[from];
  return <section className={"ch-threshold ch-passage-"+p.number} data-next-chapter={p.number} aria-label={"Continue to "+p.name}>
    <div className="ch-meta"><span>PLANTUPIA / THE PASSAGE</span><span>{String(p.number-1).padStart(2,"0")} — {String(p.number).padStart(2,"0")} / 06</span></div>
    <a href={p.href} className="ch-main">
      <div className="ch-copy"><span className="ch-label">NEXT CHAPTER / {String(p.number).padStart(2,"0")}</span><h2>{p.line}</h2><p>{p.note}</p><span className="ch-enter">Enter {p.name} <span aria-hidden="true">↗</span></span></div>
      <div className="ch-art"><PassageDrawing number={p.number}/></div>
    </a>
    <div className="ch-bottom"><div className="ch-progress" aria-hidden="true">{[1,2,3,4,5,6].map(n=><i key={n} data-filled={n<p.number} data-next={n===p.number}/>)}</div>{p.previous?<a href={p.previous.href}>← {p.previous.name}</a>:<a href="#world">↑ Return to the chapter index</a>}</div>
  </section>
}
