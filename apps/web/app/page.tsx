import type { Metadata } from "next";
import WorldExperience from "./WorldExperience";

export const metadata: Metadata = {
  title: "Plantupia — Spatial Design, Living Material & Green Technology",
  description: "Plantupia connects spatial design, living material, green technology and continuing care in one living system.",
  robots: process.env.VERCEL_ENV === "production" ? { index: true, follow: true } : { index: false, follow: false },
};

export default function HomePage() {
  return <WorldExperience />;
}
