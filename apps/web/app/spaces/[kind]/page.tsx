import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { environments, getEnvironment } from "../../environments";
import { Invitation, SiteFrame, TextLink } from "../../SiteFrame";
import { Photograph } from "../../Photograph";

type Props = { params: Promise<{ kind: string }> };

export const dynamicParams = false;

export function generateStaticParams() {
  return environments.map(item => ({ kind: item.slug }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const item = getEnvironment((await params).kind);
  return item ? { title: item.name + " — Plantupia", description: item.intro } : {};
}

export default async function EnvironmentPage({ params }: Props) {
  const item = getEnvironment((await params).kind);
  if (!item) notFound();

  const next = environments[(environments.indexOf(item) + 1) % environments.length];
  return <SiteFrame current="/spaces">
    <div className="field-shell field-detail">
      <TextLink href="/spaces" arrow="←">All spaces</TextLink>
      <section className="field-detail-opening">
        <div>
          <p className="field-kicker"><span /> {item.name}</p>
          <h1>{item.title}</h1>
          <p className="field-lead">{item.intro}</p>
        </div>
        <Photograph image={item.image} caption={item.name + " · Generated concept"} priority />
      </section>
      <section className="field-detail-questions" aria-labelledby="detail-question-title">
        <h2 id="detail-question-title">Begin with <em>the place.</em></h2>
        <dl>{item.details.map(note => <div key={note.title}><dt>{note.title}</dt><dd>{note.text}</dd></div>)}</dl>
      </section>
      <div className="field-detail-next">
        <TextLink href="/studio">Explore our approach</TextLink>
        <TextLink href={"/spaces/" + next.slug}>Next space · {next.name}</TextLink>
      </div>
    </div>
    <Invitation title="Tell us about your space." href={"/start-a-space?space=" + item.slug} />
  </SiteFrame>;
}
