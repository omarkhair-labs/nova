import type { ReactNode } from "react";
import ChapterIndex from "./ChapterIndex";
import { destinations } from "./world-destinations";
import "./field.css";
import "./world.css";

export function Arrow({ children = "↗" }: { children?: string }) {
  return <span className="field-arrow" aria-hidden="true">{children}</span>;
}

export function SiteFrame({ children, current = "/" }: { children: ReactNode; current?: string }) {
  return <div className="field-site world" data-page={current}>
    <a className="field-skip" href="#main">Skip to scene</a>
    <main id="main" tabIndex={-1}>{children}</main>
    <nav className="world-navigation" aria-label="World navigation">
      <ChapterIndex current={current} />
    </nav>
    <nav id="site-directory" className="world-directory" aria-label="Browse without interactive map">
      <a href="#main">Close map</a>
      {destinations.map(item => <a key={item.href} href={item.href}
        aria-current={item.href === current ? "page" : undefined}>{item.label}</a>)}
    </nav>
  </div>;
}

export function TextLink({ href, children, arrow = "↗" }: { href: string; children: ReactNode; arrow?: string }) {
  return <a href={href} className="field-textlink">{children} <Arrow>{arrow}</Arrow></a>;
}
