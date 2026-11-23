import { isProductionSite } from "../../publication";
import type { Metadata } from "next";
import OfficesSector from "../../OfficesSector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Offices & HQs | Plantupia", description: "Planting, vessels and ongoing care for working environments.", robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false } };
export default function OfficesPage(){

  return <OfficesSector/>;
}
