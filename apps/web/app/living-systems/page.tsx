import type { Metadata } from "next";
import { Invitation, SiteFrame, TextLink } from "../SiteFrame";
import { Photograph } from "../Photograph";
import { tending } from "../courtyard-scenes";

export const metadata: Metadata = {
  title: "Aftercare — Plantupia",
  description: "What happens after installation, from handover and observation to care, follow-up and a record that stays with the plant.",
};

const moments = [
  {
    id: "handover",
    label: "Handover",
    title: "Know what’s there.",
    text: "The olive sits in a bed beside an open passage. At handover, record what was planted, where the bed can be reached and who will look after it. Those details give later observations context.",
  },
  {
    id: "observation",
    label: "Observation",
    title: "Notice what changes.",
    text: "On a return visit, someone notices a cluster of pale leaves on the side facing the passage. They record where they saw it and take a dated photo from a place they can find again.",
  },
  {
    id: "follow-up",
    label: "Care & follow-up",
    title: "Return to the same plant.",
    text: "The next visit starts with the earlier note. Record any action taken and what can be seen on the return visit. If the cause is still unclear, the record can say so.",
  },
  {
    id: "history",
    label: "History",
    title: "Keep what was learned.",
    text: "When someone new takes over, the plant’s location, observations and care stay together. They can pick up from what the previous person actually saw and did.",
  },
];

export default function LivingSystemsPage() {
  return <SiteFrame current="/living-systems">
    <section className="field-aftercare">
      <div className="field-shell field-aftercare-inner">
        <div>
          <p className="field-kicker"><span /> Aftercare</p>
          <h1>Life after<br /><em>installation.</em></h1>
          <p className="field-lead">Return to the courtyard. The plant, its surroundings and the routines of the people looking after it all change over time.</p>
          <TextLink href="#one-plant" arrow="↓">Follow one plant</TextLink>
        </div>
        <Photograph image={tending} className="aftercare-plate" caption="Returning to the courtyard · Generated concept" priority sizes="(max-width: 760px) 90vw, 42vw" />
      </div>
    </section>

    <section className="field-shell field-journal" id="one-plant" aria-labelledby="one-plant-title">
      <div className="field-journal-intro">
        <p className="field-kicker"><span /> Illustrative care sequence</p>
        <h2 id="one-plant-title">What we learn<br />by <em>coming back.</em></h2>
        <p>Follow an example from first handover to a history someone else can pick up.</p>
        <nav aria-label="Care sequence">
          {moments.map(moment => <TextLink key={moment.id} href={"#" + moment.id} arrow="↓">{moment.label}</TextLink>)}
        </nav>
      </div>
      <ol className="field-moments">
        {moments.map(moment => <li key={moment.id} id={moment.id}>
          <p className="field-kicker">{moment.label}</p>
          <h3>{moment.title}</h3>
          <p>{moment.text}</p>
        </li>)}
      </ol>
    </section>
    <Invitation title="Already caring for a space?" href="/start-a-space?topic=care" label="Prepare a care message" />
  </SiteFrame>;
}
