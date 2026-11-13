import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "بلانتيبيا | مساحات تستمر في الحياة",
  description: "بلانتيبيا: الطبيعة والعمارة والناس. نصمم تجربة المساحات الخضراء لتكون جزءًا من الحياة اليومية وتستمر بالعناية.",
  robots: { index: false, follow: false },
  alternates: { languages: { en: "/", ar: "/ar/" } },
};

const spaces = [
  { id: "٠١", label: "مساحات العمل", text: "طبيعة لها مكان في إيقاع اليوم، والحركة، والتفكير." },
  { id: "٠٢", label: "الضيافة", text: "مساحات تترك أثرًا في تجربة الناس وذاكرتهم." },
  { id: "٠٣", label: "مساحات المعيشة", text: "حضور أخضر يتعايش مع المكان وأصحابه." },
];

const stages = [
  { id: "٠١", title: "نفهم", text: "نبدأ بالمكان: ضوءه، والناس اللي بيستخدموه، وطريقة حياتهم فيه." },
  { id: "٠٢", title: "نتخيّل", text: "نبحث عن علاقة طبيعية بين النباتات والعمارة وتفاصيل المساحة." },
  { id: "٠٣", title: "نُحيي", text: "يتحوّل التصور إلى اختيارات وتوزيع وتركيب في المكان الحقيقي." },
  { id: "٠٤", title: "نستمر", text: "لا تنتهي الحكاية بالتركيب؛ الملاحظة والعناية جزء من عمر المكان." },
];

function PlantMark() {
  return (
    <span className="wordmark" lang="en" dir="ltr">
      <svg className="brand-symbol" viewBox="0 0 44 44" fill="none" aria-hidden="true">
        <circle cx="22" cy="22" r="20.5" stroke="currentColor" strokeWidth="1" />
        <path d="M22 34V13M22 25c-7 0-11-4-11-12 8 0 11 5 11 12ZM22 21c0-8 4-12 12-12 0 8-4 12-12 12Z" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      <span>plantupia<span className="wordmark-dot">.</span></span>
    </span>
  );
}

function Leaf() {
  return (
    <svg className="arabic-hero-illustration" viewBox="0 0 480 560" fill="none" aria-hidden="true">
      <circle cx="241" cy="256" r="180" stroke="#456a4c" strokeOpacity=".18" />
      <circle cx="241" cy="256" r="135" stroke="#456a4c" strokeOpacity=".24" strokeDasharray="5 6" />
      <path d="M234 514c-2-92 4-155-4-227-7-52-22-98-29-147" stroke="#183c2e" strokeWidth="3" strokeLinecap="round" />
      <path d="M228 386c69-52 88-111 106-159M228 330c-62-35-100-85-119-146M227 288c57-47 72-108 87-165" stroke="#183c2e" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M199 150c-60 8-101-21-116-84 71-12 114 26 116 84Z" fill="#b2c39f" stroke="#183c2e" strokeWidth="2" />
      <path d="M111 187c-58 15-103-3-125-52 69-24 111-11 125 52Z" fill="#d4dfc2" stroke="#183c2e" strokeWidth="2" transform="translate(30 0)" />
      <path d="M315 126c-27-47-14-96 30-115 24 49 14 93-30 115Z" fill="#8daa85" stroke="#183c2e" strokeWidth="2" />
      <path d="M334 229c6-62 43-106 104-109-1 72-40 114-104 109Z" fill="#cedbb5" stroke="#183c2e" strokeWidth="2" />
      <path d="M229 388c-59 9-103-15-132-68 68-24 114 4 132 68Z" fill="#789981" stroke="#183c2e" strokeWidth="2" />
      <path d="M229 426c28-53 71-78 117-61-24 57-64 78-117 61Z" fill="#b4c6a4" stroke="#183c2e" strokeWidth="2" />
      <path d="M225 477c-55 6-98-19-123-68 65-16 105 10 123 68Z" fill="#c9d2a7" stroke="#183c2e" strokeWidth="2" />
      <path d="M228 490c21-49 58-68 100-58-18 49-54 72-100 58Z" fill="#91ac8d" stroke="#183c2e" strokeWidth="2" />
      <path d="M191 512h86l13 22h-112l13-22Z" fill="#af8e70" stroke="#183c2e" strokeWidth="2" />
      <path d="M185 511h95" stroke="#183c2e" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export default function ArabicHomePage() {
  return (
    <div className="arabic-page" dir="rtl" lang="ar">
      <a className="skip-link" href="#arabic-main">تجاوز القائمة والانتقال إلى المحتوى</a>
      <header className="site-header">
        <a href="/ar/" aria-label="الصفحة الرئيسية لبلانتيبيا"><PlantMark /></a>
        <nav className="desktop-nav" aria-label="التنقل الرئيسي">
          <a href="#thinking">فكرتنا</a>
          <a href="#spaces">المساحات</a>
          <a href="#digital">التجربة الرقمية</a>
          <a href="#approach">رحلتنا</a>
        </nav>
        <div className="header-tools">
          <a className="language-link" href="/" hrefLang="en" lang="en" dir="ltr" aria-label="Switch to English">English</a>
          <a href="#contact" className="header-contact">ابدأ حوارًا معنا <span aria-hidden="true">↗</span></a>
        </div>
        <details className="mobile-nav">
          <summary aria-label="فتح قائمة التنقل">القائمة <span aria-hidden="true">＋</span></summary>
          <nav aria-label="القائمة الرئيسية">
            <a href="#thinking">فكرتنا</a>
            <a href="#spaces">المساحات</a>
            <a href="#digital">التجربة الرقمية</a>
            <a href="#approach">رحلتنا</a>
            <a href="#contact">تواصل</a>
            <a href="/" hrefLang="en" lang="en" dir="ltr">English</a>
          </nav>
        </details>
      </header>

      <main id="arabic-main">
        <section className="arabic-hero">
          <div className="arabic-hero-copy">
            <p className="eyebrow"><span className="eyebrow-rule" /> الطبيعة / العمارة / الإنسان</p>
            <h1>مساحات<br />تستمرّ <em>في الحياة.</em></h1>
            <p>نقرّب العالم الطبيعي من المساحات اللي بنعيش ونشتغل فيها، ونفكر في الخضرة باعتبارها جزءًا من تجربة المكان، مش مجرد إضافة في آخر التصميم.</p>
            <a className="text-cta" href="#thinking">اكتشف فكرتنا <span className="cta-circle" aria-hidden="true">↙</span></a>
            <div className="hero-bottom"><span>مساحات تُصمَّم للحياة</span><span>اكتشف المزيد ↓</span></div>
          </div>
          <div className="arabic-art-panel">
            <Leaf />
            <span className="arabic-art-label" lang="en" dir="ltr">AN ORIGINAL BOTANICAL STUDY / 001</span>
          </div>
        </section>

        <section className="arabic-statement" id="thinking">
          <p className="section-index">٠١ / كيف نفكر في المساحات الخضراء</p>
          <div className="arabic-statement-grid">
            <h2>مش بس منظر<br /><em>جميل.</em></h2>
            <div>
              <p className="statement-lead">دي علاقة بتتكوّن مع الوقت.</p>
              <p>كل نبات يعيش في مكان حقيقي، ويتأثر بالضوء والعناية والناس. علشان كده بنبدأ بالمساحة نفسها، وبنكمل التفكير فيها بعد التركيب.</p>
            </div>
          </div>
        </section>

        <section className="arabic-spaces" id="spaces">
          <p className="section-index">٠٢ / أين تصير الطبيعة جزءًا من المكان؟</p>
          <div className="arabic-section-heading">
            <h2>أماكن مختلفة.<br /><em>فكرة واحدة.</em></h2>
            <p>مش بنعرض هنا صور مشاريع منفّذة؛ دي مساحات ممكن نصمم لها حلولًا خضراء، وتحتاج كل واحدة منها فهمًا مختلفًا.</p>
          </div>
          <div className="arabic-space-grid">
            {spaces.map((space, index) => (
              <article key={space.id} className={"arabic-space-tile arabic-space-" + index}>
                <span className="space-num">{space.id} / نوع المساحة</span>
                <div className="arabic-abstract-space" aria-hidden="true"><i /><b /><span /></div>
                <h3>{space.label}</h3>
                <p>{space.text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="arabic-continuity" id="digital">
          <div>
            <p className="section-index light">٠٣ / الحياة بعد التركيب</p>
            <h2>التركيب<br /><em>مش آخر خطوة.</em></h2>
            <p>المساحات الحيّة بتتغير. النبات يستقر، والناس تلاحظ، والعناية تسيب أثرًا. بنطوّر تجربة رقمية تربط المكان بالنبات بالملاحظات والوقت.</p>
            <div className="continuity-steps"><span>المكان</span><i /><span>النبات</span><i /><span>العناية</span><i /><span>الوقت</span></div>
            <p className="development-note">تطبيق العميل قيد التطوير والمراجعة. مفيش بيانات حقيقية لمستخدمين معروضة على الصفحة دي.</p>
          </div>
          <div className="arabic-digital-art" aria-hidden="true">
            <div className="arabic-phone-card">
              <span className="phone-eyebrow">حكاية نبات</span>
              <h3>لكل نبات<br />مساحة وذاكرة.</h3>
              <div className="arabic-phone-plant"><Leaf /></div>
              <p>مكانه</p><p>ملاحظاته</p><p>ما تغيّر مع الوقت</p>
            </div>
            <span className="phone-stage-caption">تصور توضيحي، مش شاشة فعلية من التطبيق</span>
          </div>
        </section>

        <section className="arabic-process" id="approach">
          <p className="section-index">٠٤ / من الفكرة إلى الاستمرار</p>
          <h2>نفكر في المكان<br /><em>ككائن حي.</em></h2>
          <div className="arabic-process-list">
            {stages.map((stage) => (
              <div className="arabic-process-row" key={stage.id}>
                <span>{stage.id}</span><h3>{stage.title}</h3><p>{stage.text}</p><span aria-hidden="true">↗</span>
              </div>
            ))}
          </div>
        </section>

        <section className="arabic-contact" id="contact">
          <p className="section-index light">٠٥ / نبدأ من المكان</p>
          <h2>عندك مساحة<br /><em>بتفكر فيها؟</em></h2>
          <p>كل مساحة تبدأ بسؤال مختلف. نتكلم عن المكان وإمكانياته قبل ما نقرر شكله.</p>
          <a href="https://plantupia.com/" target="_blank" rel="noopener noreferrer" className="closing-link">تواصل عبر موقع بلانتيبيا الحالي <span aria-hidden="true">↗</span></a>
          <p className="closing-note">دي نسخة تصميم للمراجعة، ولسه بيانات التواصل الرسمية وصور المشاريع محتاجة اعتماد الشركة.</p>
        </section>
      </main>

      <footer className="site-footer">
        <PlantMark />
        <span>الطبيعة · العمارة · الإنسان</span>
        <span>٢٠٢٦ © بلانتيبيا / نسخة للمراجعة</span>
      </footer>
    </div>
  );
}
