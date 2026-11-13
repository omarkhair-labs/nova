const spaceTypes = [
  {
    number: "01",
    name: "Workplaces",
    note: "Where ideas and everyday life meet.",
    composition: "work",
  },
  {
    number: "02",
    name: "Hospitality",
    note: "Places people want to stay a little longer.",
    composition: "stay",
  },
  {
    number: "03",
    name: "Living spaces",
    note: "Nature with room to become part of life.",
    composition: "live",
  },
] as const;

const phases = [
  { n: "01", title: "Understand", text: "We begin with the people, place, light, and ways a space is actually used." },
  { n: "02", title: "Imagine", text: "An intentional green concept: living material, proportion, atmosphere, and care." },
  { n: "03", title: "Bring to life", text: "From plant selection and placement to installation in the real environment." },
  { n: "04", title: "Keep living", text: "The story continues through observation, thoughtful care, and what changes over time." },
] as const;

function Wordmark() {
  return (
    <span className="wordmark">
      <svg className="brand-symbol" viewBox="0 0 44 44" fill="none" aria-hidden="true">
        <circle cx="22" cy="22" r="20.5" stroke="currentColor" strokeWidth="1" />
        <path d="M22 34V13M22 25c-7 0-11-4-11-12 8 0 11 5 11 12ZM22 21c0-8 4-12 12-12 0 8-4 12-12 12Z" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      <span>plantupia<span className="wordmark-dot">.</span></span>
    </span>
  );
}

function Arrow({ diagonal = false }: { diagonal?: boolean }) {
  return (
    <svg className="arrow" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      {diagonal
        ? <path d="M5 19 19 5M8 5h11v11" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
        : <path d="M4 12h16m-6-6 6 6-6 6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />}
    </svg>
  );
}

function BotanicalStudy() {
  return (
    <div className="living-figure" aria-label="Original abstract botanical line illustration">
      <div className="figure-orbit figure-orbit-one" />
      <div className="figure-orbit figure-orbit-two" />
      <svg viewBox="0 0 600 710" className="botanical-drawing" fill="none" aria-hidden="true">
        <path d="M301 652c-2-96 0-158-8-229-5-52-25-102-35-163" stroke="#163b2c" strokeWidth="3" strokeLinecap="round" />
        <path d="M294 485c61-69 82-140 93-216M289 421c-75-41-112-98-131-157M277 358c55-76 59-142 66-210" stroke="#163b2c" strokeWidth="2.8" strokeLinecap="round" />
        <path d="M260 282c-58-7-113-41-127-117 64-5 126 35 127 117Z" fill="#b0bf9d" stroke="#163b2c" strokeWidth="2" />
        <path d="M162 269c-59 8-107-13-135-67 74-23 123-8 135 67Z" fill="#d8ddc2" stroke="#163b2c" strokeWidth="2" />
        <path d="M343 156c-31-51-25-109 17-139 31 56 25 106-17 139Z" fill="#90ab87" stroke="#163b2c" strokeWidth="2" />
        <path d="M386 274c4-72 40-120 106-136 3 80-37 128-106 136Z" fill="#d6d9ac" stroke="#163b2c" strokeWidth="2" />
        <path d="M294 426c-64 12-115-2-152-56 64-35 125-17 152 56Z" fill="#879f81" stroke="#163b2c" strokeWidth="2" />
        <path d="M304 476c22-69 72-105 140-97-18 75-67 109-140 97Z" fill="#a8c29b" stroke="#163b2c" strokeWidth="2" />
        <path d="M292 568c-65 7-111-17-145-75 74-22 125 4 145 75Z" fill="#c1c9a0" stroke="#163b2c" strokeWidth="2" />
        <path d="M304 590c26-60 67-83 122-69-20 65-59 90-122 69Z" fill="#759678" stroke="#163b2c" strokeWidth="2" />
        <path d="M131 167c35 52 77 82 125 114M361 21c-10 43-17 91-18 133M493 141c-38 58-78 103-105 132M144 372c56 16 103 38 148 54M441 380c-57 36-105 65-137 96M148 495c49 37 92 60 141 72M424 522c-43 31-83 52-120 67" stroke="#163b2c" strokeWidth="1.1" strokeLinecap="round" opacity=".55" />
        <path d="M238 659c34-13 84-12 126 0l-15 30h-96l-15-30Z" fill="#a98060" stroke="#163b2c" strokeWidth="2" />
        <path d="M227 655h147" stroke="#163b2c" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span className="specimen-label specimen-label-one">FIG. 001<br />LIVING MATERIAL</span>
      <span className="specimen-label specimen-label-two">NATURE × TIME<br />ORIGINAL STUDY</span>
      <span className="figure-coordinate">29°59′ N / 31°25′ E</span>
    </div>
  );
}

export default function HomePage() {
  return (
    <>
      <a className="skip-link" href="#main">Skip to content</a>
      <header className="site-header">
        <a href="#" aria-label="Plantupia home"><Wordmark /></a>
        <nav className="desktop-nav" aria-label="Main navigation">
          <a href="#philosophy">Our thinking</a>
          <a href="#spaces">Spaces</a>
          <a href="#continuity">Living layer</a>
          <a href="#process">How we work</a>
        </nav>
        <div className="header-tools">
          <a className="language-link" href="/ar/" hrefLang="ar" lang="ar" aria-label="العربية — عرض الموقع باللغة العربية">العربية</a>
          <a href="#contact" className="header-contact">Start a conversation <Arrow diagonal /></a>
        </div>
        <details className="mobile-nav">
          <summary aria-label="Open navigation menu">Menu <span>＋</span></summary>
          <nav aria-label="Mobile navigation">
            <a href="#philosophy">Our thinking</a>
            <a href="#spaces">Spaces</a>
            <a href="#continuity">Living layer</a>
            <a href="#process">How we work</a>
            <a href="#contact">Contact</a>
            <a href="/ar/" hrefLang="ar" lang="ar">العربية</a>
          </nav>
        </details>
      </header>

      <main id="main">
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-content">
            <p className="eyebrow"><span className="eyebrow-rule" /> Nature / Architecture / People</p>
            <h1 id="hero-title">Spaces<br />that <em>keep</em><br />living<span className="period">.</span></h1>
            <p className="hero-description">We bring the natural world into the built one — creating considered green spaces that grow into the lives around them.</p>
            <a className="text-cta" href="#philosophy">Discover our approach <span className="cta-circle"><Arrow /></span></a>
            <div className="hero-bottom"><span>DESIGNED WITH LIFE IN MIND</span><span>SCROLL TO EXPLORE ↓</span></div>
          </div>
          <BotanicalStudy />
          <div className="hero-side-note">TRANSFORMING SPACES. INSPIRING PEOPLE.</div>
        </section>

        <section className="statement" id="philosophy" aria-labelledby="statement-title">
          <p className="section-index">01 / A DIFFERENT WAY TO THINK GREEN</p>
          <div className="statement-grid">
            <h2 id="statement-title">Not just something<br />you <em>look at.</em></h2>
            <div className="statement-aside">
              <p>Something you live with.</p>
              <p>Greenery should belong to a space — to its architecture, its everyday rhythm, and the people who make it theirs.</p>
              <p>For us, that connection starts in the design. It continues long after installation.</p>
            </div>
          </div>
          <div className="belief-line">
            <span>NATURE IS NOT THE FINISHING TOUCH.</span>
            <span>IT IS PART OF THE EXPERIENCE.</span>
          </div>
        </section>

        <section className="spaces-section" id="spaces" aria-labelledby="spaces-title">
          <div className="section-heading">
            <div>
              <p className="section-index">02 / WHERE LIFE HAPPENS</p>
              <h2 id="spaces-title">Different places.<br /><em>Same instinct.</em></h2>
            </div>
            <p>From the quiet corner to the shared environment, we think about greenery as part of the space — not something placed on top of it.</p>
          </div>
          <div className="space-grid">
            {spaceTypes.map((space) => (
              <article className={"space-tile space-" + space.composition} key={space.number}>
                <span className="space-num">{space.number} / SPACE TYPOLOGY</span>
                <div className="space-art" aria-hidden="true">
                  <span className="space-arc arc-one" /><span className="space-arc arc-two" />
                  <span className="space-block block-one" /><span className="space-block block-two" />
                  <span className="space-stem" /><span className="space-leaf leaf-one" /><span className="space-leaf leaf-two" />
                </div>
                <div className="space-caption">
                  <h3>{space.name}</h3>
                  <p>{space.note}</p>
                </div>
              </article>
            ))}
          </div>
          <p className="space-footnote">These are the kinds of spaces we design for — not a gallery of completed projects. Verified project photography will be added with Plantupia’s approval.</p>
        </section>

        <section className="continuity" id="continuity" aria-labelledby="continuity-title">
          <div className="continuity-copy">
            <p className="section-index light">03 / THE LIVING LAYER</p>
            <h2 id="continuity-title">Installation<br />is only <em>day one.</em></h2>
            <p>A living space changes. Plants settle, people notice, and care leaves its own quiet record. We are shaping a digital companion to help that story remain connected.</p>
            <div className="continuity-steps">
              <span>PLACE</span><i aria-hidden="true" /><span>PLANT</span><i aria-hidden="true" /><span>CARE</span><i aria-hidden="true" /><span>TIME</span>
            </div>
            <p className="development-note">Plantupia's customer mobile experience is in development. The illustration is conceptual; no live account data is shown.</p>
          </div>
          <div className="phone-stage" aria-hidden="true">
            <div className="phone-shell">
              <div className="phone-status"><span>plantupia.</span><span>○ ○ ○</span></div>
              <p className="phone-eyebrow">A LIVING RECORD</p>
              <h3>Every plant<br />has a story.</h3>
              <div className="phone-plant-art">
                <svg viewBox="0 0 240 220" fill="none">
                  <path d="M120 215V46M119 147c-47-6-65-39-64-77 54 4 73 41 64 77ZM121 110c7-52 29-72 76-76-8 54-31 76-76 76ZM119 183c-38-18-54-47-53-76 44 11 61 36 53 76ZM122 170c19-35 49-48 83-43-17 40-45 56-83 43Z" stroke="#244b35" strokeWidth="2" strokeLinejoin="round" fill="#b2c9a6" />
                </svg>
              </div>
              <div className="phone-moment"><span>01 · PLACE</span><span>Where it lives</span></div>
              <div className="phone-moment"><span>02 · CARE</span><span>What was noticed</span></div>
              <div className="phone-moment"><span>03 · TIME</span><span>What changed</span></div>
            </div>
            <span className="phone-stage-caption">AN ILLUSTRATIVE STUDY / NOT USER DATA</span>
          </div>
        </section>

        <section className="process" id="process" aria-labelledby="process-title">
          <p className="section-index">04 / THE WAY WE THINK</p>
          <div className="process-heading"><h2 id="process-title">Designed to<br /><em>belong.</em></h2><p>A relationship between place, plants and people, considered from the first conversation onward.</p></div>
          <div className="process-list">
            {phases.map((phase) => (
              <div className="process-row" key={phase.n}>
                <span className="process-number">{phase.n}</span>
                <h3>{phase.title}</h3>
                <p>{phase.text}</p>
                <span className="process-mark" aria-hidden="true">↗</span>
              </div>
            ))}
          </div>
        </section>

        <section className="closing" id="contact" aria-labelledby="closing-title">
          <div className="closing-inner">
            <p className="section-index light">05 / BEGIN A CONVERSATION</p>
            <h2 id="closing-title">Have a place<br />in <em>mind?</em></h2>
            <p>Every living space begins with a different question. We would love to hear yours.</p>
            <a className="closing-link" href="https://plantupia.com/" target="_blank" rel="noreferrer">Contact Plantupia via the current website <Arrow diagonal /></a>
            <p className="closing-note">This is a review concept. Contact details, project photography and launch claims remain subject to company approval.</p>
          </div>
          <span className="closing-outline" aria-hidden="true">grow</span>
        </section>
      </main>

      <footer className="site-footer">
        <Wordmark />
        <span>NATURE · ARCHITECTURE · PEOPLE</span>
        <span>© 2026 PLANTUPIA / WEBSITE CONCEPT</span>
      </footer>
    </>
  );
}
