import { publicationApproved } from "../../../publication";
import type {Metadata} from "next";import{notFound}from"next/navigation";import EMSherifCaseStudy from "../../../EMSherifCaseStudy";
export const dynamic="force-dynamic";export const metadata:Metadata={title:"EM Sherif Sea Cafe | Plantupia",description:"Seven planting options for a custom sea-side planter.",robots:publicationApproved?{index:true,follow:true}:{index:false,follow:false}};
export default function Page(){if(process.env.VERCEL_ENV!=="preview"&&!publicationApproved&&(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))notFound();return <EMSherifCaseStudy/>}
