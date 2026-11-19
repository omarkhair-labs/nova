# Start a Space / preview UX audit — 2026-11-19

Problem found: when the website inbox is disabled on preview, the entire form fieldset was disabled. This prevented reviewing the actual enquiry experience even though submission was correctly blocked.

Change:
- Preview fields, textarea, modes and optional Field Notes are now explorable/editable while inbox is unavailable.
- Final submit remains disabled when available=false, and send() still returns immediately if unavailable. Nothing is submitted or stored.
- Explicit Preview mode notice tells the reviewer that composing is local-only.
- Stable environment IDs retained. Display labels aligned with meeting priorities: Offices / HQs / workplace; Hospitality / hotel / restaurant / cafe; Commercial / mall / developer / showroom.
- Existing anti-spam, idempotency, request timeout and backend submission path are untouched.

Verification at 320/390/768/1440: preview notice visible, submit disabled, inputs editable, work environment preselected from ?space=work, Rhythm + Responsibility loaded from focus=0,2, notes successfully inserted into local message, Care mode hides project-space selector, General mode hides location, no overflow, zero page errors. No real enquiry sent. Evidence: temp/contact-preview-review/results.json and screenshots.