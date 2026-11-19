import type { Metadata } from "next";
import { notFound } from "next/navigation";
import MazalCaseStudy from "../../../MazalCaseStudy";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Mazal / Facade Study | Plantupia",description:"Nine planting directions for the Mazal Restaurant facade.",robots:process.env.VERCEL_ENV==="production"?{index:true,follow:true}:{index:false,follow:false}};
export default function MazalPage(){
 if(process.env.VERCEL_ENV!=="production"&&(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))notFound();
 return <MazalCaseStudy/>;
}
