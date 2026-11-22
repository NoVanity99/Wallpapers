#!/usr/bin/env node
// Compiles every wallpaper shader in a headless browser (WebGL) and writes thumbnail.webp
// next to it. Also a quick way to catch shader compile errors before you push.
//
//   npm install playwright && npx playwright install chromium     (once)
//   node tools/render_thumbnails.js              all wallpapers
//   node tools/render_thumbnails.js aurora       just one
//   options: --sheet out.png (contact sheet)  --touch (render with a finger down)  --time 8
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const root = path.join(__dirname, '..');
const wallDir = path.join(root, 'catalog', 'wallpapers');
const header = fs.readFileSync(path.join(root, 'app/src/main/assets/shader_header.glsl'), 'utf8');
const footer = fs.readFileSync(path.join(root, 'app/src/main/assets/shader_footer.glsl'), 'utf8');
const args = process.argv.slice(2);
const opt = (name) => { const i = args.indexOf(name); return i >= 0 ? args.splice(i, 2)[1] : null; };
const flag = (name) => { const i = args.indexOf(name); if (i >= 0) args.splice(i, 1); return i >= 0; };
const sheet = opt('--sheet'), timeArg = opt('--time'), touch = flag('--touch');
const ids = args.length ? args : fs.readdirSync(wallDir).filter(d => fs.existsSync(path.join(wallDir, d, 'wallpaper.json'))).sort();
const W = 360, H = 640;

(async () => {
  const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const page = await browser.newPage();
  await page.setContent(`<canvas id=c width=${W} height=${H}></canvas>`);
  let failed = 0;
  const shots = [];
  for (const id of ids) {
    const dir = path.join(wallDir, id);
    const def = JSON.parse(fs.readFileSync(path.join(dir, 'wallpaper.json'), 'utf8'));
    const frag = header + '\n#line 1\n' + fs.readFileSync(path.join(dir, def.shader || 'shader.frag'), 'utf8') + footer;
    const time = Number(timeArg ?? def.thumbnailTime ?? 8) * (def.speed ?? 1);
    const res = await page.evaluate(({ frag, def, time, touch }) => {
      const c = document.getElementById('c');
      const gl = c.getContext('webgl', { preserveDrawingBuffer: true, antialias: false });
      const mk = (type, src) => { const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
        if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s)); return s; };
      try {
        const prog = gl.createProgram();
        gl.attachShader(prog, mk(gl.VERTEX_SHADER, 'attribute vec2 a_pos; void main(){ gl_Position = vec4(a_pos, 0., 1.); }'));
        gl.attachShader(prog, mk(gl.FRAGMENT_SHADER, frag));
        gl.linkProgram(prog);
        if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(prog));
        gl.useProgram(prog);
        const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
        const loc = gl.getAttribLocation(prog, 'a_pos'); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
        const u = (n) => gl.getUniformLocation(prog, n);
        const rgb = (h) => [1, 3, 5].map(i => parseInt(h.substr(i, 2), 16) / 255);
        gl.uniform2f(u('u_resolution'), c.width, c.height);
        gl.uniform1f(u('u_time'), time);
        gl.uniform1f(u('u_offset'), .5);
        gl.uniform4f(u('u_adjust'), 1, 1, 1, 0);
        gl.uniform3f(u('u_touch'), .5, .55, touch ? 1 : 0);
        gl.uniform4fv(u('u_taps[0]'), touch ? [.4, .35, .45, 1, .7, .7, 1.1, 1, 0, 0, 99, 0, 0, 0, 99, 0] : [0, 0, 99, 0, 0, 0, 99, 0, 0, 0, 99, 0, 0, 0, 99, 0]);
        const cols = def.colors && def.colors.length ? def.colors : ['#000000', '#ffffff'];
        for (let i = 0; i < 4; i++) gl.uniform3fv(u('u_color' + (i + 1)), rgb(cols[i % cols.length]));
        for (const [k, v] of Object.entries(def.parameters || {})) gl.uniform1f(u('u_' + k), v);
        gl.viewport(0, 0, c.width, c.height);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
        return { webp: c.toDataURL('image/webp', 0.82), png: c.toDataURL('image/png') };
      } catch (e) { return { error: String(e.message || e) }; }
    }, { frag, def, time, touch });
    if (res.error) { failed++; console.error(`FAIL ${id}\n${res.error}`); continue; }
    if (!touch) fs.writeFileSync(path.join(dir, def.thumbnail || 'thumbnail.webp'), Buffer.from(res.webp.split(',')[1], 'base64'));
    shots.push(res.png);
    console.log(`ok   ${id}`);
  }
  if (sheet && shots.length) {
    const cols = Math.min(6, shots.length), rows = Math.ceil(shots.length / cols), s = 0.75;
    await page.setContent(`<body style="margin:0;background:#222"><div style="display:grid;grid-template-columns:repeat(${cols},${W * s}px);gap:6px;padding:6px">` +
      shots.map(x => `<img src="${x}" width=${W * s} height=${H * s}>`).join('') + '</div></body>');
    await page.setViewportSize({ width: Math.ceil(cols * (W * s + 6) + 6), height: Math.ceil(rows * (H * s + 6) + 6) });
    await page.screenshot({ path: sheet });
  }
  await browser.close();
  process.exit(failed ? 1 : 0);
})();
