import assert from "node:assert/strict";

const base = process.argv[2] ?? "http://127.0.0.1:3017";
const places = ["home", "work", "gather", "industrial", "retail", "public"];

async function html(path, expectedStatus = 200) {
  const response = await fetch(new URL(path, base));
  assert.equal(response.status, expectedStatus, path);
  return await response.text();
}

const routes = [
  ["/", "Where life", 'id="planting"', 'id="return"', 'href="#planting"', 'href="#return"', "/spaces", "/studio", "/living-systems", "/start-a-space"],
  ["/spaces", "can grow.", "Generated concepts", ...places.map(place => "/spaces/" + place)],
  ["/studio", "Read the courtyard", "reading-lens", "Movement", "/#planting"],
  ["/living-systems", "Illustrative care sequence", 'id="handover"', 'id="observation"', 'id="follow-up"', 'id="history"', "/start-a-space?topic=care", "/#return"],
  ["/about", 'id="selected-works"', "awaits approved photographs"],
  ["/start-a-space", "Review draft", "Nothing is sent or stored", "Care &amp; support", "Reply details", "Your name", "Email", "Phone", "Organisation"],
];

for (const [path, ...expect] of routes) {
  const markup = await html(path);
  for (const text of ['id="main"', ...expect]) {
    assert.ok(markup.includes(text), path + " is missing " + text);
  }
}

for (const place of places) {
  const markup = await html("/spaces/" + place);
  assert.ok(markup.includes("/field/" + place + ".webp"));
  assert.ok(markup.includes("/start-a-space?space=" + place));
  assert.ok(markup.includes("Generated concept"));

  const brief = await html("/start-a-space?space=" + place);
  assert.match(brief, new RegExp('<option[^>]*value="' + place + '"[^>]*selected'));
}

const care = await html("/start-a-space?topic=care");
assert.match(care, /<input(?=[^>]*value="care")(?=[^>]*checked)[^>]*>/);
assert.ok((await html("/study/scene")).includes("Where life"));
assert.ok((await html("/study/navigation")).includes("A Space"));
assert.ok((await html("/spaces/unknown", 404)).includes("Take another path."));

for (const name of ["threshold", "hero", "return", "studio", ...places]) {
  for (const size of ["", "-small"]) {
    const path = "/field/" + name + size + ".webp";
    const response = await fetch(new URL(path, base));
    assert.equal(response.status, 200, path);
    assert.match(response.headers.get("content-type") ?? "", /^image\/webp/, path);
    const bytes = (await response.arrayBuffer()).byteLength;
    assert.ok(bytes > 15_000 && bytes < (size ? 140_000 : 450_000), path + " size: " + bytes);
  }
}

const map = await fetch(new URL("/field/courtyard-map.webp", base));
assert.equal(map.status, 200);
assert.match(map.headers.get("content-type") ?? "", /^image\/webp/);
assert.ok((await map.arrayBuffer()).byteLength < 500_000);

console.log("Passed: 12 public pages, 7 contextual briefs, 2 study URLs, 404 and 20 responsive WebP assets and the map.");
