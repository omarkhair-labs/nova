import { test, expect } from "@playwright/test";

test.use({ channel: "chrome" });

for (const variant of [
  { name: "phone", viewport: { width: 390, height: 844 } },
  { name: "desktop", viewport: { width: 1440, height: 900 } },
]) {
  test.describe("Plantupia visitor closure — " + variant.name, () => {
    test.use({ viewport: variant.viewport, baseURL: "http://127.0.0.1:3017" });

    test("World Index opens, navigates, and browser Back works", async ({ page }) => {
      await page.goto("/");
      const trigger = page.getByRole("button", { name: "Open Plantupia world index" });
      await expect(trigger).toBeVisible();
      await trigger.click();
      const dialog = page.locator("dialog.si-overlay");
      await expect(dialog).toBeVisible();
      await dialog.locator('a[href="/spaces"]').first().click();
      await expect(page).toHaveURL(/\/spaces$/);
      await page.goBack();
      await expect(page).toHaveURL(/\/$/);
    });

    test("Studio keeps four directly selectable design stages", async ({ page }) => {
      await page.goto("/studio");
      const stages = page.locator(".pc-study-nav button");
      await expect(stages).toHaveCount(4);
      await stages.nth(2).click();
      await expect(stages.nth(2)).toHaveAttribute("aria-pressed", "true");
      await stages.nth(0).click();
      await expect(stages.nth(0)).toHaveAttribute("aria-pressed", "true");
    });

    test("Living Systems labels planned vs recorded and changes entry", async ({ page }) => {
      await page.goto("/living-systems");
      const entries = page.locator(".lc-time-button");
      await expect(entries).toHaveCount(4);
      await entries.nth(2).click();
      await expect(entries.nth(2)).toHaveAttribute("aria-pressed", "true");
      await expect(page.locator('[data-record-state="planned"]')).toHaveCount(1);
      await expect(page.locator(".lc-planned-note")).toBeVisible();
    });

    test("Shop is honest about unavailable published products", async ({ page }) => {
      await page.goto("/shop");
      await expect(page.getByText("The live product catalogue is currently unavailable.")).toBeVisible();
      await expect(page.locator(".sr-product")).toHaveCount(0);
      const statusLink = page.locator('.sr-shop-path-primary[href="#shop-collection"]');
      await expect(statusLink).toBeVisible();
      await expect(page.locator("#sr-material-title")).toHaveCount(0);
    });

    test("Shop chapter actions do not point to a missing section", async ({ page }) => {
      await page.goto("/shop");
      const links = page.locator(".sr-shop-path");
      await expect(links).toHaveCount(3);
      for (const link of await links.all()) {
        const href = await link.getAttribute("href");
        expect(href).toBeTruthy();
        if (href.startsWith("#")) {
          await expect(page.locator(href)).toHaveCount(1);
        } else {
          const response = await page.request.get(href);
          expect(response.status(), "Shop CTA target " + href).toBe(200);
        }
      }
    });

    test("Unapproved Contact never enables submitting an enquiry", async ({ page }) => {
      await page.goto("/start-a-space");
      const send = page.getByRole("button", { name: /Send your message/ });
      await expect(send).toBeDisabled();
      await page.getByRole("button", { name: "Care & support" }).click();
      await expect(send).toBeDisabled();
      await expect(page.getByText("Nothing is sent or stored.", { exact: false }).first()).toBeVisible();
    });

    test("Seven public chapter routes have no horizontal overflow", async ({ page }) => {
      for (const route of ["/", "/spaces", "/studio", "/living-systems", "/about", "/shop", "/start-a-space"]) {
        const response = await page.goto(route);
        expect(response?.status(), "HTTP for " + route).toBe(200);
        const extra = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
        expect(extra, "horizontal overflow " + route).toBeLessThanOrEqual(1);
      }
    });
  });
}
