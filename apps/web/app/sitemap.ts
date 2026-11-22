import { publicationApproved } from "./publication";
import type { MetadataRoute } from "next";

const routes = [
  "/",
  "/spaces",
  "/spaces/hospitality",
  "/spaces/commercial",
  "/spaces/offices",
  "/spaces/projects",
  "/spaces/projects/mazal",
  "/spaces/projects/em-sherif",
  "/spaces/projects/el-hallab",
  "/spaces/projects/pixels-mall",
  "/spaces/projects/audi-cairo",
  "/spaces/projects/villa-7",
  "/shop",
  "/studio",
  "/living-systems",
  "/about",
  "/start-a-space",
] as const;

export default function sitemap(): MetadataRoute.Sitemap {
  if (!publicationApproved) return [];
  const productionHost = process.env.VERCEL_PROJECT_PRODUCTION_URL || "plantupia-public-preview.vercel.app";
  const base = `https://${productionHost}`;
  return routes.map((path) => ({
    url: base + path,
    changeFrequency: path === "/" ? "weekly" : "monthly",
    priority: path === "/" ? 1 : path === "/spaces" || path === "/shop" ? 0.9 : 0.7,
  }));
}
