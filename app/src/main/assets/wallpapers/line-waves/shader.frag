// Line Waves - a stack of thin lines with a gentle swell travelling through them.
uniform float u_lines;
uniform float u_amplitude;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    float n = floor(u_lines);
    float rp = ripples(uv);
    float e = 1.5 / u_resolution.y;
    vec3 col = mix(u_color1, u_color2, uv.y);
    for (int i = 0; i < 24; i++) {
        float k = float(i);
        if (k >= n) break;
        float env = exp(-pow((uv.x - .5 - .2 * sin(t * .3 + k * .4)) * 3., 2.));
        float y = (k + 1.) / (n + 1.) + u_amplitude * (.05 * env * sin(p.x * 14. + t * 1.5 + k * .7) + .02 * sin(p.x * 5. - t + k)) + .02 * rp;
        col = mix(col, mix(u_color3, u_color4, k / n), smoothstep(.0015 + e, .0015, abs(uv.y - y)));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
