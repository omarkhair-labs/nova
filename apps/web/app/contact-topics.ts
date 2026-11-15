export const topics = [
  { id: "project", label: "Start a project", prompt: "What’s the place like today, and what would you like to change?" },
  { id: "care", label: "Care & support", prompt: "What have you noticed, and when did you first notice it?" },
  { id: "general", label: "Say hello", prompt: "What would you like to talk about?" },
] as const;

export type Topic = (typeof topics)[number]["id"];
