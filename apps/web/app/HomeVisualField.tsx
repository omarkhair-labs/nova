import { homeField } from "./visual-production-map";
import "./home-visual-field.css";

export default function HomeVisualField(){
 return <section className="hvf" aria-labelledby="hvf-title">
  <header className="hvf-head"><div><p>SELECTED WORK / VISUAL FIELD</p><h2 id="hvf-title">A living practice,<br/><em>seen in place.</em></h2></div><div><p>Proposal studies across commercial, hospitality and residential environments. The images show design direction, not verified completed installations.</p><a href="/spaces/projects">Open all case studies &#8599;</a></div></header>
  <div className="hvf-grid">{homeField.map((item,i)=><a key={item.asset} href={item.href} className={"hvf-item hvf-"+(i+1)}><figure><img src={"/api/private-arrival/"+item.asset} alt={item.label+" proposal visual"} loading={i<2?"eager":"lazy"}/><figcaption><span>{String(i+1).padStart(2,"0")}</span><div><strong>{item.label}</strong><small>{item.meta}</small></div></figcaption></figure></a>)}</div>
  <div className="hvf-rapid-index" aria-hidden="true"><div className="hvf-rapid-list">{homeField.map((item,i)=><div className="hvf-rapid-item" key={"rapid-"+item.asset}><span>{String(i+1).padStart(2,"0")}</span><img src={"/api/private-arrival/"+item.asset} alt="" loading="lazy"/><strong>{item.label}</strong></div>)}</div><p>SELECTED WORK / RAPID INDEX</p></div>
  <footer><span>PROJECTS / SPACES / CARE</span><p>Different scales, one living material.</p><a href="/spaces">Explore the environments &#8599;</a></footer>
 </section>
}
