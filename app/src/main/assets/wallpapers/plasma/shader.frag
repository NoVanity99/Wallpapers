// Plasma Drift - classic sine plasma. Your finger bends the field, taps send ripples.
uniform float u_scale;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y * u_scale;
    float t = u_time * .5;
    vec2 d = aspect(uv) - aspect(u_touch.xy);
    p += u_touch.z * .25 * d / (dot(d, d) + .08);
    p += .15 * ripples(uv);
    float v = sin(p.x * 3. + t) + sin(p.y * 2.5 - t * 1.2) + sin((p.x + p.y) * 2. + t * .7);
    vec2 c = p + vec2(sin(t * .6), cos(t * .45));
    v += sin(length(c) * 4. - t * 1.5);
    gl_FragColor = vec4(dither(palette(v * .18 + t * .05)), 1.);
}
