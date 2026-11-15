"use client";

import { useEffect, useRef, useState, type MouseEvent } from "react";
import { courtyard, materials } from "./Photograph";
import { environments } from "./environments";
import { tending, threshold } from "./courtyard-scenes";

const entries = [
  { href: "/", label: "The courtyard", line: "Enter the experience", image: threshold },
  { href: "/spaces", label: "Spaces", line: "Find a place like yours", image: environments[1].image },
  { href: "/studio", label: "Approach", line: "See how we read a site", image: courtyard },
  { href: "/living-systems", label: "Aftercare", line: "Return to the planting", image: tending },
  { href: "/about", label: "About", line: "Meet Plantupia", image: environments[5].image },
  { href: "/start-a-space", label: "Contact", line: "Prepare a note", image: materials },
] as const;

export default function ChapterIndex({ current = "/" }: { current?: string }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLAnchorElement>(null);
  const closer = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(Math.max(0, entries.findIndex(item => item.href === current)));
  const selected = entries[active];

  useEffect(() => {
    if (!open) return;
    closer.current?.focus();
    const original = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = original; };
  }, [open]);

  function show(event: MouseEvent<HTMLAnchorElement>) {
    if (!dialog.current?.showModal) return;
    event.preventDefault();
    setActive(Math.max(0, entries.findIndex(item => item.href === current)));
    dialog.current.showModal();
    setOpen(true);
  }

  return <>
    <a ref={trigger} className="chapter-toggle" href="#site-directory" aria-haspopup="dialog"
      aria-expanded={open} onClick={show}>
      <span className="chapter-cross" aria-hidden="true" /> Index
    </a>
    <dialog ref={dialog} className="chapter-index" aria-labelledby="chapter-title" onClose={() => {
      setOpen(false);
      trigger.current?.focus();
    }}>
      {open && <div className="chapter-index-inner">
        <div className="chapter-index-image" key={selected.href}>
          <img src={"/field/" + selected.image.file + ".webp"} width={selected.image.width}
            height={selected.image.height} alt="" />
        </div>
        <div className="chapter-index-header">
          <h2 id="chapter-title">Plantupia index</h2>
          <button ref={closer} type="button" onClick={() => dialog.current?.close()}>
            Close <span className="chapter-cross" aria-hidden="true" />
          </button>
        </div>
        <nav aria-label="Main navigation">
          {entries.map((item, index) => <a key={item.href} href={item.href}
            aria-current={item.href === current ? "page" : undefined}
            onFocus={() => setActive(index)}
            onPointerEnter={event => { if (event.pointerType === "mouse") setActive(index); }}
            onClick={() => dialog.current?.close()}>
            <span>{item.label}<small>{item.line}</small></span>
            <span aria-hidden="true">↗</span>
          </a>)}
        </nav>
      </div>}
    </dialog>
  </>;
}
