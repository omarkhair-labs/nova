"use client";

import { useEffect, useState } from "react";
import SiteIndex from "./SiteIndex";
import "./contact-studio.css";

const modes = [
  { id: "project", title: "Start a project", intro: "Tell us about the place you have in mind.", placeholder: "What are you imagining for your space?" },
  { id: "care", title: "Care & support", intro: "Tell us about your existing plants or installed space.", placeholder: "How can Plantupia help with your living space?" },
  { id: "general", title: "Say hello", intro: "For questions, ideas or possible collaborations.", placeholder: "What would you like to talk about?" },
] as const;
const environments = [
  { id: "", label: "Select a type of space" },
  { id: "home", label: "Home / residence" },
  { id: "work", label: "Workplace / office" },
  { id: "gather", label: "Hotel / hospitality" },
  { id: "industrial", label: "Industrial site / factory" },
  { id: "retail", label: "Retail / destination" },
  { id: "public", label: "Public / shared space" },
  { id: "other", label: "Something else" },
] as const;
type Mode = typeof modes[number]["id"];

export default function ContactStudio() {
  const [mode, setMode] = useState<Mode>("project");
  const [environment, setEnvironment] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [organization, setOrganization] = useState("");
  const [location, setLocation] = useState("");
  const [message, setMessage] = useState("");
  const [feedback, setFeedback] = useState("");
  const [showCopy, setShowCopy] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const selected = params.get("space");
    const topic = params.get("topic");
    if (topic === "care" || topic === "general" || topic === "project") setMode(topic);
    if (environments.some(item => item.id === selected)) setEnvironment(selected ?? "");
  }, []);

  const current = modes.find(item => item.id === mode)!;
  const outline = [
    "Plantupia — " + current.title,
    name.trim() ? "Name: " + name.trim() : "",
    email.trim() ? "Email: " + email.trim() : "",
    organization.trim() ? "Company: " + organization.trim() : "",
    mode === "project" && environment ? "Space: " + (environments.find(item => item.id === environment)?.label ?? "") : "",
    mode !== "general" && location.trim() ? "Location: " + location.trim() : "",
    "",
    message.trim(),
  ].filter((line, index) => line || index === 6).join("\n");

  function update(setter: (value: string) => void, value: string) {
    setter(value);
    setFeedback("");
    setShowCopy(false);
  }

  async function prepare(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!message.trim()) {
      setFeedback("Please add a short message.");
      return;
    }
    try {
      await navigator.clipboard.writeText(outline);
      setFeedback("Your message is copied. Nothing has been sent.");
      setShowCopy(false);
    } catch {
      setShowCopy(true);
      setFeedback("Copy the prepared message below. Nothing has been sent.");
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
        <section className="ct-work" aria-label="Prepare a message for Plantupia">
          <nav aria-label="How can we help?" className="ct-modes">
            {modes.map(item=><button key={item.id} aria-pressed={mode===item.id} type="button" onClick={() => {setMode(item.id);setFeedback("");setShowCopy(false);}}>{item.title}</button>)}
          </nav>
          <p className="ct-mode-intro" aria-live="polite">{current.intro}</p>
          <form className="ct-form" onSubmit={prepare}>
            <div className="ct-fields">
              <label htmlFor="ct-name">Your name <span>Optional</span></label>
              <input id="ct-name" autoComplete="name" value={name} maxLength={100} placeholder="Full name" onChange={event=>update(setName,event.target.value)}/>
              <label htmlFor="ct-email">Email address <span>Optional</span></label>
              <input id="ct-email" type="email" autoComplete="email" maxLength={160} value={email} placeholder="you@example.com" onChange={event=>update(setEmail,event.target.value)}/>
              <label htmlFor="ct-company">Company <span>Optional</span></label>
              <input id="ct-company" autoComplete="organization" maxLength={120} value={organization} placeholder="Organization" onChange={event=>update(setOrganization,event.target.value)}/>
              {mode==="project"&&<><label htmlFor="ct-space">Type of space</label><select id="ct-space" value={environment} onChange={event=>update(setEnvironment,event.target.value)}>{environments.map(item=><option key={item.id} value={item.id}>{item.label}</option>)}</select></>}
              {mode!=="general"&&<><label htmlFor="ct-location">Location <span>Optional</span></label><input id="ct-location" maxLength={100} value={location} placeholder="City or area" onChange={event=>update(setLocation,event.target.value)}/></>}
            </div>
            <div className="ct-message">
              <label htmlFor="ct-message">Your message</label>
              <textarea id="ct-message" rows={7} required maxLength={2000} value={message} placeholder={current.placeholder} onChange={event=>update(setMessage,event.target.value)}/>
              <div className="ct-action"><button type="submit">Copy your message <span aria-hidden="true">↗</span></button><p>Preview only — nothing is sent or stored by Plantupia. You can share your copied message through an approved company contact channel.</p></div>
              <p role="status" className="ct-status">{feedback}</p>
              {showCopy&&<><label htmlFor="ct-outline">Prepared message</label><textarea id="ct-outline" readOnly rows={7} value={outline}/></>}
            </div>
          </form>
        </section>
        <section className="ct-end"><span>NOT SURE WHERE TO BEGIN?</span><a href="/spaces">Explore the possibilities ↗</a></section>
      </main>
      <footer className="ct-footer"><a href="/">Plantupia</a><span>Living architecture / Design preview</span><a href="/about">About us ↗</a></footer>
    </div>
  );
}
