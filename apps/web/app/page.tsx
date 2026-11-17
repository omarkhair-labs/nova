import type { Metadata } from "next";
import CinematicExperience from "./CinematicExperience";

export const metadata: Metadata = {
  title: "Plantupia — A Space Becoming Alive",
  description: "A cinematic, interactive exploration of architecture, living material and the continuity of care.",
  robots: { index: false, follow: false },
};

export default function HomePage() {
  return <CinematicExperience />;
}
