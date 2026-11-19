import type { Metadata } from "next";
import { notFound } from "next/navigation";
import ProjectArchive from "../../ProjectArchive";
export const dynamic = "force-dynamic";
export const metadata: Metadata = {title:"Project Archive | Plantupia",description:"Explore selected planting and architectural design proposals.",robots:process.env.VERCEL_ENV==="production"?{index:true,follow:true}:{index:false,follow:false}};
export default function ProjectsPage(){
 if(process.env.VERCEL_ENV !== "production" && (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)) notFound();
 return <ProjectArchive/>;
}
