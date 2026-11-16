import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import { materials } from "../Photograph";
import ProjectBrief from "../ProjectBrief";
import { getEnvironment } from "../environments";
import { topics } from "../contact-topics";
import { SceneBackdrop, SceneCredit } from "../SpatialStage";
import "./contact.css";

export const metadata: Metadata = {
  title: "Contact — Plantupia",
  description: "Prepare a local draft about a project, care question or idea for Plantupia.",
};

type Params = Promise<{ space?: string | string[]; topic?: string | string[] }>;

export default async function StartASpacePage({ searchParams }: { searchParams: Params }) {
  const { space, topic } = await searchParams;
  const initialTopic = topics.find(item => item.id === topic)?.id ?? "project";
  const initialSpace = typeof space === "string" ? getEnvironment(space)?.slug ?? "" : "";

  return <SiteFrame current="/start-a-space">
    <section className="scene-world contact-world" aria-labelledby="contact-title">
      <SceneBackdrop image={materials} />
      <div className="contact-sheet">
        <header className="contact-heading">
          <p className="scene-coordinate">Contact</p>
          <h1 id="contact-title">Tell us about your <em>place.</em></h1>
          <p className="contact-privacy">Prepare a copy of your message here. Nothing is sent or stored by Plantupia.</p>
        </header>
        <ProjectBrief key={initialTopic + initialSpace} initialTopic={initialTopic} initialSpace={initialSpace} />
      </div>
      <SceneCredit>Worktable · Generated concept</SceneCredit>
    </section>
  </SiteFrame>;
}
