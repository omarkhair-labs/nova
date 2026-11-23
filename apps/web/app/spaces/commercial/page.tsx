import { isProductionSite } from "../../publication";
import type { Metadata } from "next";
import CommercialSector from "../../CommercialSector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Commercial | Plantupia", description: "Biophilic design for commercial destinations, developers and showrooms.", robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false } };
export default function CommercialPage() {

  return <CommercialSector />;
}
