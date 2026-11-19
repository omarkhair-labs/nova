import type { Metadata } from "next";
import { notFound } from "next/navigation";
import CommercialSector from "../../CommercialSector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Commercial | Plantupia", description: "Biophilic design for commercial destinations, developers and showrooms.", robots: process.env.VERCEL_ENV === "production" ? { index: true, follow: true } : { index: false, follow: false } };
export default function CommercialPage() {
  if (process.env.VERCEL_ENV !== "production" && (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)) notFound();
  return <CommercialSector />;
}
