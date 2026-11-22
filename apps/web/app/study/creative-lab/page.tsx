import type { Metadata } from "next";
import CreativeLab from "./CreativeLab";

export const metadata: Metadata = {
  title: "Plantupia — Creative R&D / Three Living Directions",
  description: "Private design explorations of light, material and time using supplied Plantupia proposal imagery.",
  robots: { index: false, follow: false },
};

export default function CreativeLabPage() {
  return <CreativeLab />;
}
