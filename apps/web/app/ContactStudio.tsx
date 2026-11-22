"use client";

import { useEffect, useRef, useState } from "react";
import SiteIndex from "./SiteIndex";
import { spaces, spacePrompts } from "./space-contexts";
import "./contact-studio.css";

const modes = [
  { id: "project", title: "Start a project", intro: "Tell us about the place you have in mind.", placeholder: "What are you imagining for your space?" },
  { id: "care", title: "Care & support", intro: "Tell us about your existing plants or installed space.", placeholder: "How can Plantupia help with your living space?" },
  { id: "general", title: "Say hello", intro: "For questions, ideas or possible collaborations.", placeholder: "What would you like to talk about?" },
] as const;
const environments = [
  { id: "", label: "Select a type of space" },
  { id: "home", label: "Home / residence" },
  { id: "work", label: "Offices / HQs / workplace" },
  { id: "gather", label: "Hospitality / hotel / restaurant / cafe" },
  { id: "industrial", label: "Industrial site / factory" },
  { id: "retail", label: "Commercial / mall / developer / showroom" },
  { id: "public", label: "Public / shared space" },
  { id: "other", label: "Something else" },
] as const;
type Mode = typeof modes[number]["id"];

export default function ContactStudio({ available, previewMode = true }: { available: boolean; previewMode?: boolean }) {
  const [mode, setMode] = useState<Mode>("project");
  const [environment, setEnvironment] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [organization, setOrganization] = useState("");
  const [location, setLocation] = useState("");
  const [message, setMessage] = useState("");
  const [feedback, setFeedback] = useState("");
  const [pending, setPending] = useState(false);
  const [reference, setReference] = useState("");
  const [website, setWebsite] = useState("");
  const [fieldNotes, setFieldNotes] = useState<{ environment: string; labels: string[] } | null>(null);
  const attempt = useRef<{ fingerprint: string; id: string } | null>(null);
  const busy = useRef(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const selected = params.get("space");
    const topic = params.get("topic");
    if (topic === "care" || topic === "general" || topic === "project") setMode(topic);
    const item = params.get("item");
    if (topic === "general" && item && item.length <= 160 && !/[<>]/.test(item)) {
      setMessage("I'd like to ask about the Plantupia product: " + item +
        ". Please share current available options and ordering steps.");
    }
    if (environments.some(item => item.id === selected)) setEnvironment(selected ?? "");
    const spaceIndex = ["home", "work", "gather", "industrial", "retail", "public"].findIndex(key => key === selected);
    const focus = params.get("focus");
    if (spaceIndex !== -1 && focus && /^(?:[0-2])(?:,[0-2]){0,2}$/.test(focus) && (!topic || topic === "project")) {
      const indices = [...new Set(focus.split(",").map(Number))].sort();
      setFieldNotes({ environment: selected!, labels: indices.map(index => spacePrompts[spaceIndex][index].label) });
    }
  }, []);

  const current = modes.find(item => item.id === mode)!;
  function update(setter: (value: string) => void, value: string) {
    setter(value);
    setFeedback("");
    setReference("");
  }

  async function send(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!available || busy.current || reference) return;
    if (!name.trim() || !email.trim() || !message.trim()) {
      setFeedback("Please add your name, email and message.");
      return;
    }
    const details = { topic: mode, name: name.trim(), email: email.trim(),
      organization: organization.trim(), environment: mode === "project" ? environment : "",
      location: mode !== "general" ? location.trim() : "", message: message.trim() };
    const fingerprint = JSON.stringify(details);
    if (attempt.current?.fingerprint !== fingerprint) {
      attempt.current = { fingerprint, id: crypto.randomUUID() };
    }
    busy.current = true;
    setPending(true);
    setFeedback("");
    try {
      const response = await fetch("/api/website/enquiries", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...details, submission_id: attempt.current.id, website }),
        signal: AbortSignal.timeout(16000),
      });
      const result = await response.json();
      if (!response.ok || typeof result.reference !== "string") {
        setFeedback(response.status === 429 ? "Too many requests. Please try again later."
          : response.status === 400 || response.status === 409 ? "Please check your details and try again."
          : "We could not confirm receipt. Your message is still here; please try again.");
        return;
      }
      setReference(result.reference);
      setFeedback("Your message has been received by Plantupia.");
      setMessage("");
      attempt.current = null;
    } catch {
      setFeedback("We could not confirm receipt. Your message is still here; please try again.");
    } finally {
      busy.current = false;
      setPending(false);
    }
  }

  return (
    <div className="ct-page">
      <a className="ct-skip" href="#ct-main">Skip to contact</a>
      <header className="ct-header">
        <a href="/" className="ct-brand" aria-label="Plantupia home">◕ Plantupia</a>
        <SiteIndex current="contact" />
        <a href="/spaces" className="ct-back">Explore spaces <span aria-hidden="true">↗</span></a>
      </header>
      <main className="ct-main" id="ct-main">
        <div className="ct-intro">
          <p className="ct-overline">PLANTUPIA / CONTACT</p>
          <h1>Let&apos;s make room<br/>for <em>life.</em></h1>
          <p>Every living space begins with a conversation.</p>
        </div>
        <section className="ct-work" aria-label="Send a message to Plantupia">
          <nav aria-label="How can we help?" className="ct-modes">
            {modes.map(item=><button key={item.id} aria-pressed={mode===item.id} type="button" disabled={pending} onClick={() => {setMode(item.id);setFeedback("");setReference("");}}>{item.title}</button>)}
          </nav>
          <p className="ct-mode-intro" aria-live="polite">{current.intro}</p>{!available&&<div className="ct-preview-note" role="note"><strong>{previewMode ? "Preview mode" : "Online enquiries unavailable"}</strong><span>{previewMode ? "You can complete the brief and compose a message. Sending is disabled, so nothing will be submitted or stored." : "The website inbox is not accepting messages right now. Nothing entered here will be sent or stored."}</span></div>}
          <form className="ct-form" onSubmit={send}>
            <fieldset className="ct-fields" disabled={pending}>
              <label htmlFor="ct-name">Your name</label>
              <input id="ct-name" required autoComplete="name" value={name} maxLength={100} placeholder="Full name" onChange={event=>update(setName,event.target.value)}/>
              <label htmlFor="ct-email">Email address</label>
              <input id="ct-email" required type="email" autoComplete="email" maxLength={160} value={email} placeholder="you@example.com" onChange={event=>update(setEmail,event.target.value)}/>
              <label htmlFor="ct-company">Company <span>Optional</span></label>
              <input id="ct-company" autoComplete="organization" maxLength={120} value={organization} placeholder="Organization" onChange={event=>update(setOrganization,event.target.value)}/>
              {mode==="project"&&<><label htmlFor="ct-space">Type of space</label><select id="ct-space" value={environment} onChange={event=>update(setEnvironment,event.target.value)}>{environments.map(item=><option key={item.id} value={item.id}>{item.label}</option>)}</select></>}
              {mode!=="general"&&<><label htmlFor="ct-location">Location <span>Optional</span></label><input id="ct-location" maxLength={100} value={location} placeholder="City or area" onChange={event=>update(setLocation,event.target.value)}/></>}
            </fieldset>
            <div className="ct-message">
              {mode === "project" && fieldNotes && environment === fieldNotes.environment && <div className="ct-field-notes">
                <p className="ct-field-notes-kicker">FROM YOUR SPACE STUDY</p>
                <p>{spaces[["home", "work", "gather", "industrial", "retail", "public"].indexOf(fieldNotes.environment)].name}</p>
                <ul>{fieldNotes.labels.map(label => <li key={label}>{label}</li>)}</ul>
                <button type="button" disabled={pending || !!reference} onClick={() => { const addition = "I would like to discuss: " + fieldNotes.labels.join(", ") + "."; const next = message.trim() ? message + "\n\n" + addition : addition; if(next.length <= 2000) {update(setMessage, next);setFieldNotes(null);} else {setFeedback("There is not enough room to add the notes. Please shorten your message first.");} }}>Add these notes to my message ↗</button>
              </div>}
              <label htmlFor="ct-message">Your message</label>
              <textarea id="ct-message" disabled={pending} rows={7} required maxLength={2000} value={message} placeholder={current.placeholder} onChange={event=>update(setMessage,event.target.value)}/>
              <div hidden aria-hidden="true"><label htmlFor="ct-website">Leave this empty</label><input id="ct-website" tabIndex={-1} autoComplete="off" value={website} onChange={event => setWebsite(event.target.value)} /></div>
              <div className="ct-action"><button type="submit" disabled={!available || pending || !!reference}>{pending ? "Sending…" : reference ? "Message received" : "Send your message"} <span aria-hidden="true">↗</span></button><p>{available ? "Your details and message will be saved in Plantupia’s private company inbox so the team can respond. Please do not include sensitive information." : (previewMode ? "Website submissions are not enabled on this preview. Nothing is sent or stored." : "Online submissions are not available at the moment. Nothing is sent or stored.")}</p></div>
              <p role="status" className="ct-status">{feedback}</p>
              {reference && <p className="ct-status">Reference: {reference}</p>}
            </div>
          </form>
        </section>
        <section className="ct-end"><span>NOT SURE WHERE TO BEGIN?</span><a href="/spaces">See the space types ↗</a></section>
      </main>
      <footer className="ct-footer"><a href="/">Plantupia</a><span>Design / Planting / Continuing care</span><a href="/about">About us ↗</a></footer>
    </div>
  );
}
