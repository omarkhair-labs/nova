"use client";

import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from "react";
import { environments, getEnvironment } from "./environments";
import { topics, type Topic } from "./contact-topics";

const replyFields = [
  { key: "name", label: "Your name", type: "text", autoComplete: "name", limit: 100 },
  { key: "email", label: "Email", type: "email", autoComplete: "email", limit: 254 },
  { key: "phone", label: "Phone", type: "tel", autoComplete: "tel", limit: 35 },
  { key: "organisation", label: "Organisation", type: "text", autoComplete: "organization", limit: 120 },
] as const;

type ReplyField = (typeof replyFields)[number]["key"];
type Draft = { message: string; location: string; context: string };

const emptyDraft: Draft = { message: "", location: "", context: "" };

export default function ProjectBrief({ initialTopic, initialSpace }: { initialTopic: Topic; initialSpace: string }) {
  const [ready, setReady] = useState(false);
  const [topic, setTopic] = useState(initialTopic);
  const [space, setSpace] = useState(initialSpace);
  const [reply, setReply] = useState<Record<ReplyField, string>>({
    name: "", email: "", phone: "", organisation: "",
  });
  const [drafts, setDrafts] = useState<Record<Topic, Draft>>({
    project: emptyDraft, care: emptyDraft, general: emptyDraft,
  });
  const [prepared, setPrepared] = useState("");
  const [status, setStatus] = useState("");
  const reviewTitle = useRef<HTMLHeadingElement>(null);
  const returnToEditor = useRef(false);
  const result = useRef<HTMLTextAreaElement>(null);
  const messageInput = useRef<HTMLTextAreaElement>(null);
  const current = topics.find(item => item.id === topic)!;
  const draft = drafts[topic];

  useEffect(() => setReady(true), []);
  useLayoutEffect(() => {
    if (prepared) {
      reviewTitle.current?.focus();
    } else if (returnToEditor.current) {
      returnToEditor.current = false;
      messageInput.current?.focus();
    }
  }, [prepared]);

  function updateDraft(key: keyof Draft, value: string) {
    setDrafts(previous => ({ ...previous, [topic]: { ...previous[topic], [key]: value } }));
    setStatus("");
  }

  function prepare(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = draft.message.trim();
    if (!message) {
      setStatus("Add a message before reviewing your draft.");
      messageInput.current?.focus();
      return;
    }

    const place = getEnvironment(space)?.name ?? (space === "other" ? "Other space" : "");
    const fields = [
      ["Name", reply.name],
      ["Email", reply.email],
      ["Phone", reply.phone],
      ["Organisation", reply.organisation],
      ["Space", topic === "project" ? place : ""],
      ["Location", draft.location],
      [topic === "project" ? "Timing" : "Planting or site", draft.context],
    ];
    const details = fields.flatMap(([label, value]) => value.trim() ? [label + ": " + value.trim()] : []);
    setPrepared(["Plantupia — " + current.label, ...details, "", message].join("\n"));
    setStatus("");
  }

  function edit() {
    returnToEditor.current = true;
    setPrepared("");
    setStatus("");
  }

  async function copy() {
    const text = prepared;
    try {
      await navigator.clipboard.writeText(text);
      if (result.current?.value === text) setStatus("Copied to your clipboard.");
    } catch {
      if (result.current?.value === text) {
        result.current.focus();
        result.current.select();
        setStatus("Draft selected. Use your device’s copy command.");
      }
    }
  }

  return <div className="contact-composer">
    <form className="contact-form" hidden={Boolean(prepared)} onSubmit={prepare}>
      <fieldset className="contact-topic">
        <legend>What’s your message about?</legend>
        <div className="contact-topic-choices">
          {topics.map(item => <label key={item.id}>
            <input type="radio" name="topic" value={item.id} checked={topic === item.id}
              onChange={() => { setTopic(item.id); setStatus(""); }} />
            <span>{item.label}</span>
          </label>)}
        </div>
      </fieldset>

      <div className="contact-fields">
        <div className="contact-message">
          {topic !== "general" && <div className="contact-context">
            {topic === "project" && <label>Type of space
              <select value={space} onChange={event => setSpace(event.target.value)}>
                <option value="">Choose a space</option>
                {environments.map(item => <option key={item.slug} value={item.slug}>{item.name}</option>)}
                <option value="other">Other space</option>
              </select>
            </label>}
            <label>City or area <small>Optional</small>
              <input value={draft.location} maxLength={100} onChange={event => updateDraft("location", event.target.value)} />
            </label>
            <label className={topic === "project" ? "contact-context-wide" : undefined}>
              {topic === "project" ? "Timing" : "Planting or site"} <small>Optional</small>
              <input value={draft.context} maxLength={150} onChange={event => updateDraft("context", event.target.value)} />
            </label>
          </div>}
          <label htmlFor="brief-message">Your message <small aria-hidden="true">Required</small></label>
          <textarea id="brief-message" ref={messageInput} required maxLength={2000} rows={5}
            value={draft.message} placeholder={current.prompt}
            onChange={event => updateDraft("message", event.target.value)} />
        </div>

        <fieldset className="contact-reply">
          <legend>Reply details <small>Optional</small></legend>
          {replyFields.map(field => <label key={field.key}>{field.label}
            <input type={field.type} autoComplete={field.autoComplete} maxLength={field.limit}
              value={reply[field.key]} onChange={event => {
                const value = event.target.value;
                setReply(previous => ({ ...previous, [field.key]: value }));
              }} />
          </label>)}
        </fieldset>
      </div>

      <div className="contact-actions">
        <p className="contact-feedback" role="status">{status}</p>
        <button className="contact-primary" type="submit" disabled={!ready}>Review draft <span aria-hidden="true">→</span></button>
      </div>
      <noscript><p>Enable JavaScript to prepare and copy your draft on this device.</p></noscript>
    </form>

    {prepared && <section className="contact-review" aria-labelledby="draft-heading">
      <div className="contact-review-header">
        <h2 id="draft-heading" ref={reviewTitle} tabIndex={-1}>Review your draft.</h2>
        <button className="contact-edit" type="button" onClick={edit}>← Edit details</button>
      </div>
      <label className="visually-hidden" htmlFor="prepared-message">Your prepared message</label>
      <textarea id="prepared-message" readOnly rows={10} value={prepared} ref={result} />
      <div className="contact-actions">
        <p className="contact-feedback" role="status">{status}</p>
        <button className="contact-primary" type="button" onClick={copy}>Copy message <span aria-hidden="true">↗</span></button>
      </div>
    </section>}
  </div>;
}
