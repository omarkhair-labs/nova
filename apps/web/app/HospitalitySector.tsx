import SiteIndex from "./SiteIndex";
import "./space-explorer.css";
import "./hospitality-sector.css";

const moments = [
  { label:"Arrival", heading:"The welcome starts outside.", body:"A planted facade can frame the entrance and express the character of a restaurant before guests step inside.", name:"Mazal", source:"Facade Planting Proposal / option 03 / page 5", detail:"Luxury Tropical layers Strelitzia, Areca palm and Monstera. Nine directions explore tropical, statement and evergreen identities.", href:"/spaces/projects/mazal", image:"sector-hospitality-arrival.webp", alt:"Mazal restaurant facade planting visualization, Luxury Tropical option", spec:[["Design field","Facade planting"],["Directions","Nine options"],["Families","Tropical / Statement / Evergreen"]] },
  { label:"Seating", heading:"Keep the view. Shape the boundary.", body:"Planting can define the space between tables without closing it. Vessel height, planting density and the view are one decision.", name:"EM Sherif Sea Cafe", source:"Planting Proposal / option E / page 7", detail:"The proposal holds one navy planter constant and tests seven planting characters. Option E uses a low Schefflera hedge to preserve the sea view.", href:"/spaces/projects/em-sherif", image:"sector-hospitality-seating.webp", alt:"EM Sherif navy planter proposal with a low Schefflera hedge", spec:[["Vessel","Navy metal / fiberglass liner"],["Dimensions","140 × 50 × 50 or 150 × 50 × 50 cm"],["Drainage","Self-drainage system"]] },
  { label:"Terrace", heading:"Design for the weather and the return.", body:"Outdoor planting has to respond to light, water, seasonality and the upkeep of a working hospitality space.", name:"El Hallab", source:"Planting Concepts for the Terrace / option 03 / page 5", detail:"Silver Sculpted proposes clipped Texas sage for full sun, low water needs and regular shaping. Five options compare seasonal appearance and maintenance.", href:"/spaces/projects/el-hallab", image:"sector-hospitality-terrace.webp", alt:"El Hallab terrace proposal with a clipped silver-grey hedge", spec:[["Light","Full sun"],["Water","Low"],["Maintenance","Regular shaping"]] },
] as const;

const steps = [
  ["Choose a direction", "Define the guest experience and the planting character worth testing."],
  ["Read the site", "Confirm planter count, dimensions, circulation and sun exposure on site."],
  ["Specify the proposal", "Resolve plants, vessels, installation requirements and commercial scope."],
  ["Install & care", "Agree delivery, planting and the continuing maintenance programme."],
] as const;

export default function HospitalitySector() {
  return <div className="sx-page hs-page hs-world">
    <a className="sx-skip" href="#hs-main">Skip to hospitality</a>
    <header className="sx-header"><a href="/" className="sx-brand">Plantupia</a><SiteIndex current="spaces"/><a className="sx-contact" href="/start-a-space?space=gather">Get in touch <span aria-hidden="true">&#8599;</span></a></header>
    <main id="hs-main" className="hs-main">
      <a className="sx-underlink hs-back" href="/spaces">&#8592; Spaces / sector index</a>

      <section className="hs-title" aria-labelledby="hs-title">
        <div><p className="sx-kicker">01 / HOSPITALITY</p><h1 id="hs-title">From arrival<br/>to <em>atmosphere.</em></h1></div>
        <p>Restaurants, cafes and hospitality spaces are experienced as a sequence. The planting should be read the same way: first impression, occupied space, outdoor condition, then care.</p>
      </section>

      <section className="hs-journey" aria-label="Three hospitality design moments">
        {moments.map((m,index)=><article className={"hs-moment hs-moment-"+(index+1)} key={m.label}>
          <figure><img src={"/api/private-arrival/"+m.image} alt={m.alt} loading={index===0?"eager":"lazy"}/><figcaption><span>0{index+1} / {m.label.toUpperCase()}</span><span>{m.name} / {m.source}</span></figcaption></figure>
          <div className="hs-moment-copy">
            <div><span className="hs-moment-no">0{index+1}</span><p className="sx-kicker">{m.label.toUpperCase()} / {m.name.toUpperCase()}</p><h2>{m.heading}</h2><p>{m.body}</p><p className="hs-evidence">{m.detail}</p><a className="sx-underlink hs-case-link" href={m.href}>Open {m.name} case study ↗</a></div>
            <dl>{m.spec.map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
          </div>
        </article>)}
      </section>

      <section className="hs-process" aria-labelledby="hs-process-title">
        <div><p className="sx-kicker">FROM CONCEPT TO CARE</p><h2 id="hs-process-title">Four decisions, in order.</h2><p>The proposals show a repeatable logic without pretending every site follows one fixed recipe.</p></div>
        <ol>{steps.map(([title,body],index)=><li key={title}><span>0{index+1}</span><div><h3>{title}</h3><p>{body}</p></div></li>)}</ol>
      </section>

      <section className="hs-source-note"><span>SOURCE POSITION</span><p>The images above are proposal visualizations from Mazal, EM Sherif and El Hallab. They are used to explain three hospitality design conditions, not to imply completed installations.</p></section>

      <section className="hs-contact"><p className="sx-kicker">YOUR HOSPITALITY SPACE</p><h2>Start with the guest journey.</h2><div className="hs-contact-links"><a className="sx-underlink" href="/start-a-space?space=gather">Discuss a hospitality project ↗</a><a className="sx-underlink" href="/living-systems">See how care continues ↗</a></div></section>
    </main>
    <footer className="sx-footer"><a href="/spaces">Explore spaces</a><span>Plantupia / Hospitality</span><a href="/spaces/projects">Project archive ↗</a></footer>
  </div>;
}
