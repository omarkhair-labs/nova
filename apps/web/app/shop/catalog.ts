/** Server-side only: this is a public *read* of already-published SKU choices. */
export type PublicCommerceSku = {
  sku_id: string;
  product_id: string;
  name: string;
  description: string;
  kind: "plant" | "planter" | "care";
  option: string;
  price: string;
  currency: "EGP";
  in_stock: boolean;
  has_image: boolean;
  image_alt: string;
};
export type PublicCatalogResult = {
  state: "available" | "empty" | "unavailable";
  items: PublicCommerceSku[];
};

export function commerceApiOrigin(): URL | null {
  // The website-inbox backend and the curated catalog may use one deployment.
  // No credential, session token, private URL or query string reaches the browser.
  const value = process.env.PLANTUPIA_PUBLIC_COMMERCE_BACKEND ||
    process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND;
  if (!value) return null;
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || url.username || url.password ||
        url.search || url.hash) return null;
    return url;
  } catch { return null; }
}

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const price = /^\d{1,9}\.\d{2}$/;
const kind = new Set(["plant", "planter", "care"]);
function validSku(candidate: unknown): candidate is PublicCommerceSku {
  if (candidate === null || typeof candidate !== "object" || Array.isArray(candidate)) return false;
  const item = candidate as Record<string, unknown>;
  return (
    typeof item.sku_id === "string" && uuid.test(item.sku_id) &&
    typeof item.product_id === "string" && uuid.test(item.product_id) &&
    typeof item.name === "string" && item.name.length > 0 && item.name.length <= 160 &&
    typeof item.description === "string" && item.description.length <= 3000 &&
    typeof item.kind === "string" && kind.has(item.kind) &&
    typeof item.option === "string" && item.option.length <= 120 &&
    typeof item.price === "string" && price.test(item.price) &&
    item.currency === "EGP" && typeof item.in_stock === "boolean" &&
    typeof item.has_image === "boolean" && typeof item.image_alt === "string" &&
    item.image_alt.length <= 180
  );
}

export async function getPublicCatalog(): Promise<PublicCatalogResult> {
  const base = commerceApiOrigin();
  if (!base) return { state: "unavailable", items: [] };
  try {
    const url = new URL("/api/v1/plantupia/commerce/public/catalog/", base);
    const response = await fetch(url, {
      cache: "no-store", redirect: "error", signal: AbortSignal.timeout(8000),
      headers: { Accept: "application/json" },
    });
    if (!response.ok) return { state: "unavailable", items: [] };
    const body: unknown = await response.json();
    if (!body || typeof body !== "object" || Array.isArray(body)) {
      return { state: "unavailable", items: [] };
    }
    const result = body as Record<string, unknown>;
    if (result.source !== "reviewed_published_commerce_skus" ||
        !Array.isArray(result.results) || result.results.length > 36) {
      return { state: "unavailable", items: [] };
    }
    if (!result.results.every(validSku)) {
      return { state: "unavailable", items: [] };
    }
    const items = result.results as PublicCommerceSku[];
    return { state: items.length ? "available" : "empty", items };
  } catch {
    return { state: "unavailable", items: [] };
  }
}
