import "./shop-experience.css";

/** A quiet seventh doorway, never a fake numbered chapter in the six-part story. */
export default function ShopBridge() {
  return (
    <section className="sh-bridge" aria-labelledby="shop-bridge-title">
      <div className="sh-bridge-inner">
        <div>
          <p className="sh-micro">ANOTHER WAY IN / PLANTUPIA OBJECTS</p>
          <h2 id="shop-bridge-title">What stays<br/><em>with you.</em></h2>
          <p>Beyond designing with living things, we care about the things themselves: individual plants, considered vessels, and the rituals that help them belong.</p>
          <a href="/shop">Explore the edited collection <span aria-hidden="true">↗</span></a>
        </div>
        <div className="sh-bridge-figure" aria-label="An abstract editorial composition, not an offered product image">
          <span>FIELD NOTE / PRODUCT &amp; PLACE</span>
          <strong>Life,<br/>in detail.</strong>
          <span>NO STOCK PHOTOGRAPH / ORIGINAL CONCEPT</span>
        </div>
      </div>
    </section>
  );
}
