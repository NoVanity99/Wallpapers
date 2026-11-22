// Cherry Blossom - petals tumbling on a spring breeze. Your finger blows them aside.
uniform float u_amount;
uniform float u_wind;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    vec3 col = mix(u_color2, u_color1, uv.y);
    col += .12 * smoothstep(.3, .8, noise(p * 3. + t * .05));
    vec2 d = p - aspect(u_touch.xy);
    vec2 push = u_touch.z * .04 * d / (dot(d, d) + .02);
    for (int i = 4; i >= 0; i--) {
        float k = float(i);
        vec2 g = (p - push) * (5. + k * 3.);
        g.y += t * (.6 + .1 * k);
        g.x += t * u_wind * (.3 + .1 * k) + sin(g.y * .8 + k) * .5 + k * 7.;
        vec2 h = hash22(floor(g) + k * 13.);
        vec2 q = (fract(g) - .5 - (h - .5) * .5) * rot(t * (h.x - .5) * 3. + h.y * 6.);
        float r = .16 + .1 * h.x;
        float dd = length(q * vec2(1., 2.2 + 1.2 * sin(t * 1.5 + h.x * 20.)));
        float m = smoothstep(r, r * .75, dd) * step(h.y, .5 * u_amount);
        col = mix(col, mix(u_color3, u_color4, h.x), m * (1. - k * .12));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
