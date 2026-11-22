import { publicationApproved } from "../../../publication";
import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function reply(data: object, status: number) {
  return NextResponse.json(data, { status, headers: { "Cache-Control": "no-store" } });
}

export async function POST(request: NextRequest) {
  if (!publicationApproved) return reply({error:"unavailable"},503);
  const token = process.env.PLANTUPIA_WEBSITE_INBOX_TOKEN;
  const backend = process.env.PLANTUPIA_WEBSITE_INBOX_BACKEND;
  if (process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED !== "1" || !backend || !token || token.length < 32) {
    return reply({ error: "unavailable" }, 503);
  }

  // Browser submissions must originate from this same site. Never forward cookies.
  const origin = request.headers.get("origin");
  const host = request.headers.get("host");
  try {
    if (!origin || !host || new URL(origin).host !== host || new URL(origin).protocol !== "https:") {
      return reply({ error: "invalid_origin" }, 403);
    }
  } catch {
    return reply({ error: "invalid_origin" }, 403);
  }
  if (request.headers.get("content-type")?.split(";")[0] !== "application/json") {
    return reply({ error: "invalid_request" }, 400);
  }

  let input: unknown;
  try {
    const reader = request.body?.getReader();
    if (!reader) return reply({ error: "invalid_request" }, 400);
    const chunks: Uint8Array[] = [];
    let length = 0;
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      length += value.byteLength;
      if (length > 16384) {
        await reader.cancel();
        return reply({ error: "invalid_request" }, 413);
      }
      chunks.push(value);
    }
    const bytes = new Uint8Array(length);
    let offset = 0;
    for (const chunk of chunks) {
      bytes.set(chunk, offset);
      offset += chunk.byteLength;
    }
    input = JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    return reply({ error: "invalid_request" }, 400);
  }
  if (typeof input !== "object" || input === null || Array.isArray(input)) {
    return reply({ error: "invalid_request" }, 400);
  }
  const body = input as Record<string, unknown>;
  if (body.website !== "" && body.website !== undefined) {
    return reply({ error: "invalid_request" }, 400);
  }
  const keys = ["submission_id", "topic", "name", "email", "organization", "environment", "location", "message"] as const;
  if (keys.some(key => typeof body[key] !== "string")) {
    return reply({ error: "invalid_fields" }, 400);
  }
  try {
    const base = new URL(backend);
    if (base.protocol !== "https:" || base.username || base.password) return reply({ error: "unavailable" }, 503);
    const endpoint = new URL("/api/v1/plantupia/website/enquiries/", base);
    const upstream = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: "Bearer " + token },
      body: JSON.stringify(Object.fromEntries(keys.map(key => [key, body[key]]))),
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(12000),
    });
    if (upstream.status === 429) return reply({ error: "rate_limited" }, 429);
    if (upstream.status === 400 || upstream.status === 409) return reply({ error: "invalid_fields" }, upstream.status);
    if (!upstream.ok) return reply({ error: "unavailable" }, 503);
    const result = await upstream.json();
    if (typeof result.reference !== "string" || !/^[0-9a-f-]{36}$/i.test(result.reference)) {
      return reply({ error: "unavailable" }, 503);
    }
    return reply({ reference: result.reference }, upstream.status === 201 ? 201 : 200);
  } catch {
    return reply({ error: "unavailable" }, 503);
  }
}
