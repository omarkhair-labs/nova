"use client";

import { useRef } from "react";

type NavLink = { href: string; label: string; current: boolean };

export default function MobileNav({ links, contactCurrent }: { links: NavLink[]; contactCurrent: boolean }) {
  const disclosure = useRef<HTMLDetailsElement>(null);
  return <details ref={disclosure} className="field-mobile" onKeyDown={event => {
    if (event.key === "Escape" && disclosure.current?.open) {
      disclosure.current.open = false;
      disclosure.current.querySelector("summary")?.focus();
    }
  }}>
    <summary>Menu <span aria-hidden="true" /></summary>
    <nav aria-label="Main navigation">
      {links.map(item => <a key={item.href} href={item.href} aria-current={item.current ? "page" : undefined}>{item.label}</a>)}
      <a href="/start-a-space" aria-current={contactCurrent ? "page" : undefined}>Get in touch <span aria-hidden="true">↗</span></a>
    </nav>
  </details>;
}
