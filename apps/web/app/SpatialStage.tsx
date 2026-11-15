import type { CSSProperties, ReactNode } from "react";
import { Photograph, type Plate } from "./Photograph";

export function SceneBackdrop({ image, className = "", priority = true }: { image: Plate; className?: string; priority?: boolean }) {
  return <div className={"scene-backdrop " + className} aria-hidden="true">
    <Photograph image={{ ...image, alt: "" }} caption={null} priority={priority} sizes="100vw" />
  </div>;
}

export function SceneNote({ id, label, title, children }: { id?: string; label?: string; title: ReactNode; children?: ReactNode }) {
  return <div className="scene-note">
    {label && <p className="scene-coordinate">{label}</p>}
    <h1 id={id} tabIndex={id ? -1 : undefined}>{title}</h1>
    {children}
  </div>;
}

export function SceneCredit({ children = "Generated concept" }: { children?: ReactNode }) {
  return <p className="scene-credit">{children}</p>;
}

export function scenePoint(x: number, y: number) {
  return { "--image-x": x, "--image-y": y } as CSSProperties;
}
