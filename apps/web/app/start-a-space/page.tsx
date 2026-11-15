import type { Metadata } from "next";
import { SiteFrame, TextLink } from "../SiteFrame";
import { materials, Photograph } from "../Photograph";
import ProjectBrief from "../ProjectBrief";
import { getEnvironment } from "../environments";
import { topics } from "../contact-topics";

export const metadata: Metadata = {
  title: "Get in touch — Plantupia",
  description: "Prepare a project brief, a care question or a general message to share through an approved Plantupia contact channel.",
};

type Params = Promise<{ space?: string | string[]; topic?: string | string[] }>;

export default async function StartASpacePage({ searchParams }: { searchParams: Params }) {
  const { space, topic } = await searchParams;
  const initialTopic = topics.find(item => item.id === topic)?.id ?? "project";
  const initialSpace = typeof space === "string" ? getEnvironment(space)?.slug ?? "" : "";

  return <SiteFrame current="/start-a-space">
    <section className="field-shell field-contact">
      <div className="field-contact-intro">
        <p className="field-kicker"><span /> Get in touch</p>
        <h1>Tell us about<br /><em>your place.</em></h1>
        <p className="field-lead">Prepare a note about a new project, an existing planting or an idea you’d like to share.</p>
        <p className="field-contact-privacy">This preview prepares a copy on your device only. Nothing is sent or stored by Plantupia.</p>
        <Photograph image={materials} caption="Materials and light · Generated concept" sizes="(max-width: 760px) 90vw, 35vw" />
      </div>
      <ProjectBrief key={initialTopic + initialSpace} initialTopic={initialTopic} initialSpace={initialSpace} />
    </section>
    <div className="field-shell field-archive-link">
      <p>Still figuring out what your space needs?</p>
      <TextLink href="/spaces">Explore the design studies</TextLink>
    </div>
  </SiteFrame>;
}
