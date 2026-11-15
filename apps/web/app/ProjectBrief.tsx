"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { environments, getEnvironment } from "./environments";
import { topics, type Topic } from "./contact-topics";

export default function ProjectBrief({ initialTopic, initialSpace }: { initialTopic: Topic; initialSpace: string }) {
  const [ready, setReady] = useState(false);
  const [topic, setTopic] = useState(initialTopic);
  const [space, setSpace] = useState(initialSpace);
  const [location, setLocation] = useState("");
  const [drafts, setDrafts] = useState<Record<Topic, string>>({ project: "", care: "", general: "" });
  const [prepared, setPrepared] = useState("");
  const [status, setStatus] = useState("");
  const result = useRef<HTMLTextAreaElement>(null);
  const messageInput = useRef<HTMLTextAreaElement>(null);
  const current = topics.find(item => item.id === topic)!;

  useEffect(() => setReady(true), []);

  function resetResult() {
    setPrepared("");
    setStatus("");
  }

  function prepare(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = drafts[topic].trim();
    if (!message) {
      setStatus("Add a short message to prepare your draft.");
      messageInput.current?.focus();
      return;
    }
    const place = getEnvironment(space)?.name ?? (space === "other" ? "Other space" : "");
    const summary = [
      "Plantupia — " + current.label,
      topic === "project" && place ? "Space: " + place : "",
      topic !== "general" && location.trim() ? "Location: " + location.trim() : "",
    ].filter(Boolean).join("\n");

    setPrepared(summary + "\n\n" + message);
    setStatus("Your draft is ready to copy.");
  }

  async function copy() {
    const draft = prepared;
    // Select the visible draft if the browser blocks clipboard access.
    try {
      await navigator.clipboard.writeText(draft);
      if (result.current?.value === draft) setStatus("Copied. Your message stays with you.");
    } catch {
      if (result.current?.value === draft) {
        result.current.focus();
        result.current.select();
        setStatus("The draft is selected. Use your device’s copy command.");
      }
    }
  }

  return <div className="field-composer">
    <form onSubmit={prepare}>
      <fieldset>
        <legend>What’s your message about?</legend>
        <div className="field-lenses field-topics">
          {topics.map(item => <label key={item.id}>
            <input type="radio" name="topic" value={item.id} checked={topic === item.id}
              onChange={() => { setTopic(item.id); resetResult(); }} />
            <span>{item.label}</span>
          </label>)}
        </div>
      </fieldset>
      {topic !== "general" && <div className="field-brief-fields">
        {topic === "project" && <label>Type of space
          <select value={space} onChange={event => { setSpace(event.target.value); resetResult(); }}>
            <option value="">Choose a space</option>
            {environments.map(item => <option key={item.slug} value={item.slug}>{item.name}</option>)}
            <option value="other">Other space</option>
          </select>
        </label>}
        <label>City or area <span className="field-optional">(optional)</span>
          <input value={location} maxLength={100} onChange={event => { setLocation(event.target.value); resetResult(); }} />
        </label>
      </div>}
      <label htmlFor="brief-message">Your message</label>
      <textarea id="brief-message" ref={messageInput} rows={5} required maxLength={2000} value={drafts[topic]}
        placeholder={current.prompt}
        onChange={event => { setDrafts({ ...drafts, [topic]: event.target.value }); resetResult(); }} />
      <button className="field-button" type="submit" disabled={!ready}>Prepare draft <span className="field-arrow" aria-hidden="true">↓</span></button>
      <noscript><p>Enable JavaScript to prepare and copy a draft on this device.</p></noscript>
    </form>
    <p className="field-status" role="status">{status}</p>
    {prepared && <section aria-labelledby="draft-label" className="field-draft">
      <label id="draft-label" htmlFor="prepared-message">Your prepared message</label>
      <textarea id="prepared-message" readOnly rows={6} value={prepared} ref={result} />
      <button type="button" className="field-button" onClick={copy}>Copy message <span className="field-arrow" aria-hidden="true">↗</span></button>
    </section>}
  </div>;
}
