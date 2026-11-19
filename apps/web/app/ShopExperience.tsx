"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import SiteIndex from "./SiteIndex";
import type { PublicCatalogResult, PublicCommerceSku } from "./shop/catalog";
import { shopField } from "./visual-production-map";
import "./shop-retail.css";

type Kind = PublicCommerceSku["kind"];
type Shelf = "all" | Kind;
type Sort = "default" | "low" | "high" | "name";
type Product = { id:string; name:string; description:string; kind:Kind; hasImage:boolean; alt:string; variants:PublicCommerceSku[] };
const categories:{id:Shelf;name:string}[]=[{id:"all",name:"Shop all"},{id:"plant",name:"Plants"},{id:"planter",name:"Planters"},{id:"care",name:"Plant care"}];
const label:Record<Kind,string>={plant:"Plant",planter:"Planter",care:"Plant care"};
const money=(value:number)=>new Intl.NumberFormat("en-EG",{style:"currency",currency:"EGP",maximumFractionDigits:2}).format(value);
const minPrice=(p:Product)=>Math.min(...p.variants.map(v=>Number(v.price)));
const hasStock=(p:Product)=>p.variants.some(v=>v.in_stock);
function ProductPhoto({product,eager=false}:{product:Product;eager?:boolean}){
  const [failed,setFailed]=useState(false);
  return <div className="sr-photo">
    {product.hasImage&&!failed?<img src={"/api/commerce/products/"+product.id+"/cover/"} alt={product.alt} loading={eager?"eager":"lazy"} onError={()=>setFailed(true)}/>:<div className="sr-photo-empty"><span>{label[product.kind]}</span><small>Photo not available</small></div>}
  </div>;
}
export default function ShopExperience({catalog,privateReview=false}:{catalog:PublicCatalogResult;privateReview?:boolean}){
  const [category,setCategory]=useState<Shelf>("all");
  const [query,setQuery]=useState("");
  const [sort,setSort]=useState<Sort>("default");
  const [stock,setStock]=useState(false);
  const [selected,setSelected]=useState<Product|null>(null);
  const [variantId,setVariantId]=useState("");
  const dialog=useRef<HTMLDialogElement>(null);
  const opener=useRef<HTMLElement|null>(null);
  const products=useMemo(()=>{
    const grouped=new Map<string,Product>();
    for(const sku of catalog.items){
      const existing=grouped.get(sku.product_id);
      if(existing){existing.variants.push(sku);existing.hasImage ||= sku.has_image;continue;}
      grouped.set(sku.product_id,{id:sku.product_id,name:sku.name,description:sku.description,kind:sku.kind,hasImage:sku.has_image,alt:sku.image_alt||sku.name,variants:[sku]});
    }
    return [...grouped.values()];
  },[catalog.items]);
  const visible=useMemo(()=>{
    const text=query.trim().toLocaleLowerCase();
    const matches=products.filter(p=>(category==="all"||p.kind===category)&&(!stock||hasStock(p))&&(!text||[p.name,p.description,label[p.kind],...p.variants.map(v=>v.option)].join(" ").toLocaleLowerCase().includes(text)));
    return matches.sort((a,b)=>sort==="low"?minPrice(a)-minPrice(b):sort==="high"?minPrice(b)-minPrice(a):sort==="name"?a.name.localeCompare(b.name):0);
  },[products,category,query,sort,stock]);
  const featured=products.find(p=>p.kind==="plant"&&p.hasImage&&hasStock(p))??products.find(p=>p.hasImage&&hasStock(p))??products.find(p=>p.hasImage);
  const selectedVariant=selected?.variants.find(v=>v.sku_id===variantId);
  useEffect(()=>{if(selected && dialog.current && !dialog.current.open) dialog.current.showModal();},[selected]);
  function openProduct(p:Product,event:React.MouseEvent<HTMLButtonElement>){
    opener.current=event.currentTarget;
    setSelected(p);setVariantId((p.variants.find(v=>v.in_stock)??p.variants[0]).sku_id);

  }
  function clear(){setCategory("all");setQuery("");setStock(false);setSort("default");}
  return <div className="sr-world">
    <a className="sr-skip" href="#shop-collection">Skip to products</a>
    <header className="sr-header"><a href="/" className="sr-wordmark">◕ Plantupia</a><SiteIndex current={null} tone="dark"/><a className="sr-contact" href="/start-a-space?topic=general">Get in touch ↗</a></header>
    <main>
      <section className={"sr-hero"+(featured?" sr-hero-with-photo":"")} aria-labelledby="shop-title">
        <div className="sr-intro"><p className="sr-eyebrow">THE PLANTUPIA SHOP</p><h1 id="shop-title">Make room<br/>for <em>life.</em></h1><p>Living plants, considered planters and essentials for the spaces you inhabit.</p><a className="sr-primary" href="#shop-collection">Explore the collection <span aria-hidden="true">↓</span></a></div>
        {privateReview?<div className="sr-hero-field" aria-label="Plantupia material world">{shopField.slice(0,3).map((item,i)=><figure key={item.asset} className={"sr-hero-field-"+(i+1)}><img src={"/api/private-arrival/"+item.asset} alt={item.label+" catalogue study"}/><figcaption><span>{item.role}</span><strong>{item.label}</strong><small>{item.meta}</small></figcaption></figure>)}</div>:featured&&<div className="sr-hero-visual"><ProductPhoto key={featured.id} product={featured} eager/><div className="sr-feature-note"><span>From the collection</span><button onClick={event=>openProduct(featured,event)}>{featured.name} &#8599;</button></div></div>}
      </section>
      {privateReview&&<section className="sr-material-world sr-material-library" aria-labelledby="sr-material-title">
        <header className="sr-library-head"><div><p className="sr-eyebrow">PLANT / VESSEL / CONDITION</p><h2 id="sr-material-title">A material library,<br/>not a product grid.</h2></div><div><p>The catalogue does not separate greenery from its setting. Space, plant, planter, finish and care are specified as one design problem.</p><nav className="sr-library-index" aria-label="Material library index"><a href="#shop-title"><span>01</span>Condition</a><a href="#sr-vessel-study"><span>02</span>Vessel</a><a href="#sr-plant-study"><span>03</span>Plant</a><a href="#sr-finish-study"><span>04</span>Finish</a></nav></div></header>
        <article className="sr-vessel-study" id="sr-vessel-study"><figure className="sr-vessel-setting"><img src={"/api/private-arrival/"+shopField[3].asset} alt="Augusta planter hospitality setting study" loading="lazy"/><figcaption><span>02 / VESSEL IN SPACE</span><strong>{shopField[3].label}</strong><small>{shopField[3].meta}</small></figcaption></figure><div className="sr-vessel-copy"><p className="sr-eyebrow">AUGUSTA PLANTER / SOURCE STUDY</p><h3>The vessel is part<br/>of the architecture.</h3><p>The catalogue positions the Augusta planter as a hospitality object for lobbies, lounges and interiors, not as a neutral container added afterwards.</p><dl><div><dt>Material family</dt><dd>Fiberglass</dd></div><div><dt>Role</dt><dd>Planter + spatial object</dd></div><div><dt>Source</dt><dd>Hospitality catalogue / p27</dd></div></dl></div><figure className="sr-vessel-object"><img src={"/api/private-arrival/"+shopField[4].asset} alt="Augusta planter object study" loading="lazy"/><figcaption><span>OBJECT STUDY</span><strong>{shopField[4].label}</strong></figcaption></figure></article>
        <article className="sr-plant-study" id="sr-plant-study"><figure><img src={"/api/private-arrival/"+shopField[5].asset} alt="Plantupia plant collection catalogue study" loading="lazy"/><figcaption><span>03 / PLANT PALETTE</span><strong>{shopField[5].label}</strong><small>{shopField[5].meta}</small></figcaption></figure><div><p className="sr-eyebrow">PLANT COLLECTIONS</p><h3>Plants become a<br/>material palette.</h3><p>The source catalogues palms, ficus, dracaena, ZZ plants and other species alongside planter dimensions. That relationship belongs before price and stock in a design-led experience.</p><a href="/living-systems">See how the living material continues into care &#8599;</a></div></article>
        <article className="sr-finish-study" id="sr-finish-study"><div className="sr-finish-copy"><p className="sr-eyebrow">04 / PLANTER COLLECTIONS + CUSTOM FINISHES</p><h3>Finish is a<br/>specification.</h3><p>The catalogue describes fiberglass planters as configurable through colour, finish and custom sizing. That turns the vessel collection into an architectural specification layer rather than a row of generic pots.</p><ul><li><span>01</span>Fiberglass planter families</li><li><span>02</span>RAL colour selection</li><li><span>03</span>Custom sizing</li><li><span>04</span>Specified to the interior</li></ul></div><figure><img src={"/api/private-arrival/"+shopField[6].asset} alt="Fiberglass planter collection catalogue study" loading="lazy"/><figcaption><span>VESSELS / SOURCE STUDY</span><strong>{shopField[6].label}</strong><small>{shopField[6].meta}</small></figcaption></figure></article>
        <footer className="sr-library-footer"><span>CATALOGUE STUDIES / NOT LIVE SKU PHOTOGRAPHY</span><p>Private review language only. Product availability, exact finish options and commercial terms remain governed by the live catalogue and confirmed project quotation.</p><a href="#shop-collection">Continue to the live catalogue &#8595;</a></footer>
      </section>}
      <section className="sr-collection" id="shop-collection" aria-labelledby="collection-title">
        <div className="sr-section-head"><div><p className="sr-eyebrow">{privateReview?"LIVE CATALOGUE / CURRENT PUBLISHED ITEMS":"FIND SOMETHING TO LIVE WITH"}</p><h2 id="collection-title">{categories.find(c=>c.id===category)?.name}</h2></div><p>Discover the current Plantupia collection.</p></div>
        <nav className="sr-categories" aria-label="Shop categories">{categories.map(c=><button key={c.id} type="button" aria-pressed={category===c.id} onClick={()=>setCategory(c.id)}>{c.name}<span>{c.id==="all"?products.length:products.filter(p=>p.kind===c.id).length}</span></button>)}</nav>
        <div className="sr-toolbar">
          <label className="sr-search"><span>Search</span><input type="search" value={query} onChange={e=>setQuery(e.target.value)} placeholder="Plant, planter, name…"/></label>
          <label className="sr-stock-filter"><input type="checkbox" checked={stock} onChange={e=>setStock(e.target.checked)}/> In stock</label>
          <label className="sr-sort"><span>Sort by</span><select value={sort} onChange={e=>setSort(e.target.value as Sort)}><option value="default">Featured</option><option value="low">Price: low to high</option><option value="high">Price: high to low</option><option value="name">Name: A–Z</option></select></label>
        </div>
        <div className="sr-results"><span role="status">{visible.length} {visible.length===1?"product":"products"}</span>{(category!=="all"||query||stock||sort!=="default")&&<button onClick={clear}>Clear filters ×</button>}</div>
        {catalog.state!=="available"?<div className="sr-empty" role="status"><h3>{catalog.state==="empty"?"The collection is coming soon.":"The collection is temporarily unavailable."}</h3><p>Get in touch with Plantupia to discuss what you are looking for.</p><a href="/start-a-space?topic=general">Contact Plantupia ↗</a></div>:visible.length===0?<div className="sr-empty"><h3>No matching products.</h3><p>Try a different search or category.</p><button onClick={clear}>Clear filters</button></div>:<div className="sr-grid">
          {visible.map(p=><article className="sr-product" key={p.id}><button className="sr-product-open" onClick={event=>openProduct(p,event)} aria-label={"View "+p.name}><div className="sr-product-media"><ProductPhoto product={p}/>{!hasStock(p)&&<span className="sr-stock-badge">Unavailable</span>}</div><div className="sr-product-meta"><span className="sr-kind">{label[p.kind]}</span><h3>{p.name}</h3><p>{p.variants.some(v=>Number(v.price)!==minPrice(p))?"From ":""}{money(minPrice(p))}</p><span className="sr-option-count">{p.variants.length} {p.variants.length===1?"option":"options"} available to explore</span></div></button></article>)}
        </div>}
        <p className="sr-catalog-note">Prices and stock reflect the published catalogue at the time of viewing. Ordering and final delivery pricing are confirmed through the Plantupia customer app.</p>
      </section>
      <section className="sr-service"><div><p className="sr-eyebrow">MORE THAN A SINGLE PLANT</p><h2>Planning a whole space?</h2><p>Explore our design practice for homes, hospitality and commercial interiors.</p></div><a className="sr-primary" href="/spaces">Explore spaces ↗</a><a className="sr-text-link" href="/living-systems">Discover ongoing care ↗</a></section>
    </main>
    <footer className="sr-footer"><a href="/">Plantupia</a><a href="/start-a-space">Contact</a><span>Collection preview</span></footer>
    <dialog className="sr-dialog" ref={dialog} aria-labelledby="sr-detail-name" onClose={()=>{setSelected(null);opener.current?.focus();}} onClick={e=>{if(e.target===e.currentTarget)dialog.current?.close();}}>
      {selected&&<div className="sr-detail"><button type="button" className="sr-close" aria-label="Close product details" onClick={()=>dialog.current?.close()}>×</button><ProductPhoto key={selected.id} product={selected}/><div className="sr-detail-copy"><p className="sr-eyebrow">{label[selected.kind]}</p><h2 id="sr-detail-name">{selected.name}</h2>{selectedVariant&&<p className="sr-detail-price">{money(Number(selectedVariant.price))}</p>}{selected.description&&<p className="sr-description">{selected.description}</p>}<fieldset><legend>Choose an option</legend>{selected.variants.map(v=><label className="sr-variant" key={v.sku_id}><input type="radio" name="sr-variant" value={v.sku_id} checked={variantId===v.sku_id} onChange={()=>setVariantId(v.sku_id)}/><span>{v.option||"Standard"}<small>{v.in_stock?"In stock at last check":"Currently unavailable"}</small></span><strong>{money(Number(v.price))}</strong></label>)}</fieldset><a className="sr-primary" href={"/start-a-space?topic=general&item="+encodeURIComponent(selected.name+(selectedVariant?.option?" — "+selectedVariant.option:""))}>Ask about this product ↗</a><p className="sr-catalog-note">An enquiry is not an order or reservation.</p></div></div>}
    </dialog>
  </div>;
}
