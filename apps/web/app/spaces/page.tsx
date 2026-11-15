import type { Metadata } from "next";
import SpaceExplorer from "../SpaceExplorer";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore six built environments, from residential spaces to industry and hospitality.",
  robots: { index: false, follow: false },
};

export default function SpacesPage() {
  return <SpaceExplorer />;
}
