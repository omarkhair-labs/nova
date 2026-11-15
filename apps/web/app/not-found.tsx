import { SiteFrame, TextLink } from "./SiteFrame";

export default function NotFound() {
  return <SiteFrame>
    <section className="field-shell field-page-intro field-missing">
      <p className="field-kicker"><span /> Page unavailable</p>
      <h1>Take another path.</h1>
      <p className="field-lead">This page could not be found.</p>
      <TextLink href="/" arrow="←">Return home</TextLink>
      <TextLink href="/spaces">Explore spaces</TextLink>
    </section>
  </SiteFrame>;
}
