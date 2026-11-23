#!/usr/bin/env node
/**
 * Plantupia public website closure smoke, in non-publication mode.
 * No production, customer data, private proposals or real inbox required.
 * Run against a locally built and started Next.js server (default port 3017).
 */
const base = process.env.PLANTUPIA_SMOKE_BASE_URL || "http://127.0.0.1:3017";
const checks = [];
async function request(path, init) {
  const response = await fetch(new URL(path, base), {
    ...init,
    redirect: "manual",
    signal: AbortSignal.timeout(12000),
    headers: { Accept: "text/html", ...init?.headers },
  });
  return { response, body: await response.text() };
}
function check(label, truth, detail = "") {
  checks.push({ label, pass: Boolean(truth), detail });
  console.log((truth ? "PASS" : "FAIL") + " " + label + (detail ? " — " + detail : ""));
}
async function page(path, phrases = []) {
  const { response, body } = await request(path);
  check("Public chapter " + path, response.status === 200, "HTTP " + response.status);
  if (response.status === 200) {
    check("HTML document " + path, body.includes("<html") && body.includes("Plantupia"));
    for (const phrase of phrases) check("Chapter text " + path + " / " + phrase, body.includes(phrase));
    check("Unapproved document is not indexed " + path, body.includes("noindex"));
  }
  return body;
}
try {
  const home = await page("/", ["Plantupia"]);
  for (const path of ["/spaces", "/studio", "/living-systems", "/about", "/shop", "/start-a-space"]) {
    await page(path);
  }
  check("Experience World Index reachable without JS", home.includes("Six chapters. One living world."));
  for (const path of ["/spaces", "/studio", "/living-systems", "/about", "/start-a-space", "/shop"]) {
    check("World navigation includes " + path, home.includes('href="' + path + '"'));
  }

  const projects = [
    "/spaces/projects", "/spaces/projects/mazal", "/spaces/projects/em-sherif",
    "/spaces/projects/el-hallab", "/spaces/projects/pixels-mall",
    "/spaces/projects/audi-cairo", "/spaces/projects/villa-7",
  ];
  for (const path of projects) {
    const { response } = await request(path);
    check("Project chapter is visitable " + path, response.status === 200, "HTTP " + response.status);
  }

  const privateAsset = await request("/api/private-arrival/archive-pixels-p08.webp",
    { headers: { Accept: "image/webp" } });
  check("Missing source asset does not redirect to a broken image", privateAsset.response.status === 404,
    "HTTP " + privateAsset.response.status);
  const invalidCover = await request("/api/commerce/products/not-a-uuid/cover",
    { headers: { Accept: "image/webp" } });
  check("Invalid SKU image not served", invalidCover.response.status === 404,
    "HTTP " + invalidCover.response.status);

  const post = await request("/api/website/enquiries", {
    method: "POST",
    headers: { "Content-Type": "application/json", Origin: base },
    body: "{}",
  });
  check("Unapproved lead capture disabled", post.response.status === 503,
    "HTTP " + post.response.status);

  const robots = await request("/robots.txt");
  check("Robots excludes unapproved preview",
    robots.response.status === 200 && robots.body.includes("Disallow: /"));
  const sitemap = await request("/sitemap.xml");
  check("Sitemap does not publish case URLs without approval",
    sitemap.response.status === 200 && !sitemap.body.includes("/spaces/projects/mazal"));

  const errors = checks.filter(x => !x.pass);
  console.log("\n" + (checks.length - errors.length) + "/" + checks.length + " passed");
  if (errors.length) process.exitCode = 1;
} catch (error) {
  console.error("Closure smoke could not complete:", error);
  process.exitCode = 1;
}
