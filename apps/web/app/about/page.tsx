import type { Metadata } from "next";
import { Invitation, SiteFrame, TextLink } from "../SiteFrame";
import { courtyard, Photograph } from "../Photograph";

export const metadata: Metadata = {
  title: "About — Plantupia",
  description: "Plantupia brings nature, architecture and continuing care together through planting for the built environment.",
};

export default function AboutPage() {
  return <SiteFrame current="/about">
    <section className="field-shell field-page-intro">
      <p className="field-kicker"><span /> About Plantupia</p>
      <div className="field-intro-grid">
        <h1>Nature belongs<br /><em>where we build.</em></h1>
        <p className="field-lead">Plantupia designs and installs planting for built spaces. We bring the architecture, the people who use it and the life of the plants into the same conversation.</p>
      </div>
    </section>
    <section className="field-shell field-about-image">
      <Photograph image={courtyard} caption="A planted courtyard · Generated concept" sizes="(max-width: 760px) 90vw, 94vw" priority />
    </section>
    <section className="field-shell field-about-copy">
      <h2>Make room for<br /><em>what grows.</em></h2>
      <div>
        <p>Living planting belongs in decisions about how a place works. A tree can mark an arrival. A low bed can give a room a quieter edge. A shaded courtyard can make time spent outdoors more inviting.</p>
        <p>We start from what is already there: light and daily use, the materials of the building, and the people who will look after it. Each space asks for a different response.</p>
        <TextLink href="/studio">Explore our approach</TextLink>
      </div>
    </section>
    <section id="selected-works" className="field-archive">
      <div className="field-shell field-about-copy">
        <h2>Project archive</h2>
        <div>
          <p>Plantupia’s project archive is awaiting approved photographs and project details. The design studies on this site explore possibilities for different spaces.</p>
          <TextLink href="/spaces">Browse the design studies</TextLink>
        </div>
      </div>
    </section>
    <Invitation />
  </SiteFrame>;
}
