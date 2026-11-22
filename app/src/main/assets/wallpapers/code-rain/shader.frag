// Code Rain - columns of made-up glyphs falling in the dark. Glyphs light up around your finger.
uniform float u_density;

float glyph(vec2 f, float seed) {
    vec2 g = floor(f * vec2(5., 7.));
    if (g.x < 1. || g.x > 3. || g.y < 1. || g.y > 5.) return 0.;
    return step(.45, hash21(vec2(abs(g.x - 2.) + seed * 7.13, g.y + seed * 13.7)));
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    float rows = u_density * .72;
    vec2 g = vec2(p.x * u_density, uv.y * rows);
    float cx = floor(g.x), cy = floor(g.y);
    float sp = .12 + .25 * hash11(cx + 1.);
    float len = 1.3 + hash11(cx + 5.);
    float head = fract(t * sp / len + hash11(cx + 9.)) * len;
    float fromTop = 1. - (cy + .5) / rows;
    float behind = fract((head - fromTop) / len) * len;
    float bright = exp(-behind * 3.5);
    float isHead = step(behind, 1.2 / rows);
    float ch = hash21(vec2(cx, cy));
    float seed = ch * 50. + floor(t * (.5 + 3. * ch) + ch * 10.);
    float gl = glyph(fract(g), seed);
    vec2 cell = vec2((cx + .5) / u_density, (cy + .5) / rows);
    float near = u_touch.z * smoothstep(.22, 0., length(cell - aspect(u_touch.xy)));
    float rp = ripples(vec2(cell.x * u_resolution.y / u_resolution.x, cell.y));
    near += .7 * rp * rp;
    vec3 col = u_color1 + u_color2 * .05 * bright;
    col += gl * (mix(u_color2, u_color3, isHead) * bright + u_color4 * near);
    gl_FragColor = vec4(col, 1.);
}
