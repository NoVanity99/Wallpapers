// Contour Lines - a slowly shifting map of thin elevation lines. Press to raise a hill, tap for ripples.
uniform float u_lines;

float field(vec2 p) {
    float t = u_time;
    vec2 d = p - aspect(u_touch.xy) * 1.6;
    return noise(p + vec2(t * .03, -t * .02)) + .5 * noise(p * 2.1 + vec2(-t * .025, t * .03) + 4.)
         + u_touch.z * .25 * exp(-dot(d, d) * 8.);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv) * 1.6;
    float e = 1.6 * 1.5 / u_resolution.y;
    float n = field(p);
    vec2 grad = vec2(field(p + vec2(e, 0.)) - n, field(p + vec2(0., e)) - n) / e;
    float v = (n + .05 * ripples(uv)) * u_lines;
    float lw = length(grad) * u_lines * e + 1e-4;
    float line = 1. - smoothstep(lw * .4, lw * 1.3, abs(fract(v) - .5));
    float major = step(mod(floor(v), 4.), .5);
    vec3 col = mix(u_color1, u_color2, uv.y);
    col = mix(col, mix(u_color3, u_color4, major), line * (.55 + .45 * major));
    gl_FragColor = vec4(dither(col), 1.);
}
