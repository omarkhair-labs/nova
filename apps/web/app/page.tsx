import type { Metadata } from "next";
import { SiteFrame } from "./SiteFrame";
import SceneEntrance from "./SceneEntrance";

export const metadata: Metadata = {
  title: "Plantupia — Where life takes root",
  description: "Enter an imagined living courtyard and explore how the place, the people and its planting shape one another.",
};

export default function HomePage() {
  return <SiteFrame><SceneEntrance /></SiteFrame>;
}
