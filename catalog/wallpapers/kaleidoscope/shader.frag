// Kaleidoscope - a mirrored pattern that never repeats.
uniform float u_segments;
uniform float u_zoom;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    float t = u_time * .2;
    float a = atan(p.y, p.x) + t * .3, r = length(p);
    float seg = 6.2832 / floor(u_segments);
    a = abs(mod(a, seg) - seg * .5);
    vec2 q = vec2(cos(a), sin(a)) * r * u_zoom + .1 * ripples(uv);
    float v = sin(q.x * 8. + t * 2.) + sin(q.y * 10. - t * 1.5) + sin((q.x + q.y) * 6. + t) + 2. * noise(q * 4. + t);
    vec3 col = palette(v * .12 + r * .5 - t * .2) * (.7 + .3 * sin(v * 3.));
    gl_FragColor = vec4(dither(col), 1.);
}
