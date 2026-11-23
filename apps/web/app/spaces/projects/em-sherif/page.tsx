import { isProductionSite } from "../../../publication";
import type {Metadata} from "next";import EMSherifCaseStudy from "../../../EMSherifCaseStudy";
export const dynamic="force-dynamic";export const metadata:Metadata={title:"EM Sherif Sea Cafe | Plantupia",description:"Seven planting options for a custom sea-side planter.",robots:isProductionSite?{index:true,follow:true}:{index:false,follow:false}};
export default function Page(){return <EMSherifCaseStudy/>}
