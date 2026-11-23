/** Server-only release gate. A production build is not the same as approved publication. */
export const publicationApproved =
  process.env.VERCEL_ENV === "production" &&
  process.env.PLANTUPIA_PUBLICATION_APPROVED === "1";

/**
 * Preserve the independently configured website Inbox from the existing live
 * release. Approval to publish private proposal images is NOT a prerequisite
 * for accepting a legitimate message. Both flows remain independently gated:
 * private image rights above; verified HTTPS backend/token and the original
 * Inbox enable switch below. Preview still cannot accept real enquiries.
 */
export const websiteInboxOperational =
  process.env.VERCEL_ENV === "production" &&
  process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED === "1" &&
  !!process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND &&
  (process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN?.length ?? 0) >= 32;

/** Local and authenticated preview may display source review materials.
 * Public production can do so only after editorial clearance and the asset
 * manifest have been independently reviewed before promotion.
 */
export const reviewAssetsAllowed =
  process.env.VERCEL_ENV === "preview" ||
  publicationApproved ||
  (process.env.NODE_ENV === "development" &&
    !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR);
