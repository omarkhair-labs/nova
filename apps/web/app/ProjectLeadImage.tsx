import { projects } from "./project-archive-data";
import "./project-lead.css";

type ProjectId = (typeof projects)[number]["id"];

/** One source-backed opening frame, never represented as an installed project.
 * Layout is intentionally case-specific; inner image remains uncropped in the
 * detailed proposal sequence farther down each page.
 */
export default function ProjectLeadImage({ project }: { project: ProjectId }) {
  const item = projects.find(value => value.id === project);
  if (!item) return null;
  const number = String(projects.findIndex(value => value.id === project) + 1).padStart(2, "0");
  return <figure className={"case-lead case-lead--" + item.id}>
    <div className="case-lead-photo">
      <img src={"/api/private-arrival/" + item.image} alt={item.alt} loading="eager" decoding="async" fetchPriority="high" />
      <span className="case-lead-number" aria-hidden="true">{number} / 06</span>
    </div>
    <figcaption>
      <span>{item.name} / {item.type}</span>
      <span>DESIGN PROPOSAL / SOURCE P{item.page}</span>
    </figcaption>
  </figure>;
}
