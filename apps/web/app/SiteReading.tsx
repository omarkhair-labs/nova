import { courtyard } from "./Photograph";
import { SceneBackdrop, SceneCredit, SceneNote, scenePoint } from "./SpatialStage";

const lenses = [
  {
    key: "light", label: "Light", x: .46, y: .45,
    title: "Watch the shadow.", text: "The arch shades one end while direct light reaches the open court. Watch both through the day before choosing what should grow here.",
  },
  {
    key: "movement", label: "Movement", x: .52, y: .84,
    title: "Walk the passage.", text: "People follow the clear strip beside the bed. Leave space for the canopy to grow and for a person to pass, sit or turn.",
  },
  {
    key: "care", label: "Care", x: .67, y: .75,
    title: "Leave room to work.", text: "The bed gives roots room below ground. Its edge needs to be reachable without blocking the passage people use.",
  },
] as const;

export default function SiteReading() {
  return <section className="scene-world study-world">
    <SceneBackdrop image={courtyard} className="study-backdrop" />
    <div className="scene-shadow" aria-hidden="true" />
    <div className="study-note">
      <SceneNote label="Approach" title="Read the courtyard">
        <fieldset className="image-coordinates study-points">
          <legend className="visually-hidden">Read a detail in the courtyard</legend>
          {lenses.map(lens => <label key={lens.key} className="image-point study-point"
            data-lens={lens.key} style={scenePoint(lens.x, lens.y)}>
            <input type="radio" name="reading-lens" defaultChecked={lens.key === "light"}
              aria-describedby={"reading-" + lens.key} />
            <span className="gateway-ring" aria-hidden="true" />
            <span className="study-point-label">{lens.label}</span>
          </label>)}
        </fieldset>
        <div role="status" className="study-caption">
          {lenses.map(lens => <div key={lens.key} data-reading={lens.key} id={"reading-" + lens.key}>
            <strong>{lens.title}</strong><p>{lens.text}</p>
          </div>)}
        </div>
        <a className="world-action" href="/living-systems">Return after installation <span aria-hidden="true">↗</span></a>
      </SceneNote>
    </div>
    <SceneCredit>Courtyard study · Generated concept</SceneCredit>
  </section>;
}
