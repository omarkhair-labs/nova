"use strict";
// Progressive enhancement: the selected ZIP remains in the browser across
// preview and confirm; no storage, tokens or customer data are sent elsewhere.
(() => {
  const form = document.querySelector("[data-commerce-import]");
  if (!form) return;
  const file = form.querySelector('input[name="upload"]');
  const previewButton = document.getElementById("commerce-import-preview");
  const saveButton = document.getElementById("commerce-import-save");
  const action = document.getElementById("commerce-import-action");
  const digest = document.getElementById("commerce-import-digest");
  const confirm = document.getElementById("commerce-import-confirm");
  const status = document.getElementById("commerce-import-message");
  const results = document.getElementById("commerce-import-results");
  let busy = false;

  function reset() {
    digest.value = "";
    confirm.value = "";
    saveButton.disabled = true;
    status.textContent = "Validate the selected ZIP to see the product and SKU preview.";
    results.replaceChildren();
  }
  file.addEventListener("change", reset);

  async function send(phase) {
    if (busy || !file.files.length) {
      if (!file.files.length) status.textContent = "Select a ZIP file first.";
      return;
    }
    if (phase === "import" && !digest.value) {
      status.textContent = "Validate this file before confirming its import.";
      return;
    }
    busy = true;
    action.value = phase;
    confirm.value = phase === "import" ? "1" : "";
    previewButton.disabled = true;
    saveButton.disabled = true;
    status.textContent = phase === "preview" ? "Checking the workbook and images…" : "Saving commercial drafts…";
    try {
      const response = await fetch(form.action, {
        method: "POST", body: new FormData(form),
        credentials: "same-origin", cache: "no-store",
        headers: { "Accept": "application/json" }
      });
      let data;
      try { data = await response.json(); }
      catch (_) { throw new Error("The server returned an unexpected response. Please sign in again if your session expired."); }
      if (!response.ok || !data.ok) throw new Error(data.error || "Import validation failed.");
      if (phase === "import") {
        status.textContent = "Imported as unpublished drafts. Opening products…";
        window.location.assign(data.redirect);
        return;
      }
      digest.value = data.digest;
      status.textContent = data.notice;
      const list = document.createElement("ul");
      list.className = "import-preview-list";
      for (const item of data.preview) {
        const row = document.createElement("li");
        const title = document.createElement("strong");
        title.textContent = item.name + " · " + item.key;
        const details = document.createElement("span");
        details.textContent = item.sku_count + " SKUs · " + item.photos + " photos";
        row.append(title, details);
        list.append(row);
      }
      results.replaceChildren(list);
      if (data.products > data.preview.length) {
        const more = document.createElement("p");
        more.className = "fine";
        more.textContent = "Showing the first " + data.preview.length + " of " + data.products + " products.";
        results.append(more);
      }
      saveButton.disabled = false;
    } catch (error) {
      status.textContent = error.message || "Could not validate the selected file.";
      if (phase !== "import") digest.value = "";
    } finally {
      busy = false;
      previewButton.disabled = false;
      if (digest.value) saveButton.disabled = false;
    }
  }
  form.addEventListener("submit", event => {
    event.preventDefault();
    send("preview");
  });
  saveButton.addEventListener("click", () => send("import"));
})();
