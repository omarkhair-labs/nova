#!/usr/bin/env node
/** No external calls: production pages are available without approval flags. */
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import path from "node:path";

const url = pathToFileURL(path.join(process.cwd(), "app", "publication.ts")).href;
const scenarios = [
  ["Production opens site and keeps its configured Inbox", "production", "1", "https://example.invalid", "x".repeat(48), [true, true]],
  ["Production still opens site if Inbox is switched off", "production", "0", "https://example.invalid", "x".repeat(48), [true, false]],
  ["Production still opens if Inbox token is missing", "production", "1", "https://example.invalid", "", [true, false]],
  ["Preview never accepts real Inbox submissions", "preview", "1", "https://example.invalid", "x".repeat(48), [false, false]],
  ["Local dev is not a live Inbox", "", "1", "https://example.invalid", "x".repeat(48), [false, false]],
];
for (const [i,s] of scenarios.entries()) {
  const [label, env, enabled, backend, token, expected] = s;
  process.env.VERCEL_ENV = env;
  process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED = enabled;
  process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND = backend;
  process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN = token;
  process.env.PLANTUPIA_PUBLICATION_APPROVED = "0"; // ignored by design
  const m = await import(url + "?mode=" + i);
  assert.deepEqual([m.isProductionSite,m.websiteInboxOperational],expected,label);
  console.log("PASS "+label);
}
console.log("5/5 independent site and Inbox modes passed");
