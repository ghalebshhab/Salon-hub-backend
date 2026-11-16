"""Build the A4 paged edition of the Salonhub documentation.

Usage (from this folder):
    python build_paged_edition.py ../Salon-Hup-Project-Documentation.md
    python -m http.server 8765            # serve the generated HTML
    node print_pdf.mjs "<path to chrome.exe>" "http://localhost:8765/salonhub-doc-paged.html?print=1" ../../output/pdf/Salon-Hup-Project-Documentation.pdf

The HTML shows A4 pages with a cover, a contents list with page numbers,
"Page N of M" in each footer, and a gap between pages on screen.
"""
import json,re,sys
md=open(sys.argv[1],encoding='utf-8').read()
version=re.search(r'\*\*Document version\*\* \| ([^|]+) \|',md).group(1).strip()
updated=re.search(r'\*\*Last updated\*\* \| ([^|]+) \|',md).group(1).strip()
tpl=r'''<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Salonhub Product Doc</title>
<link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,600&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
<style>
:root{--fg:#292329;--muted:#746b72;--line:#e8dedf;--plum:#76546f;--soft:#f1e8ef;--gold:#d7a86e}
@page{size:A4;margin:22mm 18mm 20mm 18mm;
  @top-left{content:"Salonhub · Product and Engineering Documentation";font:500 8pt Inter,sans-serif;color:#746b72}
  @top-right{content:"Version __VER__ · __DATE__";font:500 8pt Inter,sans-serif;color:#746b72}
  @bottom-center{content:"Page " counter(page) " of " counter(pages);font:600 9pt Inter,sans-serif;color:#76546f}}
@page cover{margin:0;@top-left{content:none}@top-right{content:none}@bottom-center{content:none}}
html,body{margin:0;color:var(--fg);font:10pt/1.55 Inter,system-ui,sans-serif}
html{color-scheme:light}
.cover{page:cover;height:297mm;box-sizing:border-box;padding:60mm 22mm 25mm;background:linear-gradient(160deg,#76546f 0%,#5e4058 60%,#3d2a39 100%);color:#fff;display:flex;flex-direction:column}
.cover .mark{width:64px;height:6px;background:var(--gold);border-radius:3px;margin-bottom:28px}
.cover h1{font:600 38pt/1.1 Fraunces,serif;color:#fff;border:0;margin:0 0 14px;padding:0;break-before:auto}
.cover p{font-size:13pt;opacity:.9;margin:0}
.cover dl{margin-top:auto;display:grid;grid-template-columns:40mm 1fr;gap:6px 12px;font-size:10.5pt}
.cover dt{opacity:.7}.cover dd{margin:0;font-weight:600}
.toc{break-before:page;break-after:page}
.toc h2{break-before:auto}
.toc ol{list-style:none;padding:0;margin:0}
.toc li a{display:flex;color:var(--fg);text-decoration:none;padding:3px 0}
.toc li a::after{content:target-counter(attr(href),page);margin-left:auto;padding-left:8px;font-weight:600;color:var(--plum)}
.toc li.part{margin-top:12px}.toc li.part a{font:600 12pt Fraunces,serif;color:var(--plum);border-bottom:1px solid var(--line)}
.toc li.ch a{padding-left:10px}
h1,h2{font-family:Fraunces,serif;color:var(--plum)}
h1{font-size:26pt;line-height:1.15;border-bottom:3px solid var(--gold);padding-bottom:6px;break-before:page;margin-top:0}
h2{font-size:17pt;border-bottom:1px solid var(--line);padding-bottom:4px;margin-top:22pt;break-after:avoid}
h3{font-size:12.5pt;margin-top:16pt;break-after:avoid}h4{color:var(--muted);break-after:avoid}
a{color:var(--plum)}code{background:var(--soft);padding:0 4px;border-radius:3px;font-size:.88em;overflow-wrap:anywhere}
pre{background:#211d23;color:#f8f3f6;padding:10px;border-radius:8px;white-space:pre-wrap;break-inside:avoid}pre code{background:none;color:inherit}
table{border-collapse:collapse;width:100%;margin:10px 0;font-size:8.5pt}
th{background:var(--soft);color:var(--plum);text-align:left}th,td{border:1px solid var(--line);padding:5px 7px;vertical-align:top;overflow-wrap:anywhere}
tr{break-inside:avoid}thead{display:table-header-group}
blockquote{margin:10px 0;padding:6px 12px;border-left:4px solid var(--gold);background:#fbf8f6}
.mermaid{border:1px solid var(--line);border-radius:10px;padding:8px;margin:10px 0;text-align:center;break-inside:avoid}
.mermaid svg{max-width:100%!important;max-height:200mm;height:auto}
#loading{font:600 14pt Inter;padding:40px;text-align:center}
</style>
<script>window.PagedConfig={auto:false};</script>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/pagedjs@0.4.3/dist/paged.polyfill.js"></script>
</head><body><div id="loading">Building pages…</div><div id="doc"></div>
<script>
const md=__MD__;
const r=new marked.Renderer();const base=r.code.bind(r);
r.code=(code,lang)=>lang==='mermaid'?'<div class="mermaid">'+code.replace(/</g,'&lt;')+'</div>':base(code,lang);
const used={};const slug=t=>{let s=t.toLowerCase().replace(/<[^>]+>/g,'').replace(/&[a-z]+;/g,'').replace(/[^\w\s-]/g,'').trim().replace(/\s+/g,'-')||'s';if(used[s]){s+='-'+(used[s]++)}else used[s]=1;return 'sec-'+s};
r.heading=(text,level)=>'<h'+level+' id="'+slug(text)+'">'+text+'</h'+level+'>';
const doc=document.getElementById('doc');
doc.innerHTML=marked.parse(md,{renderer:r,gfm:true});
// cover from the title and control table
const title=doc.querySelector('h1');const ctl=doc.querySelector('table');
const rows=[...ctl.querySelectorAll('tbody tr')].map(tr=>[tr.cells[0].textContent,tr.cells[1].textContent]).filter(([k])=>/version|updated|status|owner/i.test(k));
const cover=document.createElement('section');cover.className='cover';
cover.innerHTML='<div class="mark"></div><h1>'+title.textContent.replace(': ',':<br>')+'</h1><p>Product, design and engineering specification</p><dl>'+rows.map(([k,v])=>'<dt>'+k+'</dt><dd>'+v+'</dd>').join('')+'</dl>';
title.remove();doc.prepend(cover);
// first section (control table, how to read) must not start with a forced break
// replace the inline Contents with a numbered table of contents
const contents=[...doc.querySelectorAll('h2')].find(h=>h.textContent.trim()==='Contents');
let n=contents.nextElementSibling;while(n&&n.tagName!=='HR'){const x=n.nextElementSibling;n.remove();n=x}if(n)n.remove();
const toc=document.createElement('nav');toc.className='toc';
let html='<h2>Contents</h2><ol>';
doc.querySelectorAll('h1,h2').forEach(h=>{if(h.closest('.cover')||h===contents)return;
  if(h.tagName==='H1')html+='<li class="part"><a href="#'+h.id+'">'+h.textContent+'</a></li>';
  else if(/^\d+\./.test(h.textContent))html+='<li class="ch"><a href="#'+h.id+'">'+h.textContent+'</a></li>';});
toc.innerHTML=html+'</ol>';contents.replaceWith(toc);
// remaining horizontal rules are not needed: each part starts a new page
doc.querySelectorAll('hr').forEach(h=>h.remove());
mermaid.initialize({startOnLoad:false,theme:'base',themeVariables:{primaryColor:'#f1e8ef',primaryBorderColor:'#76546f',lineColor:'#76546f',fontFamily:'Inter'}});
window.docReady=false;
(async()=>{try{await mermaid.run({querySelector:'.mermaid'})}catch(e){window.mermaidError=String(e)}
  await document.fonts.ready;document.getElementById('loading').remove();
  const flow=await PagedPolyfill.preview();window.pageCount=flow.total;const st=document.createElement('style');st.textContent='.pagedjs_page:last-child{break-after:auto!important;page-break-after:auto!important}@media print{html,body{height:auto!important}}@media screen{html,body{background:#d9d2d6!important}.pagedjs_pages{display:flex;flex-direction:column;align-items:center;gap:40px;padding:40px 0 80px}.pagedjs_page{background:#fff!important;box-shadow:0 6px 24px rgba(41,35,41,.3)}}';document.head.appendChild(st);const fit=()=>document.querySelector('.pagedjs_pages').style.zoom=Math.min(1,(innerWidth-32)/794).toFixed(3);if(!location.search.includes('print')){fit();addEventListener('resize',fit);}window.docReady=true;document.title='Salonhub Product Doc ('+flow.total+' pages)';})();
</script></body></html>'''
open('salonhub-doc-paged.html','w',encoding='utf-8').write(tpl.replace('__VER__',version).replace('__DATE__',updated).replace('__MD__',json.dumps(md).replace('</','<\\/')))
print('ok')
