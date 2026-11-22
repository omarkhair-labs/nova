import { publicationApproved } from "../publication";
import type { Metadata } from "next";
import ShopExperience from "../ShopExperience";
import { getPublicCatalog } from "./catalog";

export const dynamic = "force-dynamic";
export const metadata: Metadata = {
  title: "Objects for Living — Plantupia",
  description: "An edited collection of reviewed, published plants, planters and care objects from Plantupia.",
  robots: publicationApproved ? { index: true, follow: true } : { index: false, follow: false },
};

export default async function CommercePage() {
  const catalog = await getPublicCatalog();
  return <ShopExperience catalog={catalog} privateReview={(process.env.VERCEL_ENV === "preview" || publicationApproved || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))}/>;
}
