import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import SpacePassage from "../SpacePassage";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore planting in homes, workplaces, hospitality, industrial sites, retail and public spaces.",
};

export default function SpacesPage() {
  return <SiteFrame current="/spaces"><SpacePassage /></SiteFrame>;
}
