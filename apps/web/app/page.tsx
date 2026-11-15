import type { Metadata } from "next";
import { Arrow, Invitation, SiteFrame } from "./SiteFrame";
import SceneEntrance from "./SceneEntrance";

export const metadata: Metadata = {
  title: "Plantupia — Where life takes root",
  description: "Enter a living courtyard, then explore Plantupia’s planting practice, its spaces and the care that continues over time.",
};

const paths = [
  { href: "/spaces", label: "Places", title: "Where it can grow", detail: "Six environments, from a small home to a shared square." },
  { href: "/studio", label: "Approach", title: "What the place asks", detail: "Read the light, movement and access for care in one courtyard." },
  { href: "/living-systems", label: "Aftercare", title: "What we come back to", detail: "Follow a planting and the people who care for it." },
];

export default function HomePage() {
  return <SiteFrame current="/">
    <SceneEntrance />
    <section className="courtyard-crossroads" id="practice" aria-labelledby="crossroads-title">
      <div className="field-shell">
        <div className="courtyard-crossroads-intro">
          <p className="field-kicker"><span /> Explore Plantupia</p>
          <h2 id="crossroads-title">Take another <em>path.</em></h2>
        </div>
        <ul>
          {paths.map(path => <li key={path.href}>
            <a href={path.href}>
              <span className="field-kicker">{path.label}</span>
              <span><strong>{path.title}</strong><small>{path.detail}</small></span>
              <Arrow />
            </a>
          </li>)}
        </ul>
      </div>
    </section>
    <Invitation />
  </SiteFrame>;
}
