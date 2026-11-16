import { courtyard } from "./Photograph";
import { tending, threshold } from "./courtyard-scenes";
import { SceneBackdrop, SceneCredit, SceneNote, scenePoint } from "./SpatialStage";

const scenes = [
  {
    id: "threshold", image: threshold, label: "Plantupia", title: "Where life takes root.",
    description: "Planting and continuing care for the places we build.",
    next: { href: "#planting", label: "Step inside", x: .66, y: .57 },
    paths: [],
  },
  {
    id: "planting", image: courtyard, label: "Inside the courtyard", title: "Room to grow.",
    description: "The bed makes space for roots, and the path gives people a way through.",
    next: { href: "/studio", label: "Read this place", x: .68, y: .72 },
    paths: [{ href: "#threshold", label: "← Threshold" }, { href: "#return", label: "Return later →" }],
  },
  {
    id: "return", image: tending, label: "A return visit", title: "Come back to it.",
    description: "Look at what changes when people keep coming back to care for the same planting.",
    next: { href: "/living-systems#observation", label: "Follow its care", x: .53, y: .42 },
    paths: [{ href: "#planting", label: "← Planting" }],
  },
] as const;

export default function SceneEntrance() {
  return <section className="scene-world entry-world" aria-label="An imagined living courtyard">
    <div className="entry-views" aria-hidden="true">
      {scenes.map((scene, index) => <div key={scene.id} data-shot={scene.id}>
        <SceneBackdrop image={scene.image} priority={index === 0} />
      </div>)}
    </div>
    <div className="scene-shadow" aria-hidden="true" />
    {scenes.map(scene => <div key={scene.id} className="entry-reading" data-shot={scene.id}>
      <div className="entry-note">
        <SceneNote id={scene.id} label={scene.label} title={scene.title}>
          <p>{scene.description}</p>
          {scene.id === "threshold" && <a className="world-action entry-direct" href="/studio">Read the courtyard ↗</a>}
        </SceneNote>
      </div>
      <div className="image-coordinates entry-points">
        <a className="scene-gateway image-point" href={scene.next.href} style={scenePoint(scene.next.x, scene.next.y)}>
          <span className="gateway-ring" aria-hidden="true" />
          <span>{scene.next.label}</span>
        </a>
      </div>
      {scene.paths.length > 0 && <div className="entry-return">
        {scene.paths.map(path => <a key={path.href} href={path.href}>{path.label}</a>)}
      </div>}
    </div>)}
    <SceneCredit>Imagined courtyard · Generated scenes</SceneCredit>
  </section>;
}
