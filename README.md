# Nova

**A native Android social product built around people, shared moments, media, and memory.**

[Portfolio case study](https://omar-khair-portfolio.vercel.app/work/nova) · [Google Play](https://play.google.com/store/apps/details?id=com.omarkhair70.nova) · [Join closed test](https://groups.google.com/g/nova-closed-testers)

> **Release channel:** Google Play closed testing.  
> **Repository status:** final whole-product coherence closure merged on August 28, 2026. This README does not claim a public production rollout.

<p align="center">
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/01-home-dashboard.webp" alt="Nova Home" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/02-orbit.webp" alt="Nova Orbit" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/04-reel.webp" alt="Nova Reel" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/07-messaging-calls.webp" alt="Nova Messaging and Calls" width="23%" />
</p>

## What Nova is

Nova is a relationship-centered social product whose core idea is:

> **Nova is the social app that remembers your life with your people.**

It combines a native Kotlin Android client with a Django backend and real-time systems. The product is intentionally broader than a feed clone: persistent posts, Stories, Pulse, Reels, Rooms, Orbit, Tonight, messaging, calls, Memories, security, and privacy are designed as one governed product system.

## Strongest product systems

1. **Social core** — Home, Feed, Posts, Post Detail, Likes, Reposts, sharing, comments, replies, optimistic interaction and cache-first returning-session behavior.
2. **Create + media pipeline** — Post/Story/Pulse/Reel creation, H.264/AAC video preparation, thumbnails, WorkManager publishing, retryable upload and media continuity.
3. **Immersive media** — Reels, Stories and Pulse with shared playback, real-duration media behavior, loading/error/retry states and Nova-owned interaction language.
4. **People + relationship surfaces** — Profiles, People discovery, Orbit, Tonight, Activity, Follow Requests and relationship-aware navigation.
5. **Messaging + calls** — Inbox, conversations, drafts, reactions, voice/photo media, shared content, group flows, realtime reconciliation and call presentation.
6. **Rooms + Memories** — shared social places, plans/reminders/media, reflective Memories/Your Week and the protected Memory Film pipeline.
7. **Privacy + security** — notification preferences, Close Friends, blocked accounts, app lock, device sessions, account deletion and external-destination safety.

## Product screens

<p align="center">
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/03-tonight.webp" alt="Nova Tonight" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/05-create-hub.webp" alt="Nova Create Hub" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/06-post-detail.webp" alt="Nova Post Detail" width="23%" />
  <img src="https://raw.githubusercontent.com/omarkhair70-droid/omar-khair-portfolio/main/public/work/nova/08-profile.webp" alt="Nova Profile" width="23%" />
</p>

## Architecture

```text
Native Android app
Kotlin + Jetpack Compose
        │
        ├── feature-owned state/repositories
        ├── Nova design system + shared UI
        ├── WorkManager media publishing
        ├── media / playback foundations
        └── realtime + call clients
                │
                ▼
Django + Django REST Framework
        │
        ├── PostgreSQL
        ├── Redis / Channels / WebSockets
        ├── media + social APIs
        └── realtime / product services
```

The repository keeps UI presentation shared while feature state and domain ownership remain feature-owned. Architecture consolidation was staged and behavior-preserving rather than a rewrite.

## Visual direction

Nova's approved identity is **Quiet Orbit**: warm canvas, Nova violet, relationship/orbit motifs, rounded-line owned iconography, soft spatial motion, and content-first media.

The product avoids generic card-heavy social UI and does not collapse Posts, Stories, Pulse, Reels, Rooms, and Memories into one format.

## Release identity

- Android application ID: `com.omarkhair70.nova`
- Target SDK: 36
- Release channel represented here: Google Play closed testing
- Public production rollout is **not** claimed by this README.

## Engineering reference

The permanent continuity and architecture references live under `docs/`, including:

- `docs/NOVA_MASTER_PROJECT_REFERENCE.md`
- `docs/NOVA_MASTER_RECOVERY_AND_EXECUTION_REFERENCE.md`
- `docs/design/`
- `docs/architecture/`
- `docs/product/`

## Local development

The repository contains both the Android application and Django backend. See the repo docs and `backend/README.md` for environment-specific setup and validation.
