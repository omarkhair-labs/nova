import { NextResponse } from "next/server";
import { commerceApiOrigin } from "../../../../../shop/catalog";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const safeMediaTypes = new Set(["image/jpeg", "image/png", "image/webp"]);

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  if (!uuid.test(id)) return new NextResponse(null, { status: 404 });
  const base = commerceApiOrigin();
  if (!base) return new NextResponse(null, { status: 503 });
  try {
    const url = new URL(
      "/api/v1/plantupia/commerce/public/products/" + id + "/cover/", base,
    );
    const upstream = await fetch(url, {
      redirect: "error", cache: "no-store", signal: AbortSignal.timeout(8000),
    });
    if (upstream.status === 404) return new NextResponse(null, { status: 404 });
    if (!upstream.ok) return new NextResponse(null, { status: 503 });
    const type = upstream.headers.get("content-type")?.split(";")[0].trim() ?? "";
    const length = Number(upstream.headers.get("content-length") ?? "0");
    if (!safeMediaTypes.has(type) || length > 4 * 1024 * 1024) {
      return new NextResponse(null, { status: 502 });
    }
    const body = new Uint8Array(await upstream.arrayBuffer());
    if (body.length === 0 || body.length > 4 * 1024 * 1024) {
      return new NextResponse(null, { status: 502 });
    }
    return new NextResponse(body, {
      status: 200,
      headers: {
        "Content-Type": type,
        "Cache-Control": "public, max-age=60",
        "X-Content-Type-Options": "nosniff",
      },
    });
  } catch {
    return new NextResponse(null, { status: 503 });
  }
}
