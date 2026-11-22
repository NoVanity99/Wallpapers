// Glitch - a clean signal that keeps tearing, splitting and snapping back.
uniform float u_intensity;

vec3 scene(vec2 uv) {
    vec2 p = aspect(uv);
    float t = u_time * .2;
    float v = sin(p.x * 4. + t) + sin(p.y * 6. - t * 1.3) + sin((p.x - p.y) * 5. + t * .7);
    return palette(v * .12 + uv.y * .4 + t * .1) * .8 + u_color4 * .15 * step(.96, fract(p.y * 14. + t));
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float t = u_time;
    float band = floor(uv.y * 18. + floor(t * 3.) * 7.);
    float g = step(1. - .18 * u_intensity, hash21(vec2(band, floor(t * 6.))));
    float sh = (hash21(vec2(band, floor(t * 12.))) - .5) * .25 * g * u_intensity + .05 * ripples(uv);
    float sp = .006 + .02 * g * u_intensity;
    vec3 col = vec3(scene(uv + vec2(sh + sp, 0.)).r, scene(uv + vec2(sh, 0.)).g, scene(uv + vec2(sh - sp, 0.)).b);
    col *= .85 + .15 * sin(gl_FragCoord.y * 1.5);
    float bl = step(1. - .03 * u_intensity, hash21(floor(uv * vec2(6., 12.)) + floor(t * 5.)));
    col = mix(col, u_color4, bl * .6);
    col += (hash21(gl_FragCoord.xy + fract(t) * 50.) - .5) * .06;
    gl_FragColor = vec4(col, 1.);
}
