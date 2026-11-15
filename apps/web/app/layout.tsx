import type { Metadata, Viewport } from "next";
import "./base.css";

export const metadata: Metadata = {
  title: "Plantupia — Room for life",
  description: "Planting, architectural integration and continuing care for the places we build.",
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#f6f3e9",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body id="top">{children}</body></html>;
}
