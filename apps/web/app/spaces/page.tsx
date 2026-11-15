import type { Metadata } from "next";
import { Invitation, SiteFrame, TextLink } from "../SiteFrame";
import SpacePassage from "../SpacePassage";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore planting for homes, workplaces, hospitality, industrial sites, retail and public spaces.",
};

export default function SpacesPage() {
  return <SiteFrame current="/spaces">
    <section className="field-shell field-page-intro field-spaces-intro">
      <p className="field-kicker"><span /> Spaces</p>
      <div className="field-intro-grid">
        <h1>Where life<br /><em>can belong.</em></h1>
        <p className="field-lead">Choose a place to explore what its light, daily use and conditions for care ask of its plants.</p>
      </div>
    </section>
    <SpacePassage />
    <div className="field-shell field-archive-link">
      <p>Plantupia’s project archive is awaiting approved photography and project details.</p>
      <TextLink href="/about#selected-works">About the archive</TextLink>
    </div>
    <Invitation />
  </SiteFrame>;
}
