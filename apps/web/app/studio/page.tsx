import type { Metadata } from "next";
import WorldRoute from "../WorldRoute";

export const metadata: Metadata = {
  title: "The Studio — Plantupia",
  description: "A design process built around place, material, life and continuing care.",
  robots: { index: false, follow: false },
};

export default function StudioPage() {
  return <WorldRoute kind="studio" />;
}
