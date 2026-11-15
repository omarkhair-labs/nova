import type { Metadata } from "next";
import StudioAtelier from "../StudioAtelier";

export const metadata: Metadata = {
  title: "The Studio — Plantupia",
  description: "A design process built around place, material, life and continuing care.",
  robots: { index: false, follow: false },
};

export default function StudioPage() {
  return <StudioAtelier />;
}
