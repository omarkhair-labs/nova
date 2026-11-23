/** Site context, not an editorial approval system. */
export const isProductionSite = process.env.VERCEL_ENV === "production";

/**
 * Keep existing server-side contact delivery separately gated on its real
 * backend configuration, without coupling it to images or page visibility.
 */
export const websiteInboxOperational =
  isProductionSite &&
  process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED === "1" &&
  !!process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND &&
  (process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN?.length ?? 0) >= 32;
