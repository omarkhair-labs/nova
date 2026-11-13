import type { Metadata } from "next";
import SpaceBrief from "../SpaceBrief";

export const metadata: Metadata = {
  title: "Start with your space — Plantupia",
  description: "Explore your space, purpose and plans. Prepare an outline to share with the Plantupia team through an approved contact channel.",
  robots: { index: false, follow: false },
};

export default function StartASpacePage() {
  return <SpaceBrief />;
}
