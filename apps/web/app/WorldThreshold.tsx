"use client";

import { useState } from "react";
import { editorialSpaces } from "./space-contexts";
import "./world-threshold.css";
import { privateWorldStudies } from "./world-private-studies";

const originalChapters = [
  {
    number: "02",
    name: "Spaces",
    line: "From the intimate to the industrial.",
    href: "/spaces",
    photo: editorialSpaces[3],
    note: "Industrial architecture / an editorial reference",
  },
  {
    number: "03",
    name: "The Studio",
    line: "How a place becomes a living design.",
    href: "/studio",
    photo: editorialSpaces[2],
    note: "Hospitality / an editorial reference",
  },
  {
    number: "04",
    name: "Living Systems",
    line: "A relationship that continues after installation.",
    href: "/living-systems",
    photo: editorialSpaces[0],
    note: "Domestic scale / an editorial reference",
  },
] as const;

type Chapter = (typeof originalChapters)[number];

export default function WorldThreshold({ privateReview = false }: { privateReview?: boolean }) {
  const chapters = originalChapters;
  const [active, setActive] = useState(0);
  const [failed, setFailed] = useState<number[]>([]);
  const current = chapters[active];

  function imageFor(chapter: Chapter, index: number) {
    return failed.includes(index) ? chapter.photo.fallback : privateReview ? privateWorldStudies[index].src : chapter.photo.src;
  }

  function onPhotoError(index: number) {
    setFailed(previous => previous.includes(index) ? previous : [...previous, index]);
  }

  return (
    <section className="wt-section" id="world" data-private-review={privateReview} data-active={active} aria-labelledby="wt-title">
      <div className="wt-intro">
        <p>PLANTUPIA / 02—04</p>
        <h2 id="wt-title">The world beyond the frame.</h2>
        <p className="wt-intro-line">Different places. One living relationship.</p>
      </div>
      <div className="wt-gallery">
        <nav className="wt-choices" aria-label="Explore Plantupia chapters">
          {chapters.map((chapter, i) => (
            <a key={chapter.href} href={chapter.href} aria-label={"Enter " + chapter.name}
              data-chapter-target={chapter.href} data-preview-active={active === i}
              className="wt-choice"
              onPointerEnter={event => { if (event.pointerType === "mouse") setActive(i); }}
              onFocus={() => setActive(i)}>
              <span className="wt-number">{chapter.number}</span>
              <span className="wt-choice-copy"><strong>{chapter.name}</strong><small>{chapter.line}</small></span>
              <span className="wt-thumb" aria-hidden="true">
                <img src={imageFor(chapter, i)} alt="" loading="lazy" onError={() => onPhotoError(i)} />
              </span>
              <span className="wt-arrow" aria-hidden="true">↗</span>
            </a>
          ))}
        </nav>
        <figure className="wt-picture">
          <div className="wt-photo-field" role="img" aria-label={failed.includes(active) ? "Generated architectural concept" : privateReview ? privateWorldStudies[active].subject : current.photo.subject}>
            {chapters.map((chapter, i) => (
              <div key={chapter.href} className={"wt-photo" + (i === active ? " wt-visible" : "")} aria-hidden="true">
                <img src={imageFor(chapter, i)} alt="" loading="lazy" onError={() => onPhotoError(i)} />
              </div>
            ))}
            <span className="wt-image-index" aria-hidden="true">{current.number} / 04</span>
          </div>
          <figcaption>
            <span>{failed.includes(active) ? "Generated concept illustration" : privateReview ? privateWorldStudies[active].note : current.note}. {privateReview ? "Not a verified installation." : "Not a Plantupia project."}</span>
            {!privateReview && !failed.includes(active) && <a href={current.photo.source} target="_blank" rel="noreferrer">{current.photo.credit} ↗</a>}
          </figcaption>
        </figure>
      </div>
      <p className="wt-mobile-credit">{privateReview ? "Supplied design proposals: Pixels Mall p28, EM Sherif p4 and El Hallab p3. Not verified installations." : "Editorial references or generated concept fallbacks. Not completed Plantupia projects."}</p>
      <div className="wt-end"><span>PLACE — DESIGN — CONTINUITY</span><a href="/about">Meet Plantupia ↗</a></div>
    </section>
  );
}
