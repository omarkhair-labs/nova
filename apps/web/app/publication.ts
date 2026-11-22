/** Server-only release gate. A production build is not the same as approved publication. */
export const publicationApproved =
  process.env.VERCEL_ENV === "production" &&
  process.env.PLANTUPIA_PUBLICATION_APPROVED === "1";

/** Local and authenticated preview may display source review materials.
 * Public production can do so only after editorial clearance and the asset
 * manifest have been independently reviewed before promotion.
 */
export const reviewAssetsAllowed =
  process.env.VERCEL_ENV === "preview" ||
  publicationApproved ||
  (process.env.NODE_ENV === "development" &&
    !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR);
