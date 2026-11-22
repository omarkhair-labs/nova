/**
 * Production build gate. Does not independently verify copyright or approval.
 * The team must supply a signed-off approval record before public release.
 */
import fs from "node:fs";
import path from "node:path";

const stagingProd = process.env.VERCEL_ENV === "production";
const approved = process.env.PLANTUPIA_PUBLICATION_APPROVED === "1";

if (!stagingProd || !approved) {
  console.log("[release-gate] Build in non-publication mode. Public promotion is NOT authorized.");
  process.exit(0);
}

const root = process.cwd();
const manifestPath = path.join(root,"production-publication-approval.json");
if (!fs.existsSync(manifestPath)) {
  throw new Error("[release-gate] Missing production-publication-approval.json with confirmed source-image permissions and company approval.");
}
const approval = JSON.parse(fs.readFileSync(manifestPath,"utf8"));
if (typeof approval.approvedBy !== "string" || !approval.approvedBy.trim() ||
    typeof approval.approvedAt !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(approval.approvedAt) ||
    !Array.isArray(approval.approvedAssets)) {
  throw new Error("[release-gate] Approval manifest must record approver, date and approved asset names.");
}
const source = fs.readFileSync(path.join(root,"app","api","private-arrival","[asset]","route.ts"),"utf8");
const matched = source.match(/const allowed = new Set\((\[[^\]]+\])\)/);
if (!matched) throw new Error("[release-gate] Cannot parse private asset allowlist.");
const required = JSON.parse(matched[1]);
const approvedSet = new Set(approval.approvedAssets);
const missingApprovals = required.filter(name => !approvedSet.has(name));
const missingFiles = required.filter(name => {
  const file = path.join(root,"public","private-review-assets",name);
  return !fs.existsSync(file) || fs.statSync(file).size === 0;
});
if (missingApprovals.length || missingFiles.length) {
  throw new Error("[release-gate] Missing image/font approvals: " + missingApprovals.length + "; missing production asset files: " + missingFiles.length + ". Publication blocked.");
}
if (process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED !== "1" ||
    !process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND ||
    (process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN?.length ?? 0) < 32) {
  throw new Error("[release-gate] Website inbox is not configured and enabled. Publication blocked.");
}
console.log("[release-gate] Technical release preflight passed for " + required.length + " assets; publication rights and inbox acceptance require human review.");
