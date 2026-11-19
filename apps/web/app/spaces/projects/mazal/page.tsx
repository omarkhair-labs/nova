import type { Metadata } from "next";
import { notFound } from "next/navigation";
import MazalCaseStudy from "../../../MazalCaseStudy";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Mazal / Facade Study | Plantupia",description:"Nine planting directions for the Mazal Restaurant facade.",robots:{index:false,follow:false}};
export default function MazalPage(){
 if(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)notFound();
 return <MazalCaseStudy/>;
}
