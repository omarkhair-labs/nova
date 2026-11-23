import { isProductionSite } from "../../publication";
import type { Metadata } from "next";
import HospitalitySector from "../../HospitalitySector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Hospitality | Plantupia", description: "Biophilic design for hospitality, from arrival to atmosphere.", robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false } };
export default function HospitalityPage() {

  return <HospitalitySector />;
}
