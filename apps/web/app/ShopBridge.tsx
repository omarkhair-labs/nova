import "./shop-experience.css";
import "./shop-bridge-photo.css";

/** A quiet seventh doorway, never a fake numbered chapter in the six-part story. */
export default function ShopBridge({ privateReview = false }: { privateReview?: boolean }) {
  return (
    <section className="sh-bridge" id="shop-bridge" data-private-review={privateReview} aria-labelledby="shop-bridge-title">
      <div className="sh-bridge-inner">
        <div>
          <p className="sh-micro">ANOTHER WAY IN / PLANTUPIA OBJECTS</p>
          <h2 id="shop-bridge-title">What stays<br/><em>with you.</em></h2>
          <p>Some projects begin with a whole space. Others begin with one plant, one vessel, or the care that keeps it alive.</p>
          <a href="/shop">Explore the edited collection <span aria-hidden="true">↗</span></a>
        </div>
        {privateReview ? <figure className="sh-bridge-figure sh-bridge-catalogue"><img src="/api/private-arrival/shop-bridge.webp" alt="Two dark sculptural planters with full green Philodendron Xanadu foliage, from the Plantupia hospitality catalogue" loading="lazy"/><figcaption>PHILODENDRON XANADU / HOSPITALITY COLLECTION / CATALOGUE ILLUSTRATION</figcaption></figure> : <div className="sh-bridge-figure" aria-label="An abstract editorial composition, not an offered product image">
          <span>FIELD NOTE / PRODUCT &amp; PLACE</span>
          <strong>Life,<br/>in detail.</strong>
          <span>NO STOCK PHOTOGRAPH / ORIGINAL CONCEPT</span>
        </div>}
      </div>
    </section>
  );
}
