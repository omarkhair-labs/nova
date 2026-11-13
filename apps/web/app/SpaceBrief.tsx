"use client";

import { useMemo, useState } from "react";
import SiteIndex from "./SiteIndex";
import "./space-brief.css";

const environments = [
  { id: "home", name: "A home", descriptor: "Residential interiors" },
  { id: "work", name: "A workplace", descriptor: "Offices and shared spaces" },
  { id: "gather", name: "A place to gather", descriptor: "Hospitality and social spaces" },
  { id: "other", name: "Something else", descriptor: "A space with its own character" },
] as const;
const intentions = [
  { id: "design", title: "Create a living space", note: "Explore an idea from the start" },
  { id: "introduce", title: "Introduce plants", note: "Think through selection and placement" },
  { id: "care", title: "Support an existing space", note: "Discuss the continuing care relationship" },
] as const;
const moments = [
  { id: "exploring", text: "Still exploring" },
  { id: "planning", text: "Planning the space" },
  { id: "soon", text: "Looking to move forward soon" },
] as const;

export default function SpaceBrief() {
  const [environment, setEnvironment] = useState<(typeof environments)[number]["id"]>("work");
  const [intention, setIntention] = useState<(typeof intentions)[number]["id"]>("design");
  const [moment, setMoment] = useState<(typeof moments)[number]["id"]>("exploring");
  const [location, setLocation] = useState("");
  const [note, setNote] = useState("");
  const [feedback, setFeedback] = useState("");

  const env = environments.find(item => item.id === environment)!;
  const aim = intentions.find(item => item.id === intention)!;
  const phase = moments.find(item => item.id === moment)!;
  const brief = useMemo(() => [
    "PLANTUPIA — SPACE CONVERSATION OUTLINE",
    "Prepared by a visitor. This is not a submitted enquiry.",
    "",
    "Type of space: " + env.name + " — " + env.descriptor,
    "Main intention: " + aim.title,
    "Current stage: " + phase.text,
    "Location: " + (location.trim() || "Not specified"),
    "A little more about the space:",
    note.trim() || "Not provided",
    "",
    "This is an exploratory outline, not a quote, commitment or project assessment.",
  ].join("\n"), [env, aim, phase, location, note]);

  async function copyBrief() {
    try {
      await navigator.clipboard.writeText(brief);
      setFeedback("Outline copied. Nothing has been sent.");
    } catch {
      setFeedback("Clipboard access was blocked. Select and copy the outline below instead.");
      document.getElementById("sb-copy-fallback")?.focus();
    }
  }

  return (
    <div className="sb-page">
      <a className="sb-skip" href="#sb-main">Skip to project outline</a>
      <header className="sb-header">
        <a className="sb-brand" href="/" aria-label="Plantupia home"><span aria-hidden="true">◕</span> Plantupia</a>
        <SiteIndex current={null} tone="dark"/>
        <a className="sb-back" href="/spaces">BACK TO SPACES <span aria-hidden="true">↗</span></a>
      </header>

      <main id="sb-main" className="sb-main">
        <div className="sb-intro">
          <div className="sb-image-wrap" aria-hidden="true">
            <img
              src="https://images.pexels.com/photos/33708096/pexels-photo-33708096.jpeg?auto=compress&cs=tinysrgb&w=1920"
              alt=""
              onError={event => {
                const image = event.currentTarget;
                const fallback = new URL("/scene/hd/life.webp", window.location.origin).href;
                if (image.src !== fallback) image.src = fallback;
              }}
            />
            <span className="sb-image-note">EDITORIAL ARCHITECTURAL REFERENCE · NOT A PLANTUPIA PROJECT</span>
          </div>
          <div className="sb-intro-text">
            <p className="sb-eyebrow">A SPACE / A FIRST CONVERSATION</p>
            <h1>Let&apos;s begin<br/><em>with the place.</em></h1>
            <p>Every living environment starts with questions. Describe the setting, what you hope to explore, and where you are in the process.</p>
            <div className="sb-intro-folio"><span>PLANTUPIA / THE NEXT STEP</span><span>NO ACCOUNT NEEDED</span></div>
          </div>
        </div>

        <div className="sb-workbench">
          <section className="sb-questions" aria-labelledby="sb-questions-title">
            <p className="sb-section-label">01—04 / THE SPACE IN MIND</p>
            <h2 id="sb-questions-title">A few things<br/><em>worth knowing.</em></h2>
            <fieldset className="sb-choice-group">
              <legend><span>01</span> What kind of space?</legend>
              <div className="sb-choice-grid">
                {environments.map(item => (
                  <button key={item.id} type="button" className={"sb-option" + (environment === item.id ? " sb-chosen" : "")}
                    aria-pressed={environment === item.id} onClick={() => { setEnvironment(item.id); setFeedback(""); }}>
                    <strong>{item.name}</strong><small>{item.descriptor}</small><span aria-hidden="true">{environment === item.id ? "●" : "○"}</span>
                  </button>
                ))}
              </div>
            </fieldset>
            <fieldset className="sb-choice-group">
              <legend><span>02</span> What would you like to explore?</legend>
              <div className="sb-choice-stack">
                {intentions.map(item => (
                  <button key={item.id} type="button" aria-pressed={intention === item.id}
                    className={"sb-option" + (intention === item.id ? " sb-chosen" : "")}
                    onClick={() => { setIntention(item.id); setFeedback(""); }}>
                    <strong>{item.title}</strong><small>{item.note}</small><span aria-hidden="true">{intention === item.id ? "●" : "○"}</span>
                  </button>
                ))}
              </div>
            </fieldset>
            <fieldset className="sb-choice-group">
              <legend><span>03</span> Where are you in the process?</legend>
              <div className="sb-moment-row">
                {moments.map(item => (
                  <button key={item.id} type="button" aria-pressed={moment === item.id}
                    className={"sb-moment" + (moment === item.id ? " sb-moment-chosen" : "")}
                    onClick={() => { setMoment(item.id); setFeedback(""); }}>{item.text}</button>
                ))}
              </div>
            </fieldset>
            <div className="sb-choice-group sb-open">
              <div className="sb-open-legend"><span>04</span> Add a little context <small>OPTIONAL</small></div>
              <label htmlFor="sb-location">City or area</label>
              <input id="sb-location" type="text" maxLength={100} placeholder="Where is your space?" value={location}
                onChange={event => { setLocation(event.target.value); setFeedback(""); }}/>
              <label htmlFor="sb-notes">What matters to you about this space?</label>
              <textarea id="sb-notes" rows={4} maxLength={700} value={note}
                onChange={event => { setNote(event.target.value); setFeedback(""); }}
                placeholder="The feeling, constraints, light, existing plants, or anything you want to explore."/>
            </div>
          </section>

          <aside className="sb-dossier" aria-label="Your space outline">
            <div className="sb-dossier-heading">
              <span>SPACE DOSSIER / PREVIEW</span>
              <span aria-hidden="true">↗</span>
            </div>
            <div className={"sb-dossier-art sb-dossier-" + environment} aria-hidden="true">
              <div className="sb-arch-sun"/><div className="sb-arch-room"/><div className="sb-arch-window"/>
              <div className="sb-arch-floor"/><div className="sb-arch-plant"><i/><i/><i/><i/></div>
              <span>STUDY / {env.name.toUpperCase()}</span>
            </div>
            <div className="sb-dossier-reading" aria-live="polite">
              <span>AN OUTLINE, NOT YET A PROPOSAL</span>
              <h3>{env.name}<br/><em>{aim.title.toLowerCase()}.</em></h3>
              <dl>
                <div><dt>PLACE</dt><dd>{env.descriptor}</dd></div>
                <div><dt>INTENTION</dt><dd>{aim.title}</dd></div>
                <div><dt>STAGE</dt><dd>{phase.text}</dd></div>
                <div><dt>LOCATION</dt><dd>{location.trim() || "To be discussed"}</dd></div>
              </dl>
              <p className="sb-privacy">The outline stays in this browser while you are on this page. It is <strong>not submitted</strong>, stored on a Plantupia server or sent to anyone.</p>
              <button className="sb-copy" type="button" onClick={copyBrief}>COPY YOUR SPACE OUTLINE <span aria-hidden="true">↗</span></button>
              <p className="sb-copy-feedback" role="status" aria-live="polite">{feedback || "You can share the copied outline with the Plantupia team through an approved contact channel."}</p>
              <label className="sb-fallback-label" htmlFor="sb-copy-fallback">Selectable outline, in case copying is blocked</label>
              <textarea id="sb-copy-fallback" className="sb-copy-fallback" readOnly rows={7} value={brief} aria-label="Prepared project outline for manual copying"/>
              <p className="sb-disclaimer">No automated enquiry has been created. Direct submission will be enabled only after the company approves its contact destination and privacy process.</p>
            </div>
          </aside>
        </div>
      </main>
      <footer className="sb-footer"><a href="/">PLANTUPIA / THE EXPERIENCE</a><span>DESIGN / LIVING MATERIAL / CONTINUITY</span><a href="/studio">EXPLORE THE STUDIO ↗</a></footer>
    </div>
  );
}
