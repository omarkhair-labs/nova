"use client";

import { useEffect, useRef, useState, type MouseEvent } from "react";
import { destinations } from "./world-destinations";

export default function ChapterIndex({ current }: { current: string }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLAnchorElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    close.current?.focus();
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = previous; };
  }, [open]);

  function show(event: MouseEvent<HTMLAnchorElement>) {
    if (!dialog.current?.showModal) return;
    event.preventDefault();
    dialog.current.showModal();
    setOpen(true);
  }

  return <>
    <a ref={trigger} href="#site-directory" className="map-trigger" aria-haspopup="dialog"
      aria-expanded={open} onClick={show}>
      <span className="map-icon" aria-hidden="true" /> Map
    </a>
    <dialog ref={dialog} className="world-map" aria-labelledby="map-title" onClose={() => {
      setOpen(false);
      trigger.current?.focus();
    }}>
      {open && <>
        <div className="map-plate">
          <img src="/field/courtyard-map.webp" alt="" width={1536} height={1024} />
          <div className="map-legend">
            <h2 id="map-title">Plantupia</h2>
            <p>Choose where to go.</p>
          </div>
          <button ref={close} type="button" className="map-close" onClick={() => dialog.current?.close()}>
            Close <span aria-hidden="true">×</span>
          </button>
          <nav aria-label="Places on the courtyard map">
            {destinations.map(item => <a key={item.href} href={item.href}
              data-coordinate={item.coordinate} aria-current={current === item.href ? "page" : undefined}
              onClick={() => dialog.current?.close()}>
              <span className="map-pin" aria-hidden="true" />
              <span>{item.label}<small>{current === item.href ? "You are here" : item.detail}</small></span>
            </a>)}
          </nav>
          <p className="map-credit">Concept plan · Generated illustration</p>
        </div>
      </>}
    </dialog>
  </>;
}
