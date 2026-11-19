import type { Metadata } from "next";
import CinematicExperience from "./CinematicExperience";

export const metadata: Metadata = {
  title: "Plantupia — A Space Becoming Alive",
  description: "A cinematic, interactive exploration of architecture, living material and the continuity of care.",
  robots: { index: false, follow: false },
};

export default function HomePage() {
  const root = process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR;
  const privateReview = process.env.NODE_ENV === "development" && !!root;
  return <CinematicExperience privateReview={privateReview} />;
}
