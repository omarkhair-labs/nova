"use strict";
// Progressive enhancement only. Authentication, validation and approvals are server enforced.
for (const form of document.forms) {
  if (form.method.toLowerCase() !== "post") continue;
  form.addEventListener("submit", event => {
    const button = event.submitter;
    form.setAttribute("aria-busy", "true");
    if (button) {
      button.dataset.originalLabel = button.textContent;
      button.disabled = true;
      button.textContent = "Working…";
    }
  });
}
window.addEventListener("pageshow", () => {
  for (const form of document.forms) form.removeAttribute("aria-busy");
  for (const button of document.querySelectorAll("button[data-original-label]")) {
    button.disabled = false;
    button.textContent = button.dataset.originalLabel;
  }
});
const secret = document.getElementById("one-time-secret");
if (secret) {
  const input = document.getElementById("handover-code");
  const reveal = document.getElementById("reveal-code");
  reveal.addEventListener("click", () => {
    input.type = input.type === "password" ? "text" : "password";
    reveal.textContent = input.type === "password" ? "Reveal manual code" : "Hide manual code";
  });
  const returnUrl = secret.dataset.returnUrl;
  window.addEventListener("pagehide", () => { input.value = ""; secret.replaceChildren(); });
  window.addEventListener("pageshow", event => { if (event.persisted) window.location.replace(returnUrl); });
}
