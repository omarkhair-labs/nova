import SiteIndex from "./SiteIndex";
import "./company-story.css";

type VerifiedProject = {
  slug: string;
  name: string;
  location: string;
  environment: string;
  scope: string;
  description: string;
  cover: string;
  coverAlt: string;
  gallery: readonly { src: string; alt: string }[];
  publicationApproved: boolean;
};

// Populate only with company-authorized names, descriptions and original photos.
const projects: readonly VerifiedProject[] = [];

const services = [
  { index: "01", name: "Understand the place", text: "Light, material, people, everyday use and practical site constraints." },
  { index: "02", name: "Introduce living material", text: "Consider planting, placement, installation and the architecture around it." },
  { index: "03", name: "Keep the relationship alive", text: "Think about care, observation and continuity long after the first day." },
];

export function CompanyIntroduction() {
  return (
    <section className="cs-intro" id="about-plantupia" aria-labelledby="cs-intro-title">
      <div className="cs-intro-copy">
        <p className="cs-kicker">PLANTUPIA / A LIVING APPROACH</p>
        <h2 id="cs-intro-title">Wherever humans build,<br/><em>nature belongs.</em></h2>
        <p>Plantupia explores the relationship between people, plants and the built environment — across homes, hospitality, workplaces, industrial sites, retail and shared spaces. The ambition is not simply to add greenery, but to make living material belong to the place and plan for its care.</p>
        <a className="cs-link" href="/about">MEET PLANTUPIA <span aria-hidden="true">↗</span></a>
      </div>
      <div className="cs-image">
        <img src="https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=2200" alt="A tree within a sunlit contemporary concrete courtyard" loading="lazy"/>
        <span>ARCHITECTURAL REFERENCE · DAIPI / PEXELS</span>
      </div>
    </section>
  );
}

export default function CompanyStory() {
  const published = projects.filter(project => project.publicationApproved);
  return (
    <div className="cs-page">
      <header className="cs-header"><a href="/" className="cs-brand">◕ Plantupia</a><SiteIndex current="about" tone="dark"/><a href="/spaces" className="cs-header-link">EXPLORE SPACES ↗</a></header>
      <main>
        <CompanyIntroduction />
        <section className="cs-practice" aria-labelledby="cs-practice-title">
          <p className="cs-kicker">THE APPROACH / FROM PLACE TO CARE</p>
          <h2 id="cs-practice-title">Not just plants.<br/>A living relationship.</h2>
          <div className="cs-services">{services.map(item => <article key={item.index}><span>{item.index} /</span><h3>{item.name}</h3><p>{item.text}</p></article>)}</div>
          <a className="cs-link" href="/studio">EXPLORE THE DESIGN PROCESS ↗</a>
        </section>
        <section className="cs-work" id="selected-works" aria-labelledby="cs-work-title">
          <p className="cs-kicker">SELECTED WORKS / THE PROJECT ARCHIVE</p>
          <h2 id="cs-work-title">The work.<br/><em>The places.</em></h2>
          {published.length === 0 ? <p className="cs-empty">This project archive is in preparation. Actual project names, locations and original photographs will appear here once the company has reviewed and authorized the material.</p> :
            <div className="cs-portfolio">{published.map(project => <article key={project.slug} id={project.slug}>
              <img src={project.cover} alt={project.coverAlt} loading="lazy"/>
              <div><p className="cs-kicker">{project.environment} / {project.location}</p><h3>{project.name}</h3><p>{project.scope}</p><p>{project.description}</p></div>
              {project.gallery.map(photo => <img key={photo.src} src={photo.src} alt={photo.alt} loading="lazy"/>)}
            </article>)}</div>}
          <a className="cs-link" href="/start-a-space">START WITH YOUR SPACE ↗</a>
        </section>
      </main>
      <footer className="cs-footer"><a href="/">PLANTUPIA / HOME</a><a href="/living-systems">LIVING SYSTEMS ↗</a><span>DESIGN PREVIEW / 2026</span></footer>
    </div>
  );
}
