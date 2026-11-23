#!/usr/bin/env node
/**
 * Pure, synthetic release-mode checks: never contacts a backend or sends
 * enquiries. Protects the previously working production Inbox from being
 * accidentally tied to unrelated permissions for private project photos.
 *
 * Run in apps/web with Node >= 22.6 (native .ts module support).
 */
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import path from "node:path";

const moduleURL = pathToFileURL(path.join(process.cwd(), "app", "publication.ts")).href;
const scenarios = [
  {
    name: "Existing production Inbox survives unpublished proposal assets",
    env: "production", approval: "0", inbox: "1", backend: "https://example.invalid",
    token: "x".repeat(48), nodeEnv: "production", expected: [false, true, false],
  },
  {
    name: "Authorized production photo publication and Inbox are independent",
    env: "production", approval: "1", inbox: "1", backend: "https://example.invalid",
    token: "x".repeat(48), nodeEnv: "production", expected: [true, true, true],
  },
  {
    name: "Production without Inbox switch stays closed, even with photo approval",
    env: "production", approval: "1", inbox: "0", backend: "https://example.invalid",
    token: "x".repeat(48), nodeEnv: "production", expected: [true, false, true],
  },
  {
    name: "Production with short Inbox token stays closed",
    env: "production", approval: "0", inbox: "1", backend: "https://example.invalid",
    token: "short", nodeEnv: "production", expected: [false, false, false],
  },
  {
    name: "Private Preview can display review images, but never send messages",
    env: "preview", approval: "1", inbox: "1", backend: "https://example.invalid",
    token: "x".repeat(48), nodeEnv: "production", expected: [false, false, true],
  },
  {
    name: "Local dev with an explicitly supplied review folder, Inbox disabled",
    env: "", approval: "0", inbox: "1", backend: "https://example.invalid",
    token: "x".repeat(48), nodeEnv: "development", privateDir: "/tmp/synthetic-only",
    expected: [false, false, true],
  },
];
let passed = 0;
for (const [i, scenario] of scenarios.entries()) {
  process.env.VERCEL_ENV = scenario.env;
  process.env.PLANTUPIA_PUBLICATION_APPROVED = scenario.approval;
  process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED = scenario.inbox;
  process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND = scenario.backend;
  process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN = scenario.token;
  process.env.NODE_ENV = scenario.nodeEnv;
  if (scenario.privateDir) process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR = scenario.privateDir;
  else delete process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR;
  const mod = await import(moduleURL + "?release-mode=" + i);
  const actual = [
    mod.publicationApproved,
    mod.websiteInboxOperational,
    mod.reviewAssetsAllowed,
  ];
  assert.deepEqual(actual, scenario.expected, scenario.name);
  console.log("PASS " + scenario.name);
  passed++;
}
console.log(passed + "/" + scenarios.length + " independent release-mode checks passed");
