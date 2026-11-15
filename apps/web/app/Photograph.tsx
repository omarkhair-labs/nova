import type { ReactNode } from "react";

export type Plate = Readonly<{
  file: string;
  alt: string;
  width: number;
  height: number;
}>;

export const courtyard: Plate = {
  file: "hero",
  alt: "An olive tree rises from a sunken bed in a limestone courtyard, seen through an arch",
  width: 1536,
  height: 1024,
};

export const materials: Plate = {
  file: "studio",
  alt: "An olive branch, pencil and tracing paper laid on a limestone worktable",
  width: 1536,
  height: 1024,
};

export function Photograph({
  image,
  caption,
  className = "",
  sizes = "(max-width: 760px) 90vw, 54vw",
  priority = false,
}: { image: Plate; caption: ReactNode; className?: string; sizes?: string; priority?: boolean }) {
  const file = "/field/" + image.file;
  const smallWidth = Math.round(image.width * 760 / Math.max(image.width, image.height));

  return <figure className={"field-plate " + className}>
    <div className="field-image">
      <img
        src={file + ".webp"} srcSet={file + "-small.webp " + smallWidth + "w, " + file + ".webp " + image.width + "w"}
        sizes={sizes} width={image.width} height={image.height} alt={image.alt}
        loading={priority ? "eager" : "lazy"} fetchPriority={priority ? "high" : undefined}
        decoding={priority ? "auto" : "async"}
      />
    </div>
    {caption && <figcaption>{caption}</figcaption>}
  </figure>;
}
