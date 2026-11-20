import type { Metadata } from "next";
import { notFound } from "next/navigation";
import VisualDirectionLab from "../../VisualDirectionLab";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Archive Instrument | Plantupia",robots:{index:false,follow:false}};
export default function VisualDirectionPage(){
 const allowed=process.env.VERCEL_ENV==="preview"||(process.env.NODE_ENV==="development"&&!!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR);
 if(!allowed)notFound();
 return <VisualDirectionLab/>;
}
