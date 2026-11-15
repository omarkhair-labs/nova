import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import { courtyard, materials } from "../Photograph";
import { tending } from "../courtyard-scenes";
import { SceneBackdrop, SceneCredit, SceneNote } from "../SpatialStage";

export const metadata: Metadata = {
  title: "Aftercare — Plantupia",
  description: "Return to the courtyard and read how an illustrative planting is handed over, observed and cared for.",
};

const moments = [
  { id: "handover", label: "Handover", title: "Know what’s there.", text: "Record the olive, its bed beside the passage and where someone can reach it. A person who takes over care can start with the same details.", image: courtyard },
  { id: "observation", label: "Observation", title: "Notice a change.", text: "On a return visit, someone notices pale leaves on the side facing the passage. A dated photograph from a point they can find again gives the next visit context.", image: tending },
  { id: "follow-up", label: "Follow-up", title: "Return to the same spot.", text: "Compare what can be seen today with the earlier note. Record any action taken and what is still uncertain about the change.", image: tending },
  { id: "history", label: "History", title: "Keep what was learned.", text: "The next caretaker can read the placement, observations, actions and follow-up together. The record belongs to the planting and its place.", image: materials },
] as const;

export default function LivingSystemsPage() {
  return <SiteFrame current="/living-systems">
    <section className="scene-world time-world" aria-label="Illustrative care sequence">
      {moments.map((moment, index) => <div key={moment.id} id={moment.id} className="time-frame">
        <SceneBackdrop image={moment.image} className={moment.id === "observation" ? "time-return-image" : ""}
          priority={index === 0} />
      </div>)}
      <div className="scene-shadow" aria-hidden="true" />
      <div className="time-note">
        <SceneNote label="Aftercare · Illustrative sequence" title="Life after installation.">
          {moments.map(moment => <div key={moment.id} data-time={moment.id} className="time-telling">
            <h2>{moment.title}</h2><p>{moment.text}</p>
          </div>)}
          <a className="world-action" href="/start-a-space?topic=care">Prepare a care message <span aria-hidden="true">↗</span></a>
        </SceneNote>
      </div>
      <nav className="time-path" aria-label="Care sequence">
        {moments.map(moment => <a key={moment.id} href={"#" + moment.id} data-time={moment.id}>
          <span className="map-pin" aria-hidden="true" />{moment.label}
        </a>)}
      </nav>
      <SceneCredit>Imagined return visits · Generated concepts</SceneCredit>
    </section>
  </SiteFrame>;
}
