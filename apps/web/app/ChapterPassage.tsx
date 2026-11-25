import { siteSections, type SiteSection } from "./site-sections";
import "./chapter-passage.css";

/** A visible route onward, including before JavaScript has loaded. */
export default function ChapterPassage({ current }: { current: SiteSection }) {
  const currentIndex = siteSections.findIndex(section => section.id === current);
  const next = currentIndex >= 0 ? siteSections[currentIndex + 1] : undefined;
  if (!next) return null;

  return (
    <nav className="ch-passage" aria-label="Continue through Plantupia" data-next-chapter={Number(next.number)}>
      <span className="ch-kicker">NEXT / {next.number}</span>
      <a href={next.href} className="ch-main" aria-label={"Continue to " + next.name}>
        <span className="ch-copy"><strong>{next.name}</strong><small>{next.line}</small></span>
        <svg className="ch-arrow" viewBox="0 0 40 40" fill="none" aria-hidden="true">
          <path d="M8 20h24M23 11l9 9-9 9" stroke="currentColor" strokeWidth="1.5" />
        </svg>
      </a>
    </nav>
  );
}
