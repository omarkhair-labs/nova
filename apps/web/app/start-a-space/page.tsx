import type { Metadata } from "next";
import ContactStudio from "../ContactStudio";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Get in touch — Plantupia",
  description: "Send your project, care or general enquiry to the Plantupia team.",
  robots: process.env.VERCEL_ENV === "production" ? { index: true, follow: true } : { index: false, follow: false },
};

export default function StartASpacePage() {
  const available = process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED === "1" &&
    !!process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND &&
    (process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN?.length ?? 0) >= 32;
  return <ContactStudio available={available} />;
}
