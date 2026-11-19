import type { Metadata } from "next";
import { notFound } from "next/navigation";
import HospitalitySector from "../../HospitalitySector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Hospitality | Plantupia", description: "Biophilic design for hospitality, from arrival to atmosphere.", robots: { index: false, follow: false } };
export default function HospitalityPage() {
  if (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR) notFound();
  return <HospitalitySector />;
}
