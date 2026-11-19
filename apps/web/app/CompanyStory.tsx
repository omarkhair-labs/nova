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

export function CompanyIntroduction({ privateReview = false, placement = "home" }: { privateReview?: boolean; placement?: "home" | "about" }) {
  const about = placement === "about";
  const kicker = about ? "PLANTUPIA / THE PRACTICE" : "PLANTUPIA / BETWEEN ARCHITECTURE AND CARE";
  const title = about ? <>Wherever humans build,<br/><em>nature belongs.</em></> : <>Greenery belongs in the plan,<br/><em>not at the end.</em></>;
  const body = about
    ? "Plantupia is a green technology and biophilic integration company. We work across design, planting, installation and continuing care for hospitality, commercial destinations and workplaces."
    : "Plantupia works between architecture and living material: reading the place, designing the planting, introducing it into the site and planning for the care that follows.";
  return (
    <section className="cs-intro" id="about-plantupia" data-private-review={privateReview} aria-labelledby="cs-intro-title">
      <div className="cs-intro-copy">
        <p className="cs-kicker">{kicker}</p>
        <h2 id="cs-intro-title">{title}</h2>
        <p>{body}</p>
        <a className="cs-link" href={about ? "/studio" : "/about"}>{about ? "SEE THE DESIGN PROCESS" : "MEET PLANTUPIA"} <span aria-hidden="true">&#8599;</span></a>
      </div>
      <div className="cs-image">
        <img src={privateReview ? "/api/private-arrival/company-intro.webp" : "https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=2200"} alt={privateReview ? "Design study of upright greenery in white cylindrical planters within an Audi showroom interior" : "A tree within a sunlit contemporary concrete courtyard"} loading="lazy"/>
        <span>{privateReview ? "AUDI CAIRO / BETWEEN THE CARS / DESIGN STUDY, NOT A VERIFIED INSTALLATION" : "ARCHITECTURAL REFERENCE / DAIPI / PEXELS"}</span>
      </div>
    </section>
  );
}

export default function CompanyStory({ privateReview = false }: { privateReview?: boolean }) {
  const published = projects.filter(project => project.publicationApproved);
  return (
    <div className="cs-page">
      <header className="cs-header"><a href="/" className="cs-brand">◕ Plantupia</a><SiteIndex current="about" tone="dark"/><a href="/spaces" className="cs-header-link">EXPLORE SPACES ↗</a></header>
      <main>
        <CompanyIntroduction privateReview={privateReview} placement="about" />
        {privateReview&&<section className="cs-focus" aria-labelledby="cs-focus-title"><div className="cs-focus-head"><div><p className="cs-kicker">PRIMARY B2B FOCUS / MEETING DIRECTION</p><h2 id="cs-focus-title">Three places to begin.</h2></div><p>Hospitality, commercial destinations and offices/HQs were identified as the main business focus in the supplied meeting. Residential work remains possible, but it is not the campaign priority.</p></div><div className="cs-focus-grid"><a href="/spaces/hospitality"><figure><img src="/api/private-arrival/spaces-hospitality.webp" alt="El Hallab restaurant interior planting design proposal"/><figcaption>HOSPITALITY / PROPOSAL STUDY</figcaption></figure><span>01</span><h3>Hospitality</h3><p>Restaurants, cafés and hotels—from arrival to atmosphere and care.</p></a><a href="/spaces/commercial"><figure><img src="/api/private-arrival/world-spaces.webp" alt="Pixels Mall commercial biophilic design proposal"/><figcaption>COMMERCIAL / PROPOSAL STUDY</figcaption></figure><span>02</span><h3>Commercial</h3><p>Developers, malls and showrooms—architecture, landscape and brand identity together.</p></a><a href="/spaces/offices"><figure><img src="/api/private-arrival/spaces-workplace-palette.webp" alt="Plantupia catalogue planting and planter collection illustration"/><figcaption>OFFICES &amp; HQs / CATALOGUE STUDY</figcaption></figure><span>03</span><h3>Offices &amp; HQs</h3><p>Planting, custom vessels and continuing maintenance for working environments.</p></a></div></section>}
        <section className="cs-practice" aria-labelledby="cs-practice-title">
          <p className="cs-kicker">THE APPROACH / FROM PLACE TO CARE</p>
          <h2 id="cs-practice-title">Not just plants.<br/>A living relationship.</h2>
          <div className="cs-services">{services.map(item => <article key={item.index}><span>{item.index} /</span><h3>{item.name}</h3><p>{item.text}</p></article>)}</div>
          <a className="cs-link" href="/studio">EXPLORE THE DESIGN PROCESS ↗</a>
        </section>
        <section className="cs-work" id="selected-works" aria-labelledby="cs-work-title">
          <p className="cs-kicker">SELECTED WORKS / THE PROJECT ARCHIVE</p>
          <h2 id="cs-work-title">The work.<br/><em>The places.</em></h2>
          {privateReview?<div className="cs-archive-preview"><div><span>06</span><p>source-led proposal studies currently mapped in the private archive</p></div><div><span>03</span><p>primary B2B sectors from the meeting direction</p></div><div><span>01</span><p>living approach connecting design, installation and continuing care</p></div><p className="cs-archive-note">These are proposal studies and catalogue evidence. Installation status and publication permission are not assumed.</p><a className="cs-link" href="/spaces/projects">EXPLORE THE PROJECT ARCHIVE ↗</a></div>:published.length === 0 ? <p className="cs-empty">This project archive is in preparation. Actual project names, locations and original photographs will appear here once the company has reviewed and authorized the material.</p> :
            <div className="cs-portfolio">{published.map(project => <article key={project.slug} id={project.slug}>
              <img src={project.cover} alt={project.coverAlt} loading="lazy"/>
              <div><p className="cs-kicker">{project.environment} / {project.location}</p><h3>{project.name}</h3><p>{project.scope}</p><p>{project.description}</p></div>
              {project.gallery.map(photo => <img key={photo.src} src={photo.src} alt={photo.alt} loading="lazy"/>)}
            </article>)}</div>}
          <a className="cs-link" href="/start-a-space">GET IN TOUCH ↗</a>
        </section>
      </main>
      <footer className="cs-footer"><a href="/">PLANTUPIA / HOME</a><a href="/living-systems">LIVING SYSTEMS ↗</a><span>DESIGN PREVIEW / 2026</span></footer>
    </div>
  );
}
