import type { Metadata } from "next";
import { notFound } from "next/navigation";
import OfficesSector from "../../OfficesSector";
export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Offices & HQs | Plantupia", description: "Planting, vessels and ongoing care for working environments.", robots: process.env.VERCEL_ENV === "production" ? { index: true, follow: true } : { index: false, follow: false } };
export default function OfficesPage(){
  if(process.env.VERCEL_ENV !== "preview" && process.env.VERCEL_ENV !== "production" && (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)) notFound();
  return <OfficesSector/>;
}
