import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import { materials } from "../Photograph";
import ProjectBrief from "../ProjectBrief";
import { getEnvironment } from "../environments";
import { topics } from "../contact-topics";
import { SceneBackdrop, SceneCredit, SceneNote } from "../SpatialStage";

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
    <section className="scene-world contact-world">
      <SceneBackdrop image={materials} />
      <div className="scene-shadow" aria-hidden="true" />
      <div className="contact-note">
        <SceneNote label="Contact" title="Tell us about your place.">
          <p>Prepare a note about a project, a planting you care for or an idea to share.</p>
          <p className="contact-privacy">This prepares a local copy. Nothing is sent or stored by Plantupia.</p>
        </SceneNote>
      </div>
      <div className="contact-table">
        <ProjectBrief key={initialTopic + initialSpace} initialTopic={initialTopic} initialSpace={initialSpace} />
      </div>
      <SceneCredit>Worktable · Generated concept</SceneCredit>
    </section>
  </SiteFrame>;
}
