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

// Lightweight in-app notification. Only an aggregate count; no customer PII.
// No email, FCM push, payment or courier side effects.
const commerceAlert = document.querySelector("[data-alert-url]");
if (commerceAlert) {
  const updateCommerceCount = async () => {
    if (document.hidden) return;
    try {
      const response = await fetch(commerceAlert.dataset.alertUrl, {
        credentials: "same-origin", headers: { "Accept": "application/json" },
        cache: "no-store"
      });
      if (!response.ok) return;
      const state = await response.json();
      if (!state.qa_only || !Number.isSafeInteger(state.new_requests)) return;
      const label = commerceAlert.querySelector("[data-alert-count]");
      if (label) label.textContent = String(Math.max(0, state.new_requests));
      commerceAlert.classList.toggle("has-new", state.new_requests > 0);
    } catch (_) { /* keep the previous number until the next check */ }
  };
  commerceAlert.classList.toggle("has-new",
    Number(commerceAlert.querySelector("[data-alert-count]")?.textContent || 0) > 0);
  setInterval(updateCommerceCount, 30000);
  document.addEventListener("visibilitychange", updateCommerceCount);
}
