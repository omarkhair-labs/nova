import { isProductionSite } from "../../../publication";
import type { Metadata } from "next";
import MazalCaseStudy from "../../../MazalCaseStudy";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Mazal / Facade Study | Plantupia",description:"Nine planting directions for the Mazal Restaurant facade.",robots:isProductionSite?{index:true,follow:true}:{index:false,follow:false}};
export default function MazalPage(){

 return <MazalCaseStudy/>;
}
