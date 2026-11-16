import type { Metadata } from "next";
import { SiteFrame } from "../SiteFrame";
import SiteReading from "../SiteReading";

export const metadata: Metadata = {
  title: "Approach — Plantupia",
  description: "Read the courtyard through light, movement and access for care.",
};

export default function StudioPage() {
  return <SiteFrame current="/studio"><SiteReading /></SiteFrame>;
}
