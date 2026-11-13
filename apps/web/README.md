# Plantupia Public Website — Design/Engineering Slice 01

**Status: Draft / not deployed / not approved for public launch.**

This is the experiential public website, **not** the customer Android app, commercial checkout, or the `/company/` staff workspace.

## Intent
Create a distinctive Plantupia public identity around nature, architecture and people. Move from physical space design and installation into the longer-lived relationship of care, while keeping the commercial and digital claims truthful.

## What is implemented
- Standalone Next.js 16 App Router + TypeScript at `apps/web/`.
- Responsive, keyboard-navigable English-first public homepage with original botanical SVG and CSS composition.
- Sections: philosophy, space typologies (clearly not named client projects), living/digital layer (clearly in development), process, and staged contact via the existing site.
- No stock photographs presented as company installations; no fake product SKUs, checkout, active sensor reading, projects, testimonials, or integration to private APIs.
- SEO indexing explicitly disabled until reviewed launch approval.

## Preview locally
```bash
cd apps/web
npm install
npm run typecheck
npm run build
npm run dev
```
Node.js 20.9+ recommended (Node 22 preferred).

## Next slices, gated on company evidence
1. Approve brand logo, typography, voice, correct legal contact details and brand/domain ownership; obtain licensed real project photos and project permissions.
2. Replace typology studies with real case studies (location, brief, visual material, installation, aftercare) only after approval.
3. Add Arabic content and genuine RTL locale review, motion/accessibility audits (reduced motion, 320–430dp, keyboard).
4. Connect public product catalogue only after reviewed published `CommerceSKU` data and marketing/commerce operating policies exist. No secondary demo checkout.
5. Contact/lead handling: define privacy, anti-spam and data retention; use approved backend or provider, never a silently dead form.
6. Only then configure preview hosting, real domain cutover, monitoring, sitemap/indexing, analytics consent and launch approval.

`plantupia.com` remains untouched; this branch contains no GoDaddy, DNS, Company Dashboard or Production deployment action.
