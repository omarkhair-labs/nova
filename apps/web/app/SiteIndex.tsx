"use client";

import { useEffect, useRef, useState } from "react";
import "./site-index.css";

export type SiteSection = "experience" | "spaces" | "studio" | "systems";

export const siteSections: ReadonlyArray<{
  id: SiteSection;
  number: string;
  href: string;
  name: string;
  line: string;
}> = [
  { id: "experience", number: "01", href: "/", name: "The Experience", line: "Enter the world where space and life meet." },
  { id: "spaces", number: "02", href: "/spaces", name: "Spaces", line: "Explore the questions behind different environments." },
  { id: "studio", number: "03", href: "/studio", name: "The Studio", line: "Understand how our thinking becomes a living place." },
  { id: "systems", number: "04", href: "/living-systems", name: "Living Systems", line: "Discover what continues after installation." },
] as const;

export default function SiteIndex({
  current,
  tone = "light",
}: {
  current: SiteSection;
  tone?: "light" | "dark";
}) {
  const [open, setOpen] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const close = useRef<HTMLButtonElement>(null);

  function dismiss() {
    setOpen(false);
    trigger.current?.focus();
  }

  useEffect(() => {
    if (!open) return;
    close.current?.focus();
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        dismiss();
      }
      if (event.key !== "Tab" || !panel.current) return;
      const controls = Array.from(panel.current.querySelectorAll<HTMLElement>('a[href], button:not([disabled])'));
      const first = controls[0], last = controls[controls.length - 1];
      if (!first || !last) return;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault(); first.focus();
      }
    }
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = previous;
    };
  }, [open]);

  return (
    <div className={"si-root si-tone-" + tone}>
      <button type="button" className="si-trigger" ref={trigger} aria-haspopup="dialog"
        aria-expanded={open} aria-label="Open Plantupia world index" onClick={() => setOpen(true)}>
        <span aria-hidden="true">+</span>
      </button>
      {open && (
        <div className="si-overlay" ref={panel} role="dialog" aria-modal="true"
          aria-labelledby="si-title">
          <div className="si-top">
            <span>PLANTUPIA / THE WORLD INDEX</span>
            <button type="button" ref={close} onClick={dismiss} className="si-close">
              CLOSE <span aria-hidden="true">×</span>
            </button>
          </div>
          <div className="si-body">
            <div className="si-intro">
              <p>AN EXPERIENCE OF LIVING ARCHITECTURE</p>
              <h2 id="si-title">A place to<br/><em>feel alive.</em></h2>
              <span>Different ways to experience one relationship.</span>
            </div>
            <nav aria-label="Explore the website" className="si-links">
              {siteSections.map(item => (
                <a key={item.id} href={item.href}
                  className={item.id === current ? "si-active" : ""}
                  aria-current={item.id === current ? "page" : undefined}>
                  <span className="si-number">{item.number}</span>
                  <span className="si-link-text"><strong>{item.name}</strong><small>{item.line}</small></span>
                  <span aria-hidden="true" className="si-link-arrow">↗</span>
                </a>
              ))}
            </nav>
          </div>
          <div className="si-foot">
            <span>SPACES / PEOPLE / TIME</span>
            <span>AN ENGLISH-ONLY DESIGN STUDY · NOT THE OFFICIAL LIVE SITE</span>
          </div>
        </div>
      )}
    </div>
  );
}
