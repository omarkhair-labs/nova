import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import { environments } from "../environments";
import { SceneBackdrop, SceneCredit, SceneNote } from "../SpatialStage";

export const metadata: Metadata = {
  title: "About — Plantupia",
  description: "Plantupia designs and installs planting for built spaces with continuing care in mind.",
};

export default function AboutPage() {
  return <SiteFrame current="/about">
    <section className="scene-world about-world">
      <SceneBackdrop image={environments[5].image} />
      <div className="scene-shadow" aria-hidden="true" />
      <div className="about-note">
        <SceneNote label="About Plantupia" title={<>Nature belongs<br />where we build.</>}>
          <p>Plantupia designs and installs planting for built spaces. We bring the architecture, the people who use it and the life of the plants into the same conversation.</p>
          <a className="world-action" href="/start-a-space">Prepare a note ↗</a>
        </SceneNote>
        <div className="about-archive" id="selected-works">
          <h2>Project archive</h2>
          <p>The archive awaits approved photographs and project details. Explore the design studies to read what different places ask of their plants.</p>
          <a href="/spaces">Explore spaces ↗</a>
        </div>
      </div>
      <SceneCredit>Shared space study · Generated concept</SceneCredit>
    </section>
  </SiteFrame>;
}
