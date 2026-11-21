import type { Metadata } from "next";
import { notFound } from "next/navigation";
import WorldStudy from "../../WorldStudy";

export const dynamic="force-dynamic";
export const metadata:Metadata={
  title:"Living Spatial Archive | Plantupia",
  description:"Private Plantupia world-system study.",
  robots:{index:false,follow:false},
};

export default function WorldStudyPage(){
  const allowed=process.env.VERCEL_ENV==="preview"||(process.env.NODE_ENV==="development"&&!!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR);
  if(!allowed) notFound();
  return <WorldStudy/>;
}
