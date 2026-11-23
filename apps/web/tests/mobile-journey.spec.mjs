import { test, expect } from "@playwright/test";

/**
 * Plantupia mobile journey acceptance.
 * Tests real navigation semantics and phone-size layout, not screenshots mistaken
 * for art-direction approval. Company image assets are verified on deployed QA.
 */
test.use({ channel: "chrome" });

for (const width of [320, 360, 390, 430]) {
  test.describe("Plantupia authored mobile journey — " + width + "px", () => {
    test.use({ baseURL: "http://127.0.0.1:3017", viewport: { width, height: 844 } });

    test("image-led World Index opens, stays within the phone and navigates", async ({ page }) => {
      await page.goto("/");
      const menuButton = page.getByRole("button", { name: "Open Plantupia world index" });
      await expect(menuButton).toBeVisible();
      await expect(menuButton.locator(".si-trigger-word")).toBeVisible();
      const triggerSize = await menuButton.boundingBox();
      expect(triggerSize?.height).toBeGreaterThanOrEqual(44);

      await menuButton.click();
      const menu = page.locator("dialog.si-overlay");
      await expect(menu).toBeVisible();
      await expect(menu.locator(".si-atlas")).toBeVisible();
      await expect(menu.locator(".si-link-image")).toHaveCount(6);
      await expect(menu.locator(".si-mobile-shop")).toBeVisible();
      const dimensions = await menu.evaluate(element => ({
        content: element.scrollWidth,
        viewport: element.clientWidth,
      }));
      expect(dimensions.content - dimensions.viewport, "Index horizontal overflow").toBeLessThanOrEqual(1);
      const closeSize = await menu.locator(".si-close").boundingBox();
      expect(closeSize?.height).toBeGreaterThanOrEqual(44);
      await menu.locator('a[href="/spaces/projects"]').click();
      await expect(page).toHaveURL(/\/spaces\/projects$/);
      await expect(page.locator(".pa-wall .pa-project")).toHaveCount(6);
    });

    test("opening, projects, main chapters, Shop and Contact do not overflow", async ({ page }) => {
      for (const route of ["/", "/spaces", "/spaces/projects", "/spaces/projects/audi-cairo", "/studio", "/living-systems", "/about", "/shop", "/start-a-space"]) {
        const response = await page.goto(route);
        expect(response?.status(), "Route " + route).toBe(200);
        const dimensions = await page.evaluate(() => ({
          content: document.documentElement.scrollWidth,
          viewport: window.innerWidth,
        }));
        expect(dimensions.content - dimensions.viewport, "Overflow on " + route).toBeLessThanOrEqual(1);
        await expect(page.getByRole("button", { name: "Open Plantupia world index" })).toBeVisible();
      }
    });

    test("mobile-first entry points and touch targets preserve working journeys", async ({ page }) => {
      await page.goto("/");
      const mobilePaths = page.locator(".ps-mobile-paths");
      await expect(mobilePaths).toBeVisible();
      await expect(mobilePaths.locator('a[href="/spaces/projects"]')).toBeVisible();
      await expect(mobilePaths.locator('a[href="/start-a-space"]')).toBeVisible();

      await page.goto("/spaces");
      const firstEnvironment = page.locator(".sx-picker button").first();
      await expect(firstEnvironment).toBeVisible();
      const environmentSize = await firstEnvironment.boundingBox();
      expect(environmentSize?.height).toBeGreaterThanOrEqual(44);

      await page.goto("/shop");
      await expect(page.locator(".sr-shop-path")).toHaveCount(3);
      for (const path of await page.locator(".sr-shop-path").all()) {
        const bounds = await path.boundingBox();
        expect(bounds?.height).toBeGreaterThanOrEqual(44);
      }

      await page.goto("/start-a-space");
      const care = page.getByRole("button", { name: "Care & support" });
      const careSize = await care.boundingBox();
      expect(careSize?.height).toBeGreaterThanOrEqual(44);
      await care.click();
      await expect(care).toHaveAttribute("aria-pressed", "true");
      await expect(page.getByRole("button", { name: /Send your message/ })).toBeDisabled();
    });
  });
}
