#!/usr/bin/env node
/**
 * Integration closure against a disposable HTTPS mock. No external backends,
 * customer records, production credentials, emails, or Vercel changes.
 *
 * Validates an already-built Next.js website with VERCEL_ENV=production,
 * no separate photo/publication approval and an enabled Company Inbox.
 * The project pages are now normal routes; no approval switch hides them.
 */
import assert from "node:assert/strict";
import { spawn, execFileSync } from "node:child_process";
import { createServer } from "node:https";
import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";

const temp = mkdtempSync(path.join(tmpdir(), "plantupia-contract-"));
const cert = path.join(temp, "ca.crt");
const key = path.join(temp, "key.pem");
const expectedRef = "11111111-1111-4111-8111-111111111111";
const expectedToken = "synthetic-plantupia-integration-".padEnd(48, "x");
const stored = new Set();
let backendPosts = 0;
let catalogReads = 0;
let mock;
let site;
let siteLog = "";
let completed = false;
function log(label, detail="") {
  console.log("PASS " + label + (detail ? " — " + detail : ""));
}
function answer(response,status,body) {
  response.writeHead(status, { "Content-Type": "application/json", "Cache-Control": "no-store" });
  response.end(JSON.stringify(body));
}
async function collect(req) {
  let data = "";
  for await (const chunk of req) {
    data += chunk;
    if(data.length > 16384) throw Error("Mock input too large");
  }
  return JSON.parse(data);
}
async function check(pathname, options) {
  const response = await fetch("http://127.0.0.1:3017" + pathname, {
    signal: AbortSignal.timeout(12000), redirect:"manual", ...options
  });
  return { status: response.status, response, text: await response.text() };
}
async function ready() {
  const start = Date.now();
  while (Date.now() - start < 25000) {
    if (site.exitCode !== null) throw Error("Next process exited unexpectedly: "+siteLog.slice(-600));
    try {
      const resp = await check("/");
      if(resp.status===200) return;
    } catch { /* starting */ }
    await new Promise(resolve=>setTimeout(resolve,250));
  }
  throw Error("Next did not start: "+siteLog.slice(-800));
}
try {
  execFileSync("openssl",[
    "req","-x509","-newkey","rsa:2048","-nodes","-days","1",
    "-keyout",key,"-out",cert,"-subj","/CN=localhost",
    "-addext","subjectAltName=DNS:localhost,IP:127.0.0.1"
  ],{stdio:"ignore"});
  const sku = {
    sku_id:"22222222-2222-4222-8222-222222222222",
    product_id:"33333333-3333-4333-8333-333333333333",
    name:"INTEGRATION ONLY TEST PLANTER",
    description:"Synthetic isolated contract test, not actual merchandise",
    kind:"planter",option:"Standard",
    price:"499.00",currency:"EGP",in_stock:true,has_image:false,
    image_alt:"Synthetic planter test"
  };
  mock = createServer({key:readFileSync(key),cert:readFileSync(cert)},async(req,res)=>{
    const url = new URL(req.url,"https://localhost");
    try {
      if(req.method==="GET" && url.pathname==="/api/v1/plantupia/commerce/public/catalog/"){
        catalogReads++;
        return answer(res,200,{
          source:"reviewed_published_commerce_skus",
          results:[sku,{...sku,sku_id:"44444444-4444-4444-8444-444444444444",
            name:"FICTIONAL QA B02",option:"Exclude QA"}]
        });
      }
      if(req.method==="POST" && url.pathname==="/api/v1/plantupia/website/enquiries/"){
        assert.equal(req.headers.authorization,"Bearer "+expectedToken);
        const body=await collect(req);
        assert.equal(body.topic,"project");
        assert.match(body.submission_id,/^[a-f0-9-]{36}$/i);
        assert.equal(body.website,undefined);
        backendPosts++;
        const replay=stored.has(body.submission_id);
        stored.add(body.submission_id);
        return answer(res,replay?200:201,{reference:expectedRef});
      }
      return answer(res,404,{error:"not_found"});
    }catch(e){
      return answer(res,400,{error:"isolated_backend_rejected"});
    }
  });
  await new Promise((resolve,reject)=>{
    mock.once("error",reject);
    mock.listen(0,()=>resolve());
  });
  const port=mock.address().port;
  const env={
    ...process.env,
    NODE_ENV:"production",
    VERCEL_ENV:"production",
    PLANTUPIA_PUBLICATION_APPROVED:"0",
    PLANTUPIA_WEBSITE_INBOX_ENABLED:"1",
    PLANTUPIA_WEBSITE_INBOX_BACKEND:"https://localhost:"+port,
    PLANTUPIA_WEBSITE_INBOX_TOKEN:expectedToken,
    PLANTUPIA_PUBLIC_COMMERCE_BACKEND:"https://localhost:"+port,
    NODE_EXTRA_CA_CERTS:cert,
    NEXT_TELEMETRY_DISABLED:"1"
  };
  site=spawn(process.execPath,["node_modules/next/dist/bin/next","start","-p","3017"],
    {cwd:process.cwd(),env,stdio:["ignore","pipe","pipe"]});
  for(const stream of [site.stdout,site.stderr]){
    stream.on("data",data=>{
      siteLog=(siteLog+data.toString()).slice(-6000);
    });
  }
  await ready();
  log("Built website starts with independent production Inbox and unpublished photos");

  const c=await check("/start-a-space");
  assert.equal(c.status,200);
  assert.ok(c.text.includes("Your details and message will be saved"));
  assert.ok(!c.text.includes("Online enquiries unavailable"));
  log("Production Contact remains enabled independently of photo approvals");

  const body={
    submission_id:"55555555-5555-4555-8555-555555555555",
    topic:"project",name:"Synthetic QA",email:"qa@example.invalid",
    organization:"",environment:"work",location:"",message:"Isolated contract test only."
  };
  const headers={"Origin":"https://127.0.0.1:3017","Content-Type":"application/json"};
  const posted=await check("/api/website/enquiries",{
    method:"POST",headers,body:JSON.stringify(body)
  });
  assert.equal(posted.status,201,"First acceptance response: "+posted.text.slice(0,180));
  assert.deepEqual(JSON.parse(posted.text),{reference:expectedRef});
  assert.equal(backendPosts,1);
  assert.equal(stored.size,1);
  log("Real Next route forwards synthetic project enquiry to isolated HTTPS mock; receives reference");

  const replay=await check("/api/website/enquiries",{
    method:"POST",headers,body:JSON.stringify(body)
  });
  assert.equal(replay.status,200);
  assert.deepEqual(JSON.parse(replay.text),{reference:expectedRef});
  assert.equal(backendPosts,2);
  assert.equal(stored.size,1);
  log("Retry preserves reference and yields one synthetic backend record");

  const blocked=await check("/api/website/enquiries",{
    method:"POST",headers:{"Origin":"https://other.example.invalid","Content-Type":"application/json"},
    body:JSON.stringify(body)
  });
  assert.equal(blocked.status,403);
  assert.equal(backendPosts,2);
  log("Cross-origin message blocked before reaching backend");

  const pixels=await check("/spaces/projects/pixels-mall");
  assert.equal(pixels.status,200);
  const image=await check("/api/private-arrival/archive-pixels-p08.webp");
  assert.equal(image.status,404); // CI doesn't bundle source pictures
  log("Project case is public without fabricated approval; unavailable image returns 404");

  const shop=await check("/shop");
  assert.equal(shop.status,200);
  assert.ok(shop.text.includes(sku.name),"Published item missing from SSR shop");
  assert.ok(!shop.text.includes("FICTIONAL QA B02"),"QA fixture leaked into storefront");
  assert.ok(catalogReads>=1);
  log("Read-only Shop consumes valid published-SKU contract and excludes QA fixture");

  const robots=await check("/robots.txt");
  assert.ok(robots.text.includes("Disallow: /"));
  log("Unapproved deployment remains noindex");
  completed=true;
  console.log("8/8 isolated backend integration checkpoints passed");
}catch(error){
  console.error("FAIL isolated integration:",error.message);
  if(siteLog) console.error("Next server tail:",siteLog.slice(-900));
  process.exitCode=1;
}finally{
  if(site && site.exitCode===null)site.kill("SIGTERM");
  if(mock)await new Promise(resolve=>mock.close(()=>resolve()));
  rmSync(temp,{recursive:true,force:true});
  if(!completed) console.error("No production backend or real visitor contacted.");
}
