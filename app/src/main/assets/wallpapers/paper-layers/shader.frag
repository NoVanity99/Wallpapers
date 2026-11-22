// Paper Layers - cut-paper hills stacked with soft shadows, swaying slightly.
uniform float u_layers;
uniform float u_wave;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    float n = floor(u_layers);
    float rp = ripples(uv);
    vec3 col = u_color1;
    for (int i = 0; i < 8; i++) {
        float k = float(i);
        if (k >= n) break;
        float f = k / max(n - 1., 1.);
        float h = .82 - .7 * k / n + u_wave * (.05 * sin(p.x * (2. + k * .7) + t * .2 * (1. + k * .2) + k * 2.)
                + .025 * sin(p.x * (5. + k) + k * 4. - t * .15)) + .015 * rp;
        col *= 1. - .25 * smoothstep(.03, 0., uv.y - h) * step(h, uv.y);
        vec3 lc = mix(mix(u_color2, u_color3, clamp(f * 2., 0., 1.)), u_color4, clamp(f * 2. - 1., 0., 1.));
        col = mix(col, lc, smoothstep(h + .0015, h - .0015, uv.y));
    }
    col *= .97 + .06 * hash21(gl_FragCoord.xy * .5);
    gl_FragColor = vec4(col, 1.);
}
