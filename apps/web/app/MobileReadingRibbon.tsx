/**
 * The Pocket Field — a phone-only reading ribbon.
 * Each link is a real in-page anchor. On desktop the original case structure
 * remains unchanged; this is editorial orientation, not a second app shell.
 */
export type FieldStop = { label: string; href: string };
export default function MobileReadingRibbon({
  number, title, stops, returnHref
}: {
  number: string; title: string; stops: readonly FieldStop[]; returnHref?: string
}) {
  return <nav className="mrf" aria-label={title + " mobile reading index"}>
    <div className="mrf-identity">
      <span className="mrf-count">{number}</span>
      <span className="mrf-title">{title}</span>
      <span className="mrf-slash" aria-hidden="true">/</span>
    </div>
    <div className="mrf-track" aria-label="Jump to a chapter">
      {stops.map((item,index)=><a href={item.href} key={item.href}><span>{String(index+1).padStart(2,"0")}</span>{item.label}</a>)}
      {returnHref&&<a href={returnHref} className="mrf-return"><span aria-hidden="true">↗</span>All work</a>}
    </div>
  </nav>;
}
