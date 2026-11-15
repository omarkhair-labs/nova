import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { environments, getEnvironment } from "../../environments";
import { SiteFrame } from "../../SiteFrame";
import { SceneBackdrop, SceneCredit, SceneNote } from "../../SpatialStage";

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
    <section className="scene-world place-world">
      <SceneBackdrop image={item.image} />
      <div className="scene-shadow" aria-hidden="true" />
      <div className="place-note">
        <SceneNote label={item.name} title={item.title}>
          <p>{item.intro}</p>
          <a className="world-action" href={"/start-a-space?space=" + item.slug}>Prepare a project brief <span aria-hidden="true">↗</span></a>
        </SceneNote>
        <details className="scene-drawer">
          <summary>Read this place <span aria-hidden="true">+</span></summary>
          <dl>{item.details.map(note => <div key={note.title}><dt>{note.title}</dt><dd>{note.text}</dd></div>)}</dl>
        </details>
        <div className="scene-exits"><a href="/spaces">← All spaces</a><a href={"/spaces/" + next.slug}>Next · {next.name} ↗</a></div>
      </div>
      <SceneCredit>{item.name} · Generated concept</SceneCredit>
    </section>
  </SiteFrame>;
}
