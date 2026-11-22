import { publicationApproved } from "../publication";
import type { Metadata } from "next";
import CompanyStory from "../CompanyStory";
export const metadata: Metadata = {
  title: "About Plantupia — living architecture",
  description: "Explore Plantupia's approach to the built environment, living material and continuing care.",
  robots: publicationApproved ? { index: true, follow: true } : { index: false, follow: false },
};
export default function AboutPage() {
  return <CompanyStory privateReview={(process.env.VERCEL_ENV === "preview" || publicationApproved || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))} />;
}
