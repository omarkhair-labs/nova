import { isProductionSite, websiteInboxOperational } from "../publication";
import type { Metadata } from "next";
import ContactStudio from "../ContactStudio";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Get in touch — Plantupia",
  description: "Send your project, care or general enquiry to the Plantupia team.",
  robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false },
};

export default function StartASpacePage() {
  // Preserve the existing production Inbox even while proposal-image
  // publication still awaits separate rights approval.
  const available = websiteInboxOperational;
  return <ContactStudio available={available} previewMode={process.env.VERCEL_ENV !== "production"} />;
}
