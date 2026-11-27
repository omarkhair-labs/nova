"use client";

import { useId, useState } from "react";
import "./living-place-study.css";

// A deliberately bounded public geometry exercise. Unlike the authenticated
// Android scenario, there is no location, date, measured room or solar ephemeris.
const room = { depth: 5, width: 3.6, sill: .8, head: 2.2, plane: .4, aperture: 1.6 };
const positions = [
  { label: "Near the window", x: .9, y: 1.8 },
  { label: "Middle of the room", x: 2.2, y: 1.8 },
  { label: "Deeper in the room", x: 4.2, y: 1.8 },
] as const;

export function perpendicularFootprint(elevation: number) {
  const slope = Math.tan(elevation * Math.PI / 180);
  const start = (room.sill - room.plane) / slope;
  const end = (room.head - room.plane) / slope;
  return { start, end, clippedStart: Math.min(room.depth, Math.max(0, start)),
    clippedEnd: Math.min(room.depth, Math.max(0, end)) };
}

export default function LivingPlaceStudy() {
  const id = useId();
  const [elevation, setElevation] = useState(30);
  const [position, setPosition] = useState(1);
  const specimen = positions[position];
  const light = perpendicularFootprint(elevation);
  const inside = specimen.x >= light.start && specimen.x <= light.end &&
    Math.abs(specimen.y - room.width / 2) <= room.aperture / 2;
  const ox = 76, oy = 49, scale = 79, wall = ox + room.depth * scale;
  const planY = (y:number) => oy + y * scale;
  const px = (x:number) => ox + x * scale;
  const openingTop = planY((room.width - room.aperture) / 2);
  const openingBottom = openingTop + room.aperture * scale;
  const floorY = 205, sectionScale = 79;
  const sz = (z:number) => floorY - z * sectionScale;
  const planeY = sz(room.plane);
  return <section className="lp-study" aria-labelledby={id+"-title"}>
    <div className="lp-intro">
      <div><p className="pc-kicker">LIVING PLACE / AN INTERACTIVE STUDY</p>
        <h2 id={id+"-title"}>Light has a place.</h2></div>
      <p>Explore how a window and the angle of direct sunlight change the part of an illustrative room that a plant occupies.</p>
    </div>
    <div className="lp-workspace">
      <div className="lp-drawings">
        <figure className="lp-sheet">
          <figcaption><span>01 / PLAN</span><span>5.0 × 3.6 m · illustrative</span></figcaption>
          <svg viewBox="0 0 530 385" role="img" aria-label={"Example floor plan. "+specimen.label+". "+(inside?"Inside":"Outside")+" the illustrated direct-ray area."}>
            <defs>
              <pattern id={id+"-grid"} width="19.75" height="19.75" patternUnits="userSpaceOnUse"><path d="M19.75 0H0V19.75" fill="none" stroke="#c9cbbf" strokeWidth=".6"/></pattern>
              <clipPath id={id+"-room"}><rect x={ox} y={oy} width={room.depth*scale} height={room.width*scale}/></clipPath>
            </defs>
            <rect x={ox} y={oy} width={room.depth*scale} height={room.width*scale} fill={"url(#"+id+"-grid)"}/>
            <g clipPath={"url(#"+id+"-room)"}>
              {light.clippedEnd>light.clippedStart&&<rect x={px(light.clippedStart)} y={openingTop} width={(light.clippedEnd-light.clippedStart)*scale} height={room.aperture*scale} fill="#ebc972" fillOpacity=".63" stroke="#ad7935" strokeWidth="1" strokeDasharray="4 3"/>}
              <path d={"M"+ox+" "+planY(room.width/2)+"H"+wall} fill="none" stroke="#8fa496" strokeDasharray="3 5"/>
            </g>
            <path d={"M"+ox+" "+openingTop+"V"+oy+"H"+wall+"V"+planY(room.width)+"H"+ox+"V"+openingBottom} fill="none" stroke="#224535" strokeWidth="4"/>
            <path d={"M"+ox+" "+openingTop+"V"+openingBottom} fill="none" stroke="#477c80" strokeWidth="6"/>
            <g stroke="#637e6c" strokeWidth="1" fill="none">
              <path d={"M"+ox+" 29H"+wall+"M"+ox+" 23V35M"+wall+" 23V35"}/>
              <path d={"M"+(wall+22)+" "+oy+"V"+planY(room.width)+"M"+(wall+16)+" "+oy+"h12M"+(wall+16)+" "+planY(room.width)+"h12"}/>
            </g>
            <g fontSize="13" fill="#3b5a49"><text x={(ox+wall)/2} y="20" textAnchor="middle">5 m</text><text x={wall+39} y={planY(room.width/2)} transform={"rotate(90 "+(wall+39)+" "+planY(room.width/2)+")"} textAnchor="middle">3.6 m</text><text x="14" y={openingTop-10}>Window</text></g>
            {positions.map((p,i)=><g key={p.label} transform={"translate("+px(p.x)+" "+planY(p.y)+")"}>
              <circle r={i===position?15:8} fill={i===position?"#234d39":"#f7f7ef"} stroke="#234d39" strokeWidth="1.7"/>
              {i===position?<><path d="M0 8V-6M0 0Q-12 0-9-10Q0-10 0 0M0-2Q11-4 8-12Q0-10 0-2" fill="#c4d6a9" stroke="#c4d6a9" strokeWidth="1.5"/><circle r="22" fill="none" stroke="#234d39" strokeDasharray="3 4"/></>:<circle r="2.5" fill="#688c68"/>}
              <text y="42" fontSize="12" textAnchor="middle" fill="#385543">{i+1}</text>
            </g>)}
            <text x={ox} y="362" fontSize="12" fill="#50685a">Dashed gold area: direct rays at 0.4 m above floor</text>
          </svg>
        </figure>
        <figure className="lp-sheet lp-section">
          <figcaption><span>02 / SECTION THROUGH WINDOW</span><span>Perpendicular rays</span></figcaption>
          <svg viewBox="0 0 530 235" role="img" aria-label={"Side section showing the "+elevation+" degree ray angle and the selected position."}>
            <defs><clipPath id={id+"-section"}><rect x={ox} y={sz(2.5)} width={room.depth*scale} height={2.5*sectionScale}/></clipPath></defs>
            <path d={"M"+ox+" "+sz(room.head)+"V"+sz(2.5)+"H"+wall+"V"+floorY+"H"+ox+"V"+sz(room.sill)} fill="#f2f3e9" stroke="#254936" strokeWidth="3"/>
            <path d={"M"+ox+" "+sz(room.head)+"V"+sz(room.sill)} stroke="#477c80" strokeWidth="5"/>
            <g clipPath={"url(#"+id+"-section)"}>
              <path d={"M"+ox+" "+sz(room.head)+"L"+px(light.end)+" "+planeY+"L"+px(light.start)+" "+planeY+"L"+ox+" "+sz(room.sill)+"Z"} fill="#ebc972" fillOpacity=".62"/>
              <path d={"M"+ox+" "+sz(room.head)+"L"+px(light.end)+" "+planeY+"M"+ox+" "+sz(room.sill)+"L"+px(light.start)+" "+planeY} stroke="#ac7736" strokeWidth="1.5"/>
              <path d={"M"+ox+" "+planeY+"H"+wall} stroke="#587964" strokeDasharray="3 4"/>
            </g>
            <g transform={"translate("+px(specimen.x)+" "+planeY+")"}>
              <circle r="6" fill="#234d39"/><path d="M0 0V-29M0-12Q-18-12-14-28Q0-28 0-12M0-18Q17-20 12-34Q0-30 0-18" fill="#a1b99a" stroke="#234d39" strokeWidth="1.5"/>
              <path d="M-9 3H9L6 18H-6Z" fill="#8a6c54" stroke="#674b38"/>
            </g>
            <text x="12" y={sz(room.head)+5} fontSize="12" fill="#385543">2.2 m</text><text x="12" y={sz(room.sill)+5} fontSize="12" fill="#385543">0.8 m</text><text x={wall} y="224" textAnchor="end" fontSize="12" fill="#385543">Study plane 0.4 m</text>
          </svg>
        </figure>
      </div>
      <div className="lp-controls">
        <p className="pc-kicker">01 / CHANGE THE ANGLE</p>
        <div className="lp-angle"><output htmlFor={id+"-angle"}>{elevation}°</output><span>Sun elevation</span></div>
        <label className="lp-range-label" htmlFor={id+"-angle"}>Angle above the horizon</label>
        <input id={id+"-angle"} type="range" min="10" max="75" step="1" value={elevation} onChange={e=>setElevation(Number(e.target.value))}/>
        <div className="lp-range-ends"><span>10°</span><span>75°</span></div>
        <fieldset><legend>02 / CHOOSE A POSITION</legend>
          {positions.map((p,i)=><button type="button" key={p.label} aria-pressed={position===i} onClick={()=>setPosition(i)}><span>{String(i+1).padStart(2,"0")}</span>{p.label}<span aria-hidden="true">{position===i?"●":"○"}</span></button>)}
        </fieldset>
        <div className="lp-result" aria-live="polite">
          <span className="lp-result-label">ILLUSTRATIVE RESULT</span>
          <h3>{inside?"Direct rays reach this point.":"Direct rays miss this point."}</h3>
          <p>{inside?"The point intersects rays passing through the example window at this elevation.":"At this angle the point lies outside the direct-ray footprint. It may still receive indirect light."}</p>
        </div>
        <p className="lp-fine">This is a geometric demonstration, not a lighting recommendation or an actual room assessment. It assumes perpendicular sunlight, an unobstructed window and a flat plane. It does not estimate lux, exposure, plant health or a time of day.</p>
      </div>
    </div>
    <div className="lp-close"><p>From a place to a living record: the product direction connects reported room geometry with individual plants and recorded care, subject to authorized field validation.</p><a href="/start-a-space?topic=care">Discuss continuing care ↗</a></div>
  </section>
}
