import type { Metadata } from "next";
import { Invitation, SiteFrame, TextLink } from "../SiteFrame";
import { care, Photograph } from "../Photograph";

export const metadata: Metadata = {
  title: "Aftercare — Plantupia",
  description: "What happens after installation, from handover and observation to care, follow-up and a record that stays with the plant.",
};

const moments = [
  {
    id: "handover",
    label: "Handover",
    title: "Know what’s there.",
    text: "The planter beside the window has a place in the room and a reason for being there. Note its location, what was planted and who will be looking after it. That first record gives later observations context.",
  },
  {
    id: "observation",
    label: "Observation",
    title: "Notice what changes.",
    text: "Someone notices a pale lower leaf. They note where it appeared and how the room has been used recently. A dated photo gives whoever follows up a point of comparison.",
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
          <p className="field-lead">Plants change as rooms and routines change around them. A good handover gives the people caring for them a place to start.</p>
          <TextLink href="#one-plant" arrow="↓">Follow one plant</TextLink>
        </div>
        <Photograph image={care} caption="Checking a leaf · Generated concept" priority sizes="(max-width: 760px) 90vw, 42vw" />
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
