import type { Metadata } from "next";
import CoherenceStudy from "./CoherenceStudy";

export const metadata: Metadata = {
  title: "Plantupia — One Living Language / Research",
  description: "Private creative research into the relationship between place, planting choice, objects and care.",
  robots: { index: false, follow: false }
};

export default function Page() { return <CoherenceStudy />; }
