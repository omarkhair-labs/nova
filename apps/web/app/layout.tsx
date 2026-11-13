import type { Metadata, Viewport } from "next";
import "./atlas.css";

export const metadata: Metadata = {
  title: "Plantupia — The Living Atlas",
  description: "Explore how a place, living plants, human care and time become one connected experience.",
  robots: { index: false, follow: false },
  alternates: { languages: { en: "/", ar: "/ar/" } },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#eeeee5",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
