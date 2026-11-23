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


/** Refinement III: real sector filtering and less visual chrome on the mobile fold. */
for(const width of [320,390,430]){
  test.describe("Plantupia photographic archive refinement / "+width+"px",()=>{
    test.use({baseURL:"http://127.0.0.1:3017",viewport:{width,height:844}});
    test("Sector folios filter live without changing the six source studies",async({page})=>{
      await page.goto("/spaces/projects");
      const tiles=page.locator(".pa-wall .pa-project");
      await expect(tiles).toHaveCount(6);
      const all=page.locator('.pa-contexts button').filter({hasText:"All studies"});
      const hospitality=page.locator('.pa-contexts button').filter({hasText:"Hospitality"});
      const commercial=page.locator('.pa-contexts button').filter({hasText:"Commercial"});
      const residential=page.locator('.pa-contexts button').filter({hasText:"Residential"});
      await expect(all).toHaveAttribute("aria-pressed","true");
      await hospitality.click();
      await expect(hospitality).toHaveAttribute("aria-pressed","true");
      await expect(tiles).toHaveCount(3);
      await expect(page.getByRole("status")).toContainText("03 / 06");
      await commercial.click();
      await expect(tiles).toHaveCount(2);
      await residential.click();
      await expect(tiles).toHaveCount(1);
      await all.click();
      await expect(tiles).toHaveCount(6);
      await tiles.first().locator("a.pa-project-link").click();
      await expect(page).toHaveURL(/\/spaces\/projects\/mazal$/);
    });
    test("Pocket reading index is concise and avoids stacking sticky bars",async({page})=>{
      await page.goto("/spaces/projects/mazal");
      const ribbon=page.locator("nav.mrf");
      await expect(ribbon).toBeVisible();
      const metrics=await ribbon.evaluate(el=>({position:getComputedStyle(el).position,height:el.getBoundingClientRect().height}));
      expect(metrics.position).not.toBe("sticky");
      expect(metrics.height).toBeLessThanOrEqual(67);
    });
    test("World Index prioritizes chapter navigation over Shop",async({page})=>{
      await page.goto("/");
      await page.getByRole("button",{name:"Open Plantupia world index"}).click();
      const menu=page.locator("dialog.si-overlay");
      const correctOrder=await menu.evaluate(dialog=>{
        const chapter=dialog.querySelector(".si-links");
        const shop=dialog.querySelector(".si-mobile-shop");
        return !!chapter&&!!shop&&!!(chapter.compareDocumentPosition(shop)&Node.DOCUMENT_POSITION_FOLLOWING);
      });
      expect(correctOrder).toBe(true);
      await expect(menu.locator(".si-link-image")).toHaveCount(6);
    });
  });
}


/** Project imagery is a source-led reading experience on the phone, not a thumbnail dead end. */
for(const width of [320,390,430]){
  test.describe("Pocket Field / native source photo reader "+width+"px",()=>{
    test.use({baseURL:"http://127.0.0.1:3017",viewport:{width,height:844}});
    test("Photo opens with source and description, cycles and returns focus",async({page})=>{
      await page.goto("/spaces/projects/em-sherif");
      const first=page.locator(".emc-visual-field .emc-wall-item>img").first();
      await expect(first).toHaveAttribute("data-pocket-photo","true");
      await expect(first).toHaveAttribute("role","button");
      const originalSrc=await first.getAttribute("src");
      await first.click();
      const dialog=page.getByRole("dialog",{name:"Plantupia full-size study photographs"});
      await expect(dialog).toBeVisible();
      await expect(dialog.locator(".pf-room-frame img")).toHaveAttribute("src",originalSrc);
      await expect(dialog.locator(".pf-room-progress")).toContainText("01 / 07");
      await dialog.getByRole("button",{name:"Next photograph"}).click();
      await expect(dialog.locator(".pf-room-progress")).toContainText("02 / 07");
      await page.keyboard.press("ArrowLeft");
      await expect(dialog.locator(".pf-room-progress")).toContainText("01 / 07");
      await page.keyboard.press("Escape");
      await expect(dialog).toBeHidden();
      await expect(first).toBeFocused();
    });
    test("Case compare remains separate from source photo reader",async({page})=>{
      await page.goto("/spaces/projects/mazal");
      const source=page.locator(".mz-wall-item>img").first();
      await expect(source).toHaveAttribute("data-pocket-photo","true");
      const comparison=page.locator(".mz-registered-frame img").first();
      await expect(comparison).not.toHaveAttribute("data-pocket-photo","true");
      await source.focus();
      await page.keyboard.press("Enter");
      await expect(page.locator("dialog.pf-photo-room")).toBeVisible();
      await page.locator(".pf-room-close").click();
      await expect(page.locator("dialog.pf-photo-room")).toBeHidden();
    });
  });
}
test.describe("Pocket Field / reader desktop independence",()=>{
  test.use({baseURL:"http://127.0.0.1:3017",viewport:{width:1440,height:900}});
  test("Desk project photos remain ordinary images",async({page})=>{
    await page.goto("/spaces/projects/em-sherif");
    const first=page.locator(".emc-visual-field .emc-wall-item>img").first();
    await expect(first).not.toHaveAttribute("data-pocket-photo","true");
    await expect(page.locator("dialog.pf-photo-room")).toBeHidden();
  });
});


/** Source-to-project handoff: never send on behalf of a visitor. */
for(const width of [320,390,430]){
 test.describe("Pocket Field / source-backed project enquiry "+width+"px",()=>{
  test.use({baseURL:"http://127.0.0.1:3017",viewport:{width,height:844}});
  test("One existing proposal leads to a useful, editable draft",async({page})=>{
    await page.goto("/spaces/projects/mazal");
    const handoff=page.locator(".ct-trail .ct-ask");
    await expect(handoff).toHaveAttribute("href","/start-a-space?space=gather&case=mazal");
    await handoff.click();
    await expect(page).toHaveURL(/\/start-a-space\?space=gather&case=mazal$/);
    await expect(page.locator(".ct-case-context strong")).toHaveText("Mazal");
    await expect(page.locator("#ct-space")).toHaveValue("gather");
    await expect(page.locator("#ct-message")).toHaveValue(/Mazal design proposal/);
    await expect(page.locator(".ct-case-context")).toContainText("design proposal");
    await expect(page.getByRole("button",{name:/Send your message/})).toBeDisabled();
  });
  test("Unknown case parameter never inserts unsupported content",async({page})=>{
    await page.goto("/start-a-space?space=gather&case=not-a-real-study");
    await expect(page.locator(".ct-case-context")).toHaveCount(0);
    await expect(page.locator("#ct-message")).toHaveValue("");
  });
 });
}
