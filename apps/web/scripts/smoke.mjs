import assert from "node:assert/strict";

const base = process.argv[2] ?? "http://127.0.0.1:3017";
const places = ["home", "work", "gather", "industrial", "retail", "public"];

async function html(path, expectedStatus = 200) {
  const response = await fetch(new URL(path, base));
  assert.equal(response.status, expectedStatus, path);
  return await response.text();
}

const routes = [
  ["/", "Room for", "/spaces", "/studio", "/living-systems", "/start-a-space"],
  ["/spaces", "Different places", "Generated concept", ...places.map(place => "/spaces/" + place)],
  ["/studio", "Read the courtyard", "reading-lens", "Movement"],
  ["/living-systems", "Illustrative care sequence", 'id="handover"', 'id="observation"', 'id="follow-up"', 'id="history"', "/start-a-space?topic=care"],
  ["/about", 'id="selected-works"', "awaiting approved photographs"],
  ["/start-a-space", "Prepare draft", "Nothing is sent or stored", "Care &amp; support"],
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
assert.ok((await html("/study/scene")).includes("Room for"));
assert.ok((await html("/study/navigation")).includes("A Space"));
assert.ok((await html("/spaces/unknown", 404)).includes("Take another path."));

for (const name of ["hero", "studio", "care", ...places]) {
  for (const size of ["", "-small"]) {
    const path = "/field/" + name + size + ".webp";
    const response = await fetch(new URL(path, base));
    assert.equal(response.status, 200, path);
    assert.match(response.headers.get("content-type") ?? "", /^image\/webp/, path);
    const bytes = (await response.arrayBuffer()).byteLength;
    assert.ok(bytes > 15_000 && bytes < (size ? 140_000 : 450_000), path + " size: " + bytes);
  }
}

console.log("Passed: 12 public pages, 7 contextual briefs, 2 study URLs, 404 and 18 responsive WebP assets.");
