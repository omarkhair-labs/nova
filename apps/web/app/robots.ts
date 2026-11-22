import { publicationApproved } from "./publication";
import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  const production = publicationApproved;
  const productionHost = process.env.VERCEL_PROJECT_PRODUCTION_URL || "plantupia-public-preview.vercel.app";
  return production
    ? {
        rules: { userAgent: "*", allow: "/" },
        sitemap: `https://${productionHost}/sitemap.xml`,
      }
    : {
        rules: { userAgent: "*", disallow: "/" },
      };
}
