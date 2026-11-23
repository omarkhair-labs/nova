import { isProductionSite } from "../../publication";
import type { Metadata } from "next";
import ProjectArchive from "../../ProjectArchive";
export const dynamic = "force-dynamic";
export const metadata: Metadata = {title:"Project Archive | Plantupia",description:"Explore selected planting and architectural design proposals.",robots:isProductionSite?{index:true,follow:true}:{index:false,follow:false}};
export default function ProjectsPage(){

 return <ProjectArchive/>;
}
