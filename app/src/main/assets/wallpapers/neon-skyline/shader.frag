// Neon Skyline - three rows of towers with windows blinking through the night.
uniform float u_windows;
uniform float u_height;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    vec3 col = mix(u_color2, u_color1, smoothstep(0., .9, uv.y));
    col += u_color3 * .25 * exp(-uv.y * 4.) + stars(p * 50.) * smoothstep(.4, .8, uv.y);
    for (int i = 0; i < 3; i++) {
        float k = float(i);
        float s = 8. + k * 6.;
        float x = p.x * s + k * 13. + u_offset * (1. + k) * .5;
        float cx = floor(x), fx = fract(x);
        float bh = (.55 - .13 * k) * (.5 + .5 * hash11(cx + k * 7.)) * u_height;
        float inside = step(uv.y, bh) * step(.06, fx) * step(fx, .94);
        vec2 wg = vec2(fx * 4., uv.y * s * 2.2);
        vec2 wid = floor(wg) + vec2(cx * 7., k * 31.);
        vec2 wf = fract(wg);
        float lit = step(1. - u_windows * .5, hash21(wid + floor(u_time * .1 + hash21(wid) * 10.)));
        float wm = step(.2, wf.x) * step(wf.x, .8) * step(.25, wf.y) * step(wf.y, .75) * lit * step(uv.y, bh - .012);
        col = mix(col, mix(u_color2 * .35, u_color1 * .35, k / 2.), inside);
        col += inside * wm * mix(u_color3, u_color4, hash21(wid + 9.)) * (.5 + .25 * k);
    }
    gl_FragColor = vec4(dither(col), 1.);
}
