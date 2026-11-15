import type { Metadata } from "next";
import { Arrow, Invitation, SiteFrame, TextLink } from "../SiteFrame";
import { Photograph } from "../Photograph";
import { environments } from "../environments";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore planting for homes, workplaces, hospitality, industrial sites, retail and public spaces.",
};

export default function SpacesPage() {
  return <SiteFrame current="/spaces">
    <section className="field-shell field-page-intro field-spaces-intro">
      <p className="field-kicker"><span /> Spaces</p>
      <div className="field-intro-grid">
        <h1>Different places.<br /><em>Different needs.</em></h1>
        <p className="field-lead">A room, a lobby and a public square each ask something different of their plants. Explore six types of places and the design questions each brings.</p>
      </div>
    </section>
    <section aria-label="Types of spaces" className="field-shell field-spaces-list">
      <ul>{environments.map(item => <li key={item.slug}>
        <a href={"/spaces/" + item.slug} aria-label={item.name + " — " + item.question}>
          <Photograph image={item.image} caption="Generated concept" sizes="(max-width: 374px) 90vw, (max-width: 1050px) 43vw, 29vw" />
          <div className="field-space-card-label"><div><h2>{item.name}</h2><p>{item.question}</p></div><Arrow /></div>
        </a>
      </li>)}</ul>
    </section>
    <div className="field-shell field-archive-link">
      <p>These are design studies. Plantupia’s project archive is awaiting approved photography and project details.</p>
      <TextLink href="/about#selected-works">About the archive</TextLink>
    </div>
    <Invitation />
  </SiteFrame>;
}
