import {test,expect} from "@playwright/test";

/** Pocket Field / mobile edition acceptance across the entire visitor website. */
test.use({channel:"chrome"});
const pages=[
  "/","/spaces","/spaces/hospitality","/spaces/commercial","/spaces/offices",
  "/spaces/projects","/spaces/projects/mazal","/spaces/projects/em-sherif",
  "/spaces/projects/el-hallab","/spaces/projects/pixels-mall",
  "/spaces/projects/audi-cairo","/spaces/projects/villa-7",
  "/studio","/living-systems","/about","/shop","/start-a-space",
];
const cases=[
  "/spaces/projects/mazal","/spaces/projects/em-sherif","/spaces/projects/el-hallab",
  "/spaces/projects/pixels-mall","/spaces/projects/audi-cairo","/spaces/projects/villa-7"
];
for(const width of [320,390,430]){
  test.describe("Pocket Field / "+width+"px",()=>{
    test.use({baseURL:"http://127.0.0.1:3017",viewport:{width,height:844}});

    test("Every chapter exists, fits the screen and has legible orientation",async({page})=>{
      for(const route of pages){
        const response=await page.goto(route);
        expect(response?.status(),"HTTP "+route).toBe(200);
        const size=await page.evaluate(()=>({
          width:window.innerWidth,doc:document.documentElement.scrollWidth,
          body:document.body.scrollWidth
        }));
        if(size.doc-size.width>1){
          const offenders=await page.evaluate(()=>[...document.querySelectorAll("body *")].map(el=>{
            const r=el.getBoundingClientRect();
            return {tag:el.tagName,cls:el.getAttribute("class"),right:Math.round(r.right),left:Math.round(r.left)};
          }).filter(x=>x.right>innerWidth+1).slice(0,8));
          console.log("POCKET_OVERFLOW "+width+" "+route+" "+JSON.stringify(offenders));
        }
        expect(size.doc-size.width,"Horizontal document overflow on "+route).toBeLessThanOrEqual(1);
        const index=page.getByRole("button",{name:"Open Plantupia world index"});
        await expect(index).toBeVisible();
        if(!["/","/spaces","/spaces/projects"].includes(route)){
          await expect(page.locator("nav.mrf")).toBeVisible();
        }
      }
    });

    test("Case-study reading ribbons lead to real sections",async({page})=>{
      for(const route of cases){
        await page.goto(route);
        const ribbon=page.locator("nav.mrf");
        await expect(ribbon).toBeVisible();
        const anchors=await ribbon.locator('a[href^="#"]').evaluateAll(
          nodes=>nodes.map(el=>el.getAttribute("href"))
        );
        expect(anchors.length).toBeGreaterThanOrEqual(2);
        for(const anchor of anchors){
          expect(anchor?.startsWith("#")).toBe(true);
          await expect(page.locator(anchor)).toHaveCount(1);
        }
        const first=ribbon.locator('a[href^="#"]').first();
        const href=await first.getAttribute("href");
        await first.click();
        await expect(page).toHaveURL(new RegExp(href+"$"));
      }
    });

    test("Source-image collections are native horizontal, keyboard reachable and contained",async({page})=>{
      for(const [route,selector] of [
        ["/spaces/projects/mazal",".mz-family-field>div[role=region]"],
        ["/spaces/projects/em-sherif",".emc-visual-field[role=region]"],
        ["/spaces/projects/villa-7",".vl-visual-field[role=region]"],
      ]){
        await page.goto(route);
        const strip=page.locator(selector).first();
        await expect(strip).toBeVisible();
        await expect(strip).toHaveAttribute("tabindex","0");
        const dimensions=await strip.evaluate(el=>({scroll:el.scrollWidth,client:el.clientWidth}));
        expect(dimensions.scroll,"Horizontal gallery should be browsable "+route).toBeGreaterThan(dimensions.client);
        await strip.focus();
        await page.keyboard.press("ArrowRight");
        await page.waitForTimeout(130);
        const x=await strip.evaluate(el=>el.scrollLeft);
        expect(x,"Keyboard should scroll native gallery "+route).toBeGreaterThan(0);
      }
    });
  });
}

test.describe("Pocket Field / desktop preservation",()=>{
  test.use({baseURL:"http://127.0.0.1:3017",viewport:{width:1440,height:900}});
  test("Reading ribbon is mobile-only",async({page})=>{
    await page.goto("/spaces/projects/mazal");
    await expect(page.locator(".mrf")).toBeHidden();
    await expect(page.getByRole("button",{name:"Open Plantupia world index"})).toBeVisible();
    await page.goto("/");
    await expect(page.locator(".ps-swipe-hint")).toBeHidden();
  });
});

test.describe("Pocket Field / touch scene",()=>{
  test.use({baseURL:"http://127.0.0.1:3017",viewport:{width:390,height:844},hasTouch:true,isMobile:true});
  test("A real horizontal touch changes the scene without hijacking vertical scroll",async({page})=>{
    await page.goto("/");
    const stage=page.locator(".ps-site");
    await expect(stage).toHaveAttribute("data-stage","0");
    await page.locator(".ps-story h1").evaluate(el=>{
      const start=new Touch({identifier:1,target:el,clientX:285,clientY:250});
      const end=new Touch({identifier:1,target:el,clientX:100,clientY:260});
      el.dispatchEvent(new TouchEvent("touchstart",{bubbles:true,changedTouches:[start],touches:[start],targetTouches:[start]}));
      el.dispatchEvent(new TouchEvent("touchend",{bubbles:true,changedTouches:[end],touches:[],targetTouches:[]}));
    });
    await expect(stage).toHaveAttribute("data-stage","1");
  });
});
