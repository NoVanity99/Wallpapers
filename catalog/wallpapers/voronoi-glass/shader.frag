// Stained Glass - shifting cells of colour with dark leading between them.
uniform float u_scale;
uniform float u_edge;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv) * u_scale + .1 * ripples(uv);
    float t = u_time * .4;
    vec2 g = floor(p), f = fract(p);
    float d1 = 8., d2 = 8.;
    vec2 id = vec2(0.);
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 o = vec2(float(i), float(j));
            vec2 h = hash22(g + o);
            float d = length(o + .5 + .4 * sin(t + 6.2831 * h) - f);
            if (d < d1) { d2 = d1; d1 = d; id = g + o; } else if (d < d2) { d2 = d; }
        }
    }
    float e = smoothstep(0., u_edge, d2 - d1);
    vec3 col = palette(hash21(id) + t * .03) * (.55 + .45 * e) * (1. - .4 * d1);
    col = mix(u_color1 * .2, col, e);
    col += u_touch.z * .35 * smoothstep(.3, 0., length(aspect(uv) - aspect(u_touch.xy)));
    gl_FragColor = vec4(dither(col), 1.);
}
