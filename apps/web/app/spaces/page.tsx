import { publicationApproved } from "../publication";
import type { Metadata } from "next";
import SpaceExplorer from "../SpaceExplorer";

export const metadata: Metadata = {
  title: "Spaces — Plantupia",
  description: "Explore six built environments, from residential spaces to industry and hospitality.",
  robots: publicationApproved ? { index: true, follow: true } : { index: false, follow: false },
};

export default function SpacesPage() {
  return <SpaceExplorer privateReview={(process.env.VERCEL_ENV === "preview" || publicationApproved || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))}/>;
}
