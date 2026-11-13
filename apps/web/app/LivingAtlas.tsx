"use client";

import { useEffect, useState } from "react";

type Lang = "en" | "ar";
type Lens = "place" | "plant" | "time";
type PlaceId = "dwelling" | "work" | "gather";
type Bi = { en: string; ar: string };
type Note = { heading: Bi; detail: Bi; tag: Bi };
type Chapter = { number: string; title: Bi; description: Bi };

const pair = (en: string, ar: string): Bi => ({ en, ar });
const spaces: { id: PlaceId; number: string; title: Bi; subtitle: Bi; cue: Bi }[] = [
  {
    id: "dwelling", number: "01",
    title: pair("A place to live", "مساحة للمعيشة"),
    subtitle: pair("A quieter relationship with the everyday.", "علاقة أهدأ مع تفاصيل اليوم."),
    cue: pair("LIGHT / THRESHOLD / ROUTINE", "ضوء / عتبات / إيقاع"),
  },
  {
    id: "work", number: "02",
    title: pair("A place to think", "مساحة للعمل"),
    subtitle: pair("Nature belongs beside the work, not on top of it.", "الطبيعة جزء من طريقة استخدام المكان."),
    cue: pair("RHYTHM / SIGHTLINE / HUMAN", "إيقاع / رؤية / إنسان"),
  },
  {
    id: "gather", number: "03",
    title: pair("A place to gather", "مساحة للقاء"),
    subtitle: pair("A shared atmosphere that stays with people.", "جوّ مشترك له حضور في ذاكرة الناس."),
    cue: pair("ARRIVAL / PAUSE / SHARED LIFE", "وصول / توقف / حياة مشتركة"),
  },
];
const lensNames: Record<Lens, Bi> = {
  place: pair("01 / SPACE", "٠١ / المكان"),
  plant: pair("02 / LIVING", "٠٢ / النبات"),
  time: pair("03 / TIME", "٠٣ / الزمن"),
};
const lensSubjects: Record<Lens, Bi> = {
  place: pair("Read the space", "اقرأ المكان"),
  plant: pair("Notice what's alive", "لاحظ ما هو حي"),
  time: pair("Follow what changes", "تابع ما يتغير"),
};
const lensNotes: Record<Lens, Note[]> = {
  place: [
    {
      heading: pair("Light has a direction.", "للضوء اتجاه."),
      detail: pair("A space has natural conditions before a single plant is introduced. Observe light, edges and how people move.", "لكل مكان ظروفه قبل دخول أي نبات. الضوء وحدود المكان وحركة الناس هي البداية."),
      tag: pair("01 / THE SETTING", "٠١ / السياق"),
    },
    {
      heading: pair("An edge can become a habitat.", "الحدود قد تصبح موطنًا."),
      detail: pair("Windows, corners and transitions shape how something living can belong to architecture.", "النوافذ والزوايا والانتقالات تحدد أين يمكن أن تعيش الطبيعة داخل العمارة."),
      tag: pair("02 / THE THRESHOLD", "٠٢ / العتبة"),
    },
    {
      heading: pair("People complete the room.", "الناس يكملون المكان."),
      detail: pair("A thoughtful green space considers where people pause, work, walk and return.", "المساحة الخضراء المفكّر فيها تراعي أين يمشي الناس ويعملون ويتوقفون ويعودون."),
      tag: pair("03 / THE HUMAN", "٠٣ / الإنسان"),
    },
  ],
  plant: [
    {
      heading: pair("Not a decorative object.", "ليس مجرد عنصر ديكور."),
      detail: pair("A living plant has placement, needs and a real context. The scene illustrates a relationship, not a recommended species.", "النبات الحيّ له مكان واحتياجات وسياق حقيقي. المشهد توضيح للفكرة، وليس توصية بنوع نبات."),
      tag: pair("01 / THE LIVING", "٠١ / الكائن الحي"),
    },
    {
      heading: pair("Place changes the story.", "المكان يغيّر الحكاية."),
      detail: pair("The same design gesture cannot be copied blindly from room to room. Care starts with understanding the conditions.", "لا ينفع ننقل نفس الاختيار من مكان للتاني بدون فهم. العناية تبدأ بفهم الظروف."),
      tag: pair("02 / THE CONTEXT", "٠٢ / السياق"),
    },
    {
      heading: pair("A relationship, not a prop.", "علاقة، مش عنصر ثابت."),
      detail: pair("Plants are experienced through proximity, routine and observation. We design for that ongoing contact.", "نختبر وجود النبات بالقرب والعادة والملاحظة. نصمّم لهذه العلاقة المستمرة."),
      tag: pair("03 / THE ENCOUNTER", "٠٣ / اللقاء"),
    },
  ],
  time: [
    {
      heading: pair("Arrival is only the beginning.", "التركيب هو البداية."),
      detail: pair("A living place is never finished by a final photograph. This is a conceptual stage, not a claim of installation.", "المكان الحي لا تكتمل حكايته بصورة نهائية. دي مرحلة تصوّرية، مش ادعاء بتركيب مشروع."),
      tag: pair("01 / ARRIVAL", "٠١ / البداية"),
    },
    {
      heading: pair("Observation is a kind of care.", "الملاحظة شكل من العناية."),
      detail: pair("Notice what actually happened. Do not confuse a calendar date with proof that anything was done.", "نسجل اللي حصل فعلًا. ميعاد في التقويم مش دليل إن رعاية تمت."),
      tag: pair("02 / ATTENTION", "٠٢ / الملاحظة"),
    },
    {
      heading: pair("Evidence becomes memory.", "الدليل يبقى ذاكرة."),
      detail: pair("Plantupia's digital direction connects a place, its plants and human-recorded moments over time — never fabricated sensor readings.", "الاتجاه الرقمي في بلانتيبيا بيربط المكان بالنبات وباللحظات اللي سجلها الناس، من غير اختلاق قراءات حساسات."),
      tag: pair("03 / CONTINUITY", "٠٣ / الاستمرار"),
    },
  ],
};
const chapters: Chapter[] = [
  {
    number: "01", title: pair("Place", "المكان"),
    description: pair("Start with the real environment: architecture, daylight, paths and the people moving through it.", "نبدأ بالمكان الحقيقي: العمارة والضوء والممرات والناس اللي بتستخدمه."),
  },
  {
    number: "02", title: pair("Living material", "الكائن الحي"),
    description: pair("Plants are living participants, not decoration pasted on a finished space.", "النباتات كائنات حية في المكان، مش ديكور بيتضاف بعد انتهاء التصميم."),
  },
  {
    number: "03", title: pair("Human care", "العناية"),
    description: pair("What someone noticed, chose and actually did matters more than an automatic green status.", "اللي الإنسان لاحظه واختاره وعمله فعلًا أهم من حالة خضراء أوتوماتيكية."),
  },
  {
    number: "04", title: pair("Living time", "الزمن الحي"),
    description: pair("The relationship continues through dated observations, records and thoughtful return.", "العلاقة بتكمل من خلال ملاحظات موثقة وسجلات والعودة للمكان مع الوقت."),
  },
];
const storyTimes = [
  pair("At the beginning", "عند البداية"),
  pair("When we return", "وقت العودة"),
  pair("Across time", "على امتداد الزمن"),
] as const;
const at = (t: Bi, lang: Lang) => t[lang];

function Brand() {
  return (
    <span className="atlas-brand" dir="ltr" lang="en">
      <svg viewBox="0 0 32 32" width="30" height="30" fill="none" aria-hidden="true">
        <circle cx="16" cy="16" r="14.5" stroke="currentColor"/>
        <path d="M16 25V9M16 18C9 18 7 15 7 8c7 0 9 4 9 10ZM16 17c0-7 3-10 10-10 0 7-3 10-10 10Z" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      </svg>
      <span>plantupia<span className="atlas-brand-dot">.</span></span>
    </span>
  );
}

function BotanicalCluster({ x, y, scale = 1, turn = 1, muted = false }: {
  x: number; y: number; scale?: number; turn?: number; muted?: boolean;
}) {
  return (
    <g transform={"translate(" + x + " " + y + ") scale(" + scale + ")"} className={muted ? "atlas-leaves-muted" : "atlas-leaves"}>
      <path d="M0 0C-4-56 6-105-2-164M0-42c-40-24-60-51-69-85M-1-67c40-28 55-55 60-90M-1-116c-20-33-24-52-20-77" stroke="#244e3c" strokeWidth="3" strokeLinecap="round" fill="none"/>
      <path d="M-69-127C-97-149-115-181-110-208c43 12 63 42 41 81Z" fill="#97b28e" stroke="#244e3c" strokeWidth="2"/>
      <path d="M59-157c7-40 31-58 64-58-4 44-26 65-64 58Z" fill="#b8cda7" stroke="#244e3c" strokeWidth="2"/>
      <path d="M-22-191c-23-28-19-68 0-91 25 25 22 64 0 91Z" fill="#7f9c7c" stroke="#244e3c" strokeWidth="2"/>
      <path d="M-1-87C-48-86-72-108-82-141c45-4 70 15 81 54Z" fill="#c3cfa8" stroke="#244e3c" strokeWidth="2"/>
      <path d="M0-44c39-22 58-52 68-81 16 52-14 82-68 81Z" fill="#8ea98a" stroke="#244e3c" strokeWidth="2"/>
      <path d="M-31 0H31l9 15H-41l10-15Z" fill={turn > 1 ? "#baa58b" : "#a98c73"} stroke="#314735" strokeWidth="2"/>
      <path d="M-34-3H34" stroke="#314735" strokeWidth="3" strokeLinecap="round"/>
    </g>
  );
}

function SpatialStudy({ place, lens, time, lang }: {
  place: PlaceId; lens: Lens; time: number; lang: Lang;
}) {
  const phase = time === 0 ? 0.87 : time === 1 ? 0.96 : 1;
  const isWork = place === "work";
  const isGather = place === "gather";
  return (
    <svg className={"atlas-spatial-svg lens-" + lens} viewBox="0 0 1000 680" role="img"
      aria-label={lang === "ar" ? "رسم معماري تصوري لمساحة بها نباتات قابلة للاستكشاف" : "Original conceptual architectural study of a space with living plants"}>
      <defs>
        <linearGradient id="atlas-wall" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor={isWork ? "#d1dcd3" : isGather ? "#dfc8a8" : "#e3d8c6"}/>
          <stop offset="1" stopColor={isWork ? "#aec1b9" : isGather ? "#cfb498" : "#c9c2ae"}/>
        </linearGradient>
        <linearGradient id="atlas-floor" x1="0" x2="0.9" y1="0" y2="1">
          <stop offset="0" stopColor={isWork ? "#acb8aa" : isGather ? "#a9a092" : "#bcb29f"}/>
          <stop offset="1" stopColor="#657b68"/>
        </linearGradient>
        <linearGradient id="atlas-windowlight" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#f0f0d4" stopOpacity=".98"/>
          <stop offset="1" stopColor="#b3d0ad" stopOpacity=".48"/>
        </linearGradient>
        <filter id="atlas-blur"><feGaussianBlur stdDeviation="19"/></filter>
        <pattern id="atlas-floor-grid" width="48" height="48" patternUnits="userSpaceOnUse">
          <path d="M48 0H0V48" fill="none" stroke="#f2e9ce" strokeOpacity=".14" strokeWidth="1"/>
        </pattern>
      </defs>
      <rect width="1000" height="680" fill="#243c32"/>
      <path d="M55 80 695 28 947 146 945 487 330 560 48 441Z" fill="url(#atlas-wall)"/>
      <path d="M55 80 330 180 330 560 48 441Z" fill="#607b6e" fillOpacity=".36"/>
      <path d="M695 28 947 146 945 487 689 402Z" fill="#9caa97" fillOpacity=".27"/>
      <path d="M330 560 945 487 986 607 292 667 48 532 48 441Z" fill="url(#atlas-floor)"/>
      <path d="M330 560 945 487 986 607 292 667 48 532 48 441Z" fill="url(#atlas-floor-grid)"/>
      <path d="M48 441 330 560 945 487" stroke="#f7efda" strokeOpacity=".7" strokeWidth="2" fill="none"/>
      <path d="M55 80 695 28 947 146" stroke="#f8f1df" strokeOpacity=".5" strokeWidth="3" fill="none"/>
      <g className="atlas-architecture">
        <path d={isWork ? "M408 88 649 72 649 342 408 370Z" : isGather ? "M425 89 665 74 665 339 425 370Z" : "M424 93 651 79 651 345 424 373Z"}
          fill="#5d8376" stroke="#f3eddc" strokeWidth="7"/>
        <path d="M445 118 626 105 626 323 445 347Z" fill="url(#atlas-windowlight)" />
        <path d="M445 118 626 105 626 323 445 347Z" fill="#a4d0c9" fillOpacity=".16"/>
        <path d="M536 113 536 336M445 226 626 213" stroke="#789588" strokeWidth="5"/>
        <path d="M444 347 747 508 878 535 625 333" fill="#f3f1d5" fillOpacity=".13"/>
        <path d="M445 118 625 106 822 502 647 521" fill="#e8e4b8" opacity=".13" filter="url(#atlas-blur)"/>
        <path d="M730 84 735 411" stroke="#f1e8d7" strokeOpacity=".3" strokeWidth="2"/>
      </g>
      {place === "dwelling" && (
        <g className="atlas-furniture">
          <path d="M485 427 704 409 745 470 519 493Z" fill="#ddc8aa" stroke="#445d50" strokeWidth="4"/>
          <path d="M507 491 505 544M731 472 743 516" stroke="#2f5142" strokeWidth="11"/>
          <path d="M504 430 511 384 620 377 613 429" fill="#d4bb9f" stroke="#536457" strokeWidth="3"/>
          <rect x="366" y="456" width="125" height="29" rx="8" fill="#9b745c"/>
          <path d="M373 485 367 528M477 485 491 521" stroke="#5e5342" strokeWidth="7"/>
          <path d="M555 423 603 420 618 434 573 440Z" fill="#f0e5cf"/>
        </g>
      )}
      {isWork && (
        <g className="atlas-furniture">
          <path d="M395 418 740 395 824 461 453 499Z" fill="#a77d5b" stroke="#4a554a" strokeWidth="4"/>
          <path d="M461 500 465 568M816 462 827 518" stroke="#52695b" strokeWidth="10"/>
          <path d="M566 422 646 416 646 386 565 393Z" fill="#354e4b" stroke="#d8d5bb" strokeWidth="4"/>
          <path d="M603 419 605 435" stroke="#486457" strokeWidth="7"/>
          <path d="M467 512 512 500 546 535 500 548Z" fill="#5b7566"/>
          <path d="M513 541 520 581" stroke="#274334" strokeWidth="6"/>
        </g>
      )}
      {isGather && (
        <g className="atlas-furniture">
          <path d="M450 429 701 405 750 440 494 466Z" fill="#745b46" stroke="#3e5347" strokeWidth="4"/>
          <path d="M502 468 502 531M741 441 758 497" stroke="#435647" strokeWidth="10"/>
          <path d="M536 437 610 431 617 443 546 451Z" fill="#cfab84"/>
          <path d="M366 446 415 435 444 481 398 493Z" fill="#b28a6e" stroke="#45574d" strokeWidth="4"/>
          <path d="M399 493 396 543" stroke="#48594a" strokeWidth="7"/>
          <path d="M758 426 801 411 837 455 793 470Z" fill="#c09d7b" stroke="#45574d" strokeWidth="4"/>
          <path d="M807 469 808 516" stroke="#48594a" strokeWidth="7"/>
        </g>
      )}
      <g className="atlas-vegetation" opacity={lens === "place" ? .87 : 1}>
        <BotanicalCluster x={340} y={499} scale={isWork ? 0.91 * phase : 1.05 * phase} />
        <BotanicalCluster x={833} y={469} scale={(isGather ? .8 : .65) * phase}/>
        {lens !== "place" && <BotanicalCluster x={680} y={406} scale={.39 * phase} muted/>}
      </g>
      {lens === "place" && (
        <g className="atlas-place-guides">
          <path d="M445 118 626 105M330 560 945 487" stroke="#ebf2bd" strokeWidth="1.8" strokeDasharray="8 8"/>
          <path d="M445 120 338 493M626 105 837 465" stroke="#f8efcc" strokeOpacity=".5" strokeWidth="1.2" strokeDasharray="6 9"/>
        </g>
      )}
      {lens === "time" && (
        <g className="atlas-time-guides">
          <circle cx="346" cy="345" r={time === 0 ? 88 : time === 1 ? 110 : 130} stroke="#d8edb4" strokeWidth="1.6" strokeDasharray="6 9" fill="#d2ebb2" fillOpacity=".07"/>
          <circle cx="829" cy="361" r={time === 0 ? 47 : time === 1 ? 63 : 73} stroke="#d8edb4" strokeWidth="1.4" fill="none" strokeDasharray="4 8"/>
          <path d="M370 465Q485 355 676 410T849 445" fill="none" stroke="#d8edb4" strokeWidth="1.5" strokeDasharray="5 8"/>
        </g>
      )}
      {lens === "plant" && (
        <g className="atlas-life-guides">
          <circle cx="344" cy="340" r="120" fill="#d2edaa" fillOpacity=".07" stroke="#ecf2c8" strokeDasharray="3 8"/>
          <circle cx="822" cy="356" r="74" fill="#d2edaa" fillOpacity=".06" stroke="#ecf2c8" strokeDasharray="3 8"/>
        </g>
      )}
      <path d="M55 80 48 441 48 532M947 146 945 487" stroke="#092c25" strokeOpacity=".3" strokeWidth="2" fill="none"/>
      <text x="55" y="53" fill="#d5e7cf" fontSize="13" letterSpacing="4" fontFamily="monospace">PLANTUPIA / SPATIAL STUDY</text>
      <text x="811" y="650" fill="#e6e9d4" fontSize="11" letterSpacing="2" fontFamily="monospace">NOT TO SCALE</text>
    </svg>
  );
}

const pointPositions: [number, number][] = [[48, 31], [34, 61], [78, 66]];

export default function LivingAtlas({ lang }: { lang: Lang }) {
  const arabic = lang === "ar";
  const [spaceId, setSpaceId] = useState<PlaceId>("dwelling");
  const [lens, setLens] = useState<Lens>("place");
  const [point, setPoint] = useState(0);
  const [time, setTime] = useState(0);
  const [chapter, setChapter] = useState(0);
  const [focus, setFocus] = useState(false);
  const selectedSpace = spaces.find((space) => space.id === spaceId) || spaces[0];
  const selectedNote = lensNotes[lens][point];

  useEffect(() => {
    // Keep the document language and reading direction honest after hydration.
    document.documentElement.lang = lang;
    document.documentElement.dir = arabic ? "rtl" : "ltr";
  }, [lang, arabic]);

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape" && focus) setFocus(false);
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [focus]);

  const selectSpace = (next: PlaceId) => { setSpaceId(next); setPoint(0); };
  const selectLens = (next: Lens) => { setLens(next); setPoint(0); };
  const switchHref = arabic ? "/" : "/ar/";
  const lensOrder: Lens[] = ["place", "plant", "time"];

  return (
    <div className={"atlas-site" + (focus ? " atlas-focused" : "")} dir={arabic ? "rtl" : "ltr"} lang={lang}>
      <a className="atlas-skip" href="#atlas-main">{arabic ? "انتقل للمحتوى" : "Skip to the experience"}</a>
      <header className="atlas-topbar">
        <a href={arabic ? "/ar/" : "/"} aria-label={arabic ? "بلانتيبيا - الرئيسية" : "Plantupia home"}><Brand /></a>
        <span className="atlas-topline">{arabic ? "دراسة عن الأماكن الحيّة / ٠١" : "A STUDY OF LIVING SPACES / 01"}</span>
        <div className="atlas-top-actions">
          <a className="atlas-lang" href={switchHref} hrefLang={arabic ? "en" : "ar"} lang={arabic ? "en" : "ar"}>{arabic ? "EN" : "عربي"}</a>
          <a className="atlas-index-link" href="#atlas-method">{arabic ? "الفكرة ↗" : "THE METHOD ↗"}</a>
        </div>
      </header>

      <main id="atlas-main">
        <div className="atlas-experience">
          <aside className="atlas-intro">
            <div className="atlas-intro-top">
              <span className="atlas-overline">{arabic ? "دليل لاستكشاف العلاقة" : "FIELD GUIDE / VOL. 001"}</span>
              <div className="atlas-intro-mark" aria-hidden="true"><span>✳</span> 01—03</div>
            </div>
            <div className="atlas-intro-center">
              <h1>{arabic ? <>المكان<br/>مش <em>ثابت.</em></> : <>A space<br/>is never <em>still.</em></>}</h1>
              <p className="atlas-intro-lead">{arabic ? "الطبيعة مش آخر لمسة في التصميم. شوف إزاي المكان والنبات والإنسان والزمن بيغيّروا بعض." : "Nature is not the finishing touch. Look closer at the relationship between a space, what lives in it, and the time it takes."}</p>
            </div>
            <div className="atlas-space-selector">
              <p className="atlas-selector-caption">{arabic ? "اختار مساحة تستكشفها" : "CHOOSE A SPACE TO EXPLORE"}</p>
              {spaces.map((space) => (
                <button type="button" key={space.id} className={"atlas-space-choice" + (spaceId === space.id ? " is-selected" : "")}
                  aria-pressed={spaceId === space.id} onClick={() => selectSpace(space.id)}>
                  <span className="atlas-choice-number">{space.number}</span>
                  <span>{at(space.title, lang)}</span>
                  <span className="atlas-choice-arrow" aria-hidden="true">↗</span>
                </button>
              ))}
            </div>
            <div className="atlas-intro-footnote">{arabic ? "مشهد تصوري أصلي، مش صورة مشروع منفّذ." : "ORIGINAL CONCEPT STUDY / NOT A BUILT PROJECT"}</div>
          </aside>

          <section className="atlas-workbench" aria-label={arabic ? "استكشف المساحة" : "Explore the spatial study"}>
            <div className="atlas-workbench-bar">
              <div className="atlas-workbench-id"><span className="atlas-live-dot" />{arabic ? "المشهد" : "CURRENT STUDY"} <b>{selectedSpace.number} / 03</b></div>
              <div className="atlas-lens-tabs" role="group" aria-label={arabic ? "منظور الاستكشاف" : "Choose an exploration lens"}>
                {lensOrder.map((value) => (
                  <button type="button" key={value} aria-pressed={lens === value}
                    className={lens === value ? "is-active" : ""} onClick={() => selectLens(value)}>
                    {at(lensNames[value], lang)}
                  </button>
                ))}
              </div>
              <button className="atlas-focus-toggle" type="button" aria-pressed={focus} onClick={() => setFocus(!focus)}
                aria-label={focus ? (arabic ? "أظهر الواجهة" : "Show interface") : (arabic ? "وسّع المشهد" : "Expand view")}>
                {focus ? "↙" : "⤢"}
              </button>
            </div>

            <div className="atlas-scene" data-space={spaceId} data-lens={lens}>
              <div className="atlas-scene-render" key={spaceId + "-" + lens}>
                <SpatialStudy place={spaceId} lens={lens} time={time} lang={lang}/>
              </div>
              <div className="atlas-corner-label">{at(selectedSpace.cue, lang)}</div>
              <div className="atlas-space-signature" aria-live="polite">
                <span>{arabic ? "المساحة الحالية" : "SELECTED SPACE"} / {selectedSpace.number}</span>
                <strong>{at(selectedSpace.title, lang)}</strong>
                <small>{at(selectedSpace.subtitle, lang)}</small>
              </div>
              <div className="atlas-hotspot-layer">
                {lensNotes[lens].map((item, i) => (
                  <button type="button" key={lens + "-" + i}
                    className={"atlas-hotspot" + (point === i ? " is-current" : "")}
                    style={{ left: pointPositions[i][0] + "%", top: pointPositions[i][1] + "%" }}
                    aria-label={at(item.heading, lang)}
                    aria-pressed={point === i}
                    onClick={() => setPoint(i)}>
                    <span>{String(i+1).padStart(2, "0")}</span>
                  </button>
                ))}
              </div>
              <div className="atlas-scene-indicator">{arabic ? "اضغط على نقطة للتعمّق" : "SELECT A MARKER TO REVEAL A RELATIONSHIP"}<span aria-hidden="true"> ↗</span></div>
            </div>

            <div className="atlas-reading">
              <div className="atlas-reading-icon" aria-hidden="true">↳</div>
              <div className="atlas-reading-copy" aria-live="polite" aria-atomic="true">
                <span className="atlas-reading-tag">{at(selectedNote.tag, lang)}</span>
                <h2>{at(selectedNote.heading, lang)}</h2>
                <p>{at(selectedNote.detail, lang)}</p>
              </div>
              <div className="atlas-reading-tools">
                <span>{arabic ? "ملاحظة" : "NOTE"} {String(point + 1).padStart(2, "0")} / 03</span>
                <div className="atlas-reading-nav">
                  <button type="button" aria-label={arabic ? "الملاحظة السابقة" : "Previous note"} onClick={() => setPoint((point+2)%3)}>←</button>
                  <button type="button" aria-label={arabic ? "الملاحظة التالية" : "Next note"} onClick={() => setPoint((point+1)%3)}>→</button>
                </div>
              </div>
            </div>

            {lens === "time" && (
              <div className="atlas-time-control">
                <span>{arabic ? "تابع علاقة المكان بالوقت" : "THE LIVING SEQUENCE"}</span>
                <div role="group" aria-label={arabic ? "مرحلة تصورية" : "Conceptual stage"}>
                  {storyTimes.map((name, i) => (
                    <button key={i} type="button" className={i === time ? "is-active" : ""}
                      aria-pressed={i === time} onClick={() => setTime(i)}>
                      <span>{String(i+1).padStart(2, "0")}</span>{at(name, lang)}
                    </button>
                  ))}
                </div>
                <span className="atlas-time-disclaimer">{arabic ? "تصوّر للمراحل — لا توجد قراءات أو بيانات فعلية هنا." : "CONCEPTUAL PHASES / NO SENSOR OR CLIENT DATA"}</span>
              </div>
            )}
          </section>
        </div>

        <section className="atlas-method" id="atlas-method" aria-labelledby="atlas-method-title">
          <div className="atlas-method-rail">
            <span>{arabic ? "٠٢ / ما الذي يجعل المساحة حيّة؟" : "02 / THE RELATIONSHIP"}</span>
            <span>{arabic ? "تابع الخيط ↙" : "FOLLOW THE THREAD ↘"}</span>
          </div>
          <div className="atlas-method-stage">
            <div className="atlas-method-intro">
              <span className="atlas-overline">{arabic ? "بلانتيبيا / فكرتنا" : "PLANTUPIA / THE IDEA"}</span>
              <h2 id="atlas-method-title">{arabic ? <>التركيب<br/>مش <em>النهاية.</em></> : <>The moment of<br/>installation is <em>not the end.</em></>}</h2>
              <p>{arabic ? "في قلب بلانتيبيا فكرة بسيطة: النبات يعيش في مكان حقيقي، ويتغير مع رعاية الناس، ويستحق أن نحتفظ بما حدث له بالفعل." : "At the heart of Plantupia is a simple idea: a real plant lives in a real place. Human care and honest records make the relationship visible over time."}</p>
              <div className="atlas-method-actions">
                <span>{arabic ? "اختار مرحلة" : "EXPLORE THE FOUR LAYERS"}</span>
                <div className="atlas-step-buttons" role="group" aria-label={arabic ? "مراحل العلاقة" : "Four living layers"}>
                  {chapters.map((item, i) => (
                    <button type="button" key={item.number} aria-pressed={chapter===i} className={chapter===i ? "is-selected":""}
                      onClick={() => setChapter(i)}>{item.number}</button>
                  ))}
                </div>
              </div>
            </div>
            <div className="atlas-method-focus" aria-live="polite">
              <div className="atlas-method-folio">FIELDNOTE / {chapters[chapter].number} <span>— {arabic ? "من أربعة" : "OF FOUR"}</span></div>
              <div className="atlas-method-graphic" data-chapter={chapter}>
                <span className="atlas-graphic-line atlas-graphic-line-one"/>
                <span className="atlas-graphic-line atlas-graphic-line-two"/>
                <div className="atlas-graphic-ring"><div className="atlas-graphic-core">{chapters[chapter].number}</div></div>
                <span className="atlas-graphic-dot atlas-graphic-dot-one"/>
                <span className="atlas-graphic-dot atlas-graphic-dot-two"/>
                <span className="atlas-graphic-dot atlas-graphic-dot-three"/>
              </div>
              <h3>{at(chapters[chapter].title, lang)}</h3>
              <p>{at(chapters[chapter].description, lang)}</p>
            </div>
          </div>
        </section>
      </main>
      <footer className="atlas-footer">
        <Brand />
        <span>{arabic ? "الطبيعة × العمارة × الإنسان × الزمن" : "NATURE × ARCHITECTURE × PEOPLE × TIME"}</span>
        <span>{arabic ? "دراسة رقمية / نسخة للمراجعة" : "A DIGITAL DESIGN STUDY / IN REVIEW"}</span>
      </footer>
    </div>
  );
}
