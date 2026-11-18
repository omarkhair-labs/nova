"use client";

import { useMemo, useState } from "react";
import SiteIndex from "./SiteIndex";
import type { PublicCatalogResult, PublicCommerceSku } from "./shop/catalog";
import "./shop-experience.css";

type ShelfKind = "all" | PublicCommerceSku["kind"];
type Product = {
  id: string;
  name: string;
  description: string;
  kind: PublicCommerceSku["kind"];
  hasImage: boolean;
  alt: string;
  variants: PublicCommerceSku[];
};

const shelves: { id: ShelfKind; name: string; count?: string }[] = [
  { id: "all", name: "All objects" },
  { id: "plant", name: "Living plants" },
  { id: "planter", name: "Planters" },
  { id: "care", name: "Care objects" },
];
const nouns: Record<PublicCommerceSku["kind"], string> = {
  plant: "THE LIVING",
  planter: "THE VESSELS",
  care: "THE RITUAL",
};
function price(value: string): string {
  return Number(value).toLocaleString("en-EG", {
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }) + " EGP";
}

export default function ShopExperience({ catalog }: { catalog: PublicCatalogResult }) {
  const [active, setActive] = useState<ShelfKind>("all");
  const products = useMemo(() => {
    const grouped = new Map<string, Product>();
    for (const sku of catalog.items) {
      let item = grouped.get(sku.product_id);
      if (!item) {
        item = {
          id: sku.product_id,
          name: sku.name,
          description: sku.description,
          kind: sku.kind,
          hasImage: sku.has_image,
          alt: sku.image_alt || sku.name,
          variants: [],
        };
        grouped.set(item.id, item);
      }
      item.variants.push(sku);
    }
    return [...grouped.values()];
  }, [catalog.items]);
  const visible = active === "all" ? products : products.filter(item => item.kind === active);

  return (
    <div className="sh-world">
      <a className="sh-skip" href="#shop-collection">Skip to the collection</a>
      <header className="sh-top">
        <a href="/" className="sh-brand">◕ Plantupia</a>
        <SiteIndex current={null} tone="dark"/>
        <a href="/start-a-space?topic=general" className="sh-contact">Ask the studio <span aria-hidden="true">↗</span></a>
      </header>

      <main>
        <section className="sh-opening" aria-labelledby="shop-title">
          <div className="sh-edition">
            <p className="sh-micro">PLANTUPIA / AN EDITED COLLECTION <span>01 — OBJECTS</span></p>
            <div className="sh-opening-copy">
              <span className="sh-side-number" aria-hidden="true">P—01</span>
              <h1 id="shop-title">Things to<br/><em>live with.</em></h1>
              <p>Plants, vessels and the small materials of care. An extension of the spaces we make — selected with intention, kept alive through attention.</p>
            </div>
            <div className="sh-opening-foot">
              <span>OBJECT / PLACE / CONTINUITY</span>
              <a href="#shop-collection">Explore the collection <span aria-hidden="true">↓</span></a>
            </div>
          </div>
          <div className="sh-study" aria-label="A conceptual architectural study, not a product photograph">
            <div className="sh-study-head"><span>THE LIVING OBJECT</span><span>FIELD STUDY / 001</span></div>
            <svg viewBox="0 0 600 720" fill="none" aria-hidden="true">
              <g stroke="currentColor" strokeWidth="1.15">
                <path opacity=".23" d="M40 652H560M300 50V684M95 80H505M95 83V652M505 83V652"/>
                <path opacity=".34" d="M95 652L300 594L505 652M95 652L95 224L300 146L505 224V652M95 224L300 302L505 224M300 302V594"/>
                <path opacity=".64" d="M194 547V454H406V547C406 608 365 646 300 646C235 646 194 608 194 547Z"/>
                <path strokeWidth="2.2" d="M300 453C289 362 295 250 329 161M296 376C215 350 178 281 183 211C245 226 290 267 296 376ZM302 340C364 326 426 269 437 202C359 213 316 259 302 340ZM304 264C266 225 262 179 288 116C324 155 328 208 304 264Z"/>
                <path opacity=".7" d="M195 503H405M195 547H405M213 583H386"/>
                <circle cx="300" cy="376" r="220" opacity=".18" strokeDasharray="3 9"/>
                <path d="M530 192H580M530 377H580M530 560H580M30 192H80M30 377H80M30 560H80" opacity=".45"/>
              </g>
              <g fill="currentColor" fontFamily="monospace" fontSize="11" opacity=".64">
                <text x="49" y="178">01</text><text x="49" y="364">02</text><text x="49" y="551">03</text>
                <text x="438" y="680">NOT TO SCALE</text>
              </g>
            </svg>
            <div className="sh-study-foot"><span>DRAWN CONCEPT / NOT AN OFFERED PRODUCT</span><span>PLANTUPIA / 2026</span></div>
          </div>
        </section>

        <section className="sh-collection" id="shop-collection" aria-labelledby="collection-title">
          <div className="sh-collection-head">
            <p className="sh-micro">02 / THE COLLECTION <span>REVIEWED & PUBLISHED OBJECTS ONLY</span></p>
            <h2 id="collection-title">Considered.<br/><em>Not crowded.</em></h2>
            <p>Only reviewed, published products appear here. Prices and availability are confirmed when you place an order in the Plantupia app.</p>
          </div>
          <nav className="sh-filters" aria-label="Filter by product type">
            {shelves.map(shelf => (
              <button key={shelf.id} type="button" className={active === shelf.id ? "sh-filter sh-selected" : "sh-filter"} aria-pressed={active === shelf.id} onClick={() => setActive(shelf.id)}>
                {shelf.name}<span>{shelf.id === "all" ? String(products.length).padStart(2, "0") : String(products.filter(p => p.kind === shelf.id).length).padStart(2, "0")}</span>
              </button>
            ))}
          </nav>

          {catalog.state !== "available" ? (
            <div className="sh-unavailable" role="status">
              <span className="sh-unavailable-mark" aria-hidden="true">◌</span>
              <div>
                <p className="sh-micro">THE COLLECTION / NOT YET PUBLIC</p>
                <h3>{catalog.state === "empty" ? "Good things deserve a proper introduction." : "The published collection is not available right now."}</h3>
                <p>{catalog.state === "empty" ? "We’ll share plants and objects here once their photos, options and prices have been confirmed. For now, tell us what you need." : "We can’t show the collection right now. Tell us what you need, and the Plantupia team can help."}</p>
                <a href="/start-a-space?topic=general">Talk about what you need <span aria-hidden="true">↗</span></a>
              </div>
            </div>
          ) : visible.length === 0 ? (
            <div className="sh-unavailable" role="status"><div><h3>No published objects in this category.</h3><p>Choose another shelf to see what is currently available.</p></div></div>
          ) : (
            <div className="sh-objects">
              {visible.map((product, i) => (
                <article className="sh-object" key={product.id}>
                  <div className="sh-object-media">
                    <div className="sh-object-media-label">{nouns[product.kind]} <span>{String(i + 1).padStart(2, "0")}/{String(visible.length).padStart(2, "0")}</span></div>
                    <div className="sh-object-photo">
                      <span className="sh-no-photo">NO APPROVED PHOTO AVAILABLE</span>
                      {product.hasImage && (
                        <img
                          src={"/api/commerce/products/" + product.id + "/cover/"}
                          alt={product.alt} loading="lazy"
                          onError={event => { event.currentTarget.style.display = "none"; }}
                        />
                      )}
                    </div>
                  </div>
                  <div className="sh-object-copy">
                    <span className="sh-micro">PRODUCT REFERENCE / {product.kind.toUpperCase()}</span>
                    <h3>{product.name}</h3>
                    {product.description && <p className="sh-object-description">{product.description}</p>}
                    <div className="sh-variants" aria-label={"Current published options for " + product.name}>
                      {product.variants.map(sku => (
                        <div className="sh-variant" key={sku.sku_id}>
                          <div>
                            <span className="sh-variant-name">{sku.option || "Standard"}</span>
                            <span className="sh-stock">{sku.in_stock ? "IN STOCK AT LAST CHECK" : "CURRENTLY UNAVAILABLE"}</span>
                          </div>
                          <strong>{price(sku.price)}</strong>
                        </div>
                      ))}
                    </div>
                    <a className="sh-ask" href={"/start-a-space?topic=general&item=" + encodeURIComponent(product.name)}>
                      Ask about this object <span aria-hidden="true">↗</span>
                    </a>
                    <p className="sh-object-note">This is product discovery, not a web checkout. An enquiry does not place or reserve an order.</p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="sh-continuity" aria-labelledby="sh-continuity-title">
          <div><p className="sh-micro">03 / AFTER THE OBJECT</p><h2 id="sh-continuity-title">A plant does not end<br/>at delivery.</h2></div>
          <div className="sh-continuity-notes">
            <p><span>01 / CHOOSE</span> One actual product and its published variant.</p>
            <p><span>02 / PLACE</span> A real location, with conditions and people.</p>
            <p><span>03 / RETURN</span> Recorded care, honest observations and living history in the customer app.</p>
            <a href="/living-systems">Explore the living system <span aria-hidden="true">↗</span></a>
          </div>
        </section>
      </main>
      <footer className="sh-footer"><a href="/">PLANTUPIA / THE WORLD</a><span>PUBLIC COLLECTION PREVIEW</span><a href="/start-a-space?topic=general">GET IN TOUCH ↗</a></footer>
    </div>
  );
}
