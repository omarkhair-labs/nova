import { isProductionSite } from "../publication";
import type { Metadata } from "next";
import LivingContinuity from "../LivingContinuity";

export const metadata: Metadata = {
  title: "Living Systems — Plantupia",
  description: "An exploration of how plants, place, recorded care and time connect.",
  robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false },
};

export default function LivingSystemsPage() {
  return <LivingContinuity privateReview={(process.env.VERCEL_ENV === "preview" || isProductionSite || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))} />;
}
