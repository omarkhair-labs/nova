import type { Metadata } from "next";
import { notFound } from "next/navigation";
import VisualDirectionLab from "../../VisualDirectionLab";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Visual Direction Lab | Plantupia",robots:{index:false,follow:false}};
export default function VisualDirectionPage(){
 if(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)notFound();
 return <VisualDirectionLab/>;
}
