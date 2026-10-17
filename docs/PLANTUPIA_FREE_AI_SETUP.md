# Plantupia — zero-payment AI trial (draft, no deployment)

The existing Android assistant and Django endpoint are real, not scripted.
This PR adds a second optional server-side provider: GroqCloud Chat Completions.

## Activation only on an isolated QA backend

1. Create a **GroqCloud Free Plan** project at https://console.groq.com. Check its current rate limits and model availability. Do not enable a billable plan unless separately approved.
2. Keep the key in the **QA server's secure environment-variable manager**, never in Android assets, GitHub, a screenshot, or a chat.
3. In that QA service, configure:
   - `PLANTUPIA_AI_ENABLED=1`
   - `PLANTUPIA_AI_PROVIDER=groq`
   - `PLANTUPIA_AI_MODEL=openai/gpt-oss-20b`
   - `PLANTUPIA_AI_API_KEY` = secure secret from GroqCloud
   - `REDIS_URL` = QA-owned shared Redis rate-limit store (mandatory in production-mode Django).
4. Test with **synthetic, nonsensitive** questions before any consented private plant context.
5. Ensure the Android QA variant points at the isolated backend; do **not** deploy Plantupia Django migrations or secrets to Nova production.
6. Verify an actual response in QA on the device; a mocked unit test or an APK build does **not** mean the remote provider has been activated.

## Cost and privacy boundaries

- Free-tier availability and model IDs can change; daily quotas are capped and a free tier is not an unlimited zero-cost guarantee.
- This backend already limits each account to 35 AI questions/day, bounds conversation history and sends plant observations **only after explicit per-request opt-in**.
- Free-tier inference still sends any typed question (and opted-in context) to a third party. Confirm consent and data policy; for private/company records prefer disabled context and check Groq's Data Controls/Zero Data Retention policy.
- The AI response is informational, cannot directly create care records, cannot access sensors or make unverified irrigation instructions.
- On HTTP errors or quota exhaustion the app displays an unavailable state, never generated fake guidance.
- Keep the OpenAI adapter intact: it is selected by `PLANTUPIA_AI_PROVIDER=openai` and has separately priced usage.

Official docs:
- https://console.groq.com/docs/api-reference
- https://console.groq.com/docs/rate-limits
- https://console.groq.com/docs/your-data
