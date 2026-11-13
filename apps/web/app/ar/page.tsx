import type { Metadata } from "next";
import LivingAtlas from "../LivingAtlas";

export const metadata: Metadata = {
  title: "بلانتيبيا — الأطلس الحي",
  description: "تجربة بلانتيبيا التفاعلية: استكشف العلاقة بين المكان والنبات والعناية والزمن.",
  robots: { index: false, follow: false },
  alternates: { languages: { en: "/", ar: "/ar/" } },
};

export default function ArabicHomePage() {
  return <LivingAtlas lang="ar" />;
}
