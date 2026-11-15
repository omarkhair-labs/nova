import type { Metadata, Viewport } from "next";
import "./base.css";

export const metadata: Metadata = {
  title: "Plantupia — Where life takes root",
  description: "Planting, architectural integration and continuing care for the places we build.",
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#121a13",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body id="top">{children}</body></html>;
}
