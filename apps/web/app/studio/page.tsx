import type { Metadata } from "next";
import { Invitation, SiteFrame, TextLink } from "../SiteFrame";
import { materials, Photograph } from "../Photograph";
import SiteReading from "../SiteReading";

export const metadata: Metadata = {
  title: "Approach — Plantupia",
  description: "Read a place through its light, movement and access for care. See how those conditions shape planting.",
};

export default function StudioPage() {
  return <SiteFrame current="/studio">
    <section className="field-shell field-page-intro">
      <p className="field-kicker"><span /> Approach</p>
      <div className="field-intro-grid">
        <h1>Start by<br /><em>looking.</em></h1>
        <p className="field-lead">A planting plan begins with the room, courtyard or path it belongs to. We read the place before choosing what should grow there.</p>
      </div>
    </section>

    <section className="field-reading-section" aria-labelledby="read-title">
      <div className="field-shell">
        <h2 id="read-title">Read the courtyard.</h2>
        <SiteReading />
      </div>
    </section>

    <section className="field-shell field-material-section" aria-labelledby="material-title">
      <Photograph image={materials} caption="Materials and light · Generated concept" sizes="(max-width: 760px) 90vw, 48vw" />
      <div>
        <h2 id="material-title">From the first visit to <em>handover.</em></h2>
        <p>We consider the plants, their growing space and the materials around them together. The people who will use and care for the place can help shape those decisions early.</p>
        <p>At handover, the reasons for those choices matter. Knowing what was planted, where it sits and how to look after it gives the next person somewhere to begin.</p>
        <TextLink href="/living-systems">What happens after handover</TextLink>
      </div>
    </section>
    <Invitation />
  </SiteFrame>;
}
