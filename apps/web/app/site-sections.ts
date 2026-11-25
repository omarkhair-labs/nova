export type SiteSection = "experience" | "spaces" | "studio" | "systems" | "about" | "contact";

export const siteSections: ReadonlyArray<{
  id: SiteSection;
  number: string;
  href: string;
  name: string;
  line: string;
}> = [
  { id: "experience", number: "01", href: "/", name: "The Experience", line: "Enter the world where space and life meet." },
  { id: "spaces", number: "02", href: "/spaces", name: "Spaces", line: "Explore the questions behind different environments." },
  { id: "studio", number: "03", href: "/studio", name: "The Studio", line: "Understand how our thinking becomes a living place." },
  { id: "systems", number: "04", href: "/living-systems", name: "Living Systems", line: "Care, records and what continues after installation." },
  { id: "about", number: "05", href: "/about", name: "About Plantupia", line: "Who we are, what we do, and the work behind it." },
  { id: "contact", number: "06", href: "/start-a-space", name: "Get in touch", line: "Start a project or talk about an existing space." },
] as const;
