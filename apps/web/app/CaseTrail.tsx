import Link from "next/link";
import { projects } from "./project-archive-data";
import "./case-trail.css";

type ProjectId = (typeof projects)[number]["id"];

const handoffs: Record<ProjectId, { name: string; reading: string }> = {
  "mazal": {
    name: "From façade to vessel.",
    reading: "The entrance changes with the planting. Next, hold the container still and watch seven planting characters change around it.",
  },
  "em-sherif": {
    name: "From one vessel to a terrace edge.",
    reading: "What happens when the same choice is extended across a whole outdoor guest boundary?",
  },
  "el-hallab": {
    name: "From a planted edge to a destination.",
    reading: "Follow the planting outward: beyond a terrace, into an architectural landscape and the way people move through it.",
  },
  "pixels-mall": {
    name: "From landscape to interior.",
    reading: "The next study moves the same question indoors, through six showroom moments and their planned care programme.",
  },
  "audi-cairo": {
    name: "From showroom to daily life.",
    reading: "A residence stretches the material choices into ten different places that all belong to the same home.",
  },
  "villa-7": {
    name: "Back to the threshold.",
    reading: "Return to one façade and see how nine planting directions change the character of arrival.",
  },
};

const processPaths: Record<ProjectId, { href: string; label: string }> = {
  "mazal": { href: "/studio", label: "How design alternatives take shape" },
  "em-sherif": { href: "/studio", label: "How material choices are specified" },
  "el-hallab": { href: "/living-systems", label: "Continuing care beyond selection" },
  "pixels-mall": { href: "/studio", label: "How site conditions shape the design" },
  "audi-cairo": { href: "/living-systems", label: "From installation to continuing care" },
  "villa-7": { href: "/living-systems", label: "What a living care record can contain" },
};

/** A connected editorial reading sequence, not six indistinguishable
 * project cards. Previews use proposal source material, never installed work.
 */
export default function CaseTrail({ current }: { current: ProjectId }) {
  const i = projects.findIndex(p => p.id === current);
  if (i < 0) return null;
  const next = projects[(i + 1) % projects.length];
  const handoff = handoffs[current];
  const wraps = i === projects.length - 1;
  return <aside className={"ct-trail ct-trail--" + current} aria-label="Continue through the project studies">
    <div className="ct-trail-reading">
      <span className="ct-overline">FIELD ARCHIVE / {String(i + 1).padStart(2, "0")} OF 06</span>
      <h2>{handoff.name}</h2>
      <p>{handoff.reading}</p>
      <nav className="ct-reading-paths" aria-label="Related Plantupia readings">
        <Link href="/spaces/projects" className="ct-index">All six project studies <span aria-hidden="true">↗</span></Link>
        <Link href={processPaths[current].href} className="ct-side">{processPaths[current].label} <span aria-hidden="true">↗</span></Link>
      </nav>
    </div>
    <Link href={"/spaces/projects/" + next.id} className="ct-trail-next" aria-label={(wraps ? "Return to " : "Continue to ") + next.name + " proposal study"}>
      <div className="ct-trail-image">
        <img src={"/api/private-arrival/" + next.image} alt={next.alt} loading="lazy" decoding="async"/>
        <span className="ct-image-label">{wraps ? "RETURN / 01" : "NEXT / " + String(i + 2).padStart(2, "0")}</span>
      </div>
      <div className="ct-trail-footer">
        <span><small>{next.sector.toUpperCase()} / DESIGN PROPOSAL</small><strong>{next.name}</strong></span>
        <span className="ct-trail-arrow" aria-hidden="true">↗</span>
      </div>
    </Link>
  </aside>;
}
