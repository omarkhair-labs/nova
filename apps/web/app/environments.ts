import type { Plate } from "./Photograph";

type Environment = {
  slug: string;
  name: string;
  question: string;
  title: string;
  intro: string;
  details: readonly { title: string; text: string }[];
  image: Plate;
};

export const environments: readonly Environment[] = [
  {
    slug: "home",
    name: "Homes",
    question: "Where does the light reach?",
    title: "A place in the everyday.",
    intro: "A single plant can change the corner you pass every morning. Start with the rooms you use, the light they receive, and how much care fits into your day.",
    details: [
      { title: "Light through the day", text: "A bright window and a deep room are different environments. Look at where direct light falls and how conditions change when the blinds are drawn." },
      { title: "Room to live", text: "Give stems and leaves room to develop while keeping doors, seats and walkways clear. Consider pets and children when choosing what can go where." },
      { title: "An easy routine", text: "Think about how to reach the soil, carry water and clear fallen leaves. A care routine is easier to keep when it fits the room and the people who live there." },
    ],
    image: { file: "home", alt: "A potted tree by a sunlit apartment window beside a reading chair", width: 1536, height: 1024 },
  },
  {
    slug: "work",
    name: "Workplaces",
    question: "How do people move and pause?",
    title: "A quieter edge to the working day.",
    intro: "Planting can define the edge of a desk area, soften a shared room or give a busy route a place to pause. The design starts with how the working day actually unfolds.",
    details: [
      { title: "Shared rhythms", text: "Trace the paths people take at arrival, between meetings and on breaks. Position planting where it can mark a change of pace without narrowing a route." },
      { title: "The working climate", text: "Look at light, air movement and heating or cooling at plant level. Conditions at a window can differ sharply from those at a desk a few steps away." },
      { title: "Access after installation", text: "Plan care visits around the working day and keep planters accessible without shifting furniture or interrupting shared work." },
    ],
    image: { file: "work", alt: "A low planted bed separates a walkway from desks in a daylight-filled workplace", width: 1536, height: 1024 },
  },
  {
    slug: "gather",
    name: "Hospitality",
    question: "What does arrival feel like?",
    title: "A welcome with roots.",
    intro: "A hotel entrance, restaurant or guest lounge has a rhythm of arrivals, pauses and departures. Planting can give each of those moments a different character.",
    details: [
      { title: "The approach", text: "Read the entrance from a guest’s point of view, including where luggage passes, where people wait and what they need to see first." },
      { title: "Planting at the right scale", text: "Give larger specimens enough growing space. Set the height and density of planting with staff sightlines and the movement of guests in mind." },
      { title: "Behind the welcome", text: "Make it possible to reach, clean and care for the bed during ordinary operations. The service route belongs in the design brief." },
    ],
    image: { file: "gather", alt: "A planted bed marks a turn in a limestone hotel lobby near reception", width: 1536, height: 1024 },
  },
  {
    slug: "industrial",
    name: "Industrial sites",
    question: "Where is planting practical and safe?",
    title: "A place to pause between shifts.",
    intro: "A staff courtyard, break area or administrative entrance can offer room for planting. The site's safety rules, operating conditions and access determine what is possible.",
    details: [
      { title: "Start with site rules", text: "Work within designated areas and keep production, emergency and loading routes clear. Confirm access and approvals with the people who operate the site." },
      { title: "Exposure", text: "Observe wind, dust and heat where the planting would sit. The conditions around a factory entrance may be very different from those in a sheltered staff courtyard." },
      { title: "Safe upkeep", text: "Agree who can enter and when, how water is available and how plant materials can be moved without affecting operations." },
    ],
    image: { file: "industrial", alt: "A protected raised bed and shaded bench beside a light-industrial building", width: 1536, height: 1024 },
  },
  {
    slug: "retail",
    name: "Retail",
    question: "Where can a visitor pause?",
    title: "Life between destinations.",
    intro: "From a small shop to a retail passage, planting can support the way visitors find, enter and spend time in a space.",
    details: [
      { title: "Sightlines", text: "Walk the approach from several directions. Place planting where it can define a threshold while keeping entries, displays and wayfinding legible." },
      { title: "Changing displays", text: "Account for layouts that change through the year. Reserve enough floor and growing space for plants when fixtures or queues shift." },
      { title: "Opening hours", text: "Make routine watering and cleaning possible around visitor traffic. Check how planters can be reached when the space is busy." },
    ],
    image: { file: "retail", alt: "A tree and low planting bed define a pause along a glazed retail passage", width: 1536, height: 1024 },
  },
  {
    slug: "public",
    name: "Public spaces",
    question: "What makes it easier to stay?",
    title: "Ground to share.",
    intro: "A courtyard, civic entrance or shared forecourt is used in many ways across a day. Planting can help make room for people moving through and people staying awhile.",
    details: [
      { title: "Shade and seasons", text: "Observe which parts of the site people choose in sun, shade and wind. Consider how exposure changes with the season and time of day." },
      { title: "Space for different users", text: "Keep step-free routes and sightlines open. Think about where seating, planting and circulation can support each other." },
      { title: "A shared responsibility", text: "Agree who can look after the planting and how they can access the beds. Plan for the intensity of public use from the start." },
    ],
    image: { file: "public", alt: "Trees cast dappled shade over seating and paths in a civic square", width: 1536, height: 1024 },
  },
];

export function getEnvironment(slug?: string) {
  return environments.find(item => item.slug === slug);
}
