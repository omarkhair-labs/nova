import type { ReactNode } from "react";
import ChapterIndex from "./ChapterIndex";
import "./field.css";
import "./experience.css";

export const navigation = [
  { href: "/spaces", label: "Spaces" },
  { href: "/studio", label: "Approach" },
  { href: "/living-systems", label: "Aftercare" },
  { href: "/about", label: "About" },
] as const;

export function Arrow({ children = "↗" }: { children?: string }) {
  return <span className="field-arrow" aria-hidden="true">{children}</span>;
}

function Wordmark() {
  return <a className="field-wordmark" href="/" aria-label="Plantupia home">
    <span className="field-mark" aria-hidden="true"><i /><i /><i /></span>
    plantupia<span aria-hidden="true">.</span>
  </a>;
}

export function SiteFrame({ children, current }: { children: ReactNode; current?: string }) {
  return <div className="field-site" data-page={current}>
    <a className="field-skip" href="#main">Skip to content</a>
    <header className="field-header">
      <div className="field-shell field-header-inner">
        <Wordmark />
        <ChapterIndex current={current} />
        <a className="field-header-cta" href="/start-a-space" aria-current={current === "/start-a-space" ? "page" : undefined}>
          Contact <Arrow />
        </a>
      </div>
    </header>
    <main id="main" tabIndex={-1}>{children}</main>
    <footer className="field-footer" id="site-directory">
      <div className="field-shell">
        <div className="field-footer-main">
          <div><Wordmark /><p>Planting for the places we build.</p></div>
          <nav aria-label="Footer">
            {navigation.map(item => <a key={item.href} href={item.href}>{item.label}</a>)}
            <a href="/start-a-space">Get in touch <Arrow /></a>
          </nav>
        </div>
        <div className="field-footer-meta">
          <span>Plantupia · Website preview</span>
          <a href="#top">Back to top <Arrow>↑</Arrow></a>
        </div>
      </div>
    </footer>
  </div>;
}

export function TextLink({ href, children, arrow = "↗" }: { href: string; children: ReactNode; arrow?: string }) {
  return <a href={href} className="field-textlink">{children} <Arrow>{arrow}</Arrow></a>;
}

export function Invitation({
  title = "What could grow here?",
  href = "/start-a-space",
  label = "Prepare a project brief",
}: { title?: string; href?: string; label?: string }) {
  return <section className="field-invitation">
    <div className="field-shell field-invitation-inner">
      <h2>{title}</h2>
      <a href={href} className="field-button">{label} <Arrow /></a>
    </div>
  </section>;
}
