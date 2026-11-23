import { isProductionSite } from "./publication";
import type { Metadata, Viewport } from "next";
import "./base.css";
import "./material-scroll.css";

export const metadata: Metadata = {
  title: "Plantupia — A Space Becoming Alive",
  description: "An English-only creative website exploring living architecture, plants, people and time.",
  robots: isProductionSite ? { index: true, follow: true } : { index: false, follow: false },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#152920",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
