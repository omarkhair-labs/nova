import type { Metadata } from "next";
import WorldRoute from "../WorldRoute";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore the questions behind living spaces for home, work and gathering.",
  robots: { index: false, follow: false },
};

export default function SpacesPage() {
  return <WorldRoute kind="spaces" />;
}
