import type { Metadata } from "next";
import WorldRoute from "../WorldRoute";

export const metadata: Metadata = {
  title: "Living Systems — Plantupia",
  description: "An exploration of how plants, place, recorded care and time connect.",
  robots: { index: false, follow: false },
};

export default function LivingSystemsPage() {
  return <WorldRoute kind="systems" />;
}
