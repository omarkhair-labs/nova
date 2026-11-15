import type { Metadata } from "next";
import { Arrow, Invitation, SiteFrame, TextLink } from "./SiteFrame";
import { care, courtyard, Photograph } from "./Photograph";
import { environments } from "./environments";

export const metadata: Metadata = {
  title: "Plantupia — Room for life",
  description: "Plantupia designs and installs planting for the built environment, with continuing care in mind from the first visit.",
};

export default function HomePage() {
  return <SiteFrame>
    <section className="field-shell field-opening">
      <div className="field-opening-copy">
        <p className="field-kicker"><span /> Planting for built spaces</p>
        <h1>Room for<br /><em>life.</em></h1>
        <p className="field-lead">Plantupia designs and installs planting for the places we build, with the people who use them and the care that comes after in mind.</p>
        <a className="field-button" href="/spaces">Explore spaces <Arrow /></a>
      </div>
      <Photograph image={courtyard} caption="A planted courtyard · Generated concept" className="field-cover" priority />
      <a href="#practice" className="field-opening-down" aria-label="Continue to our approach"><Arrow>↓</Arrow></a>
    </section>

    <section className="field-practice" id="practice" aria-labelledby="practice-title">
      <div className="field-shell field-practice-inner">
        <p className="field-kicker"><span /> Our approach</p>
        <div>
          <h2 id="practice-title">The place comes <em>first.</em></h2>
          <p>Where does the light fall? How do people move? Who can reach the plants to care for them? These are design questions from the first visit.</p>
          <TextLink href="/studio">How we approach a space</TextLink>
        </div>
      </div>
    </section>

    <section className="field-shell field-home-spaces" aria-labelledby="home-spaces-title">
      <div className="field-section-title">
        <p className="field-kicker"><span /> Spaces</p>
        <h2 id="home-spaces-title">From one room<br />to <em>shared ground.</em></h2>
        <p>A windowsill has different needs from a hotel entrance or a factory courtyard. Find the conditions that matter in your space.</p>
      </div>
      <ul className="field-environment-index">
        {environments.map(item => <li key={item.slug}>
          <a href={"/spaces/" + item.slug}>
            <span><strong>{item.name}</strong><small>{item.question}</small></span><Arrow />
          </a>
        </li>)}
      </ul>
    </section>

    <section className="field-home-care" aria-labelledby="home-care-title">
      <div className="field-shell field-home-care-inner">
        <Photograph image={care} caption="Checking a leaf · Generated concept" sizes="(max-width: 760px) 100vw, 38vw" />
        <div>
          <p className="field-kicker"><span /> Aftercare</p>
          <h2 id="home-care-title">The work keeps <em>growing.</em></h2>
          <p>Light shifts. A room gets used in new ways. A leaf changes. The care that follows installation helps us understand what a place and its plants need over time.</p>
          <TextLink href="/living-systems">Follow the life of a planting</TextLink>
        </div>
      </div>
    </section>
    <Invitation />
  </SiteFrame>;
}
