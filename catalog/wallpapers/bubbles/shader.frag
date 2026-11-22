// Bubbles - glassy bubbles rising through deep water. Your finger nudges them, taps make them wobble.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    vec3 col = mix(u_color1, u_color2, uv.y);
    col += u_color2 * .25 * pow(noise(vec2(p.x * 6. + uv.y * 2. + t * .1, t * .15)), 2.) * uv.y;
    vec2 d = p - aspect(u_touch.xy);
    vec2 q0 = p - u_touch.z * .03 * d / (dot(d, d) + .02) + .012 * ripples(uv);
    for (int i = 0; i < 5; i++) {
        float k = float(i);
        float s = 3.5 + k * 2.2;
        vec2 g = q0 * s;
        g.y -= t * (.5 + .12 * k);
        g.x += .25 * sin(g.y * 1.3 + t * .8 + k * 4.) + k * 9.1;
        vec2 h = hash22(floor(g) + k * 23.);
        float r = .12 + .2 * h.x;
        vec2 q = fract(g) - .5 - (h - .5) * (.9 - 2. * r);
        float dd = length(q);
        float rim = smoothstep(r, r - .03, dd) * smoothstep(r - .14, r, dd);
        float hl = smoothstep(r * .3, 0., length(q - vec2(-.38, .38) * r));
        float body = smoothstep(r, r - .03, dd);
        col += (u_color3 * (rim * .5 + body * .06) + u_color4 * hl * .8) * step(.45, h.y) * (1. - k * .13);
    }
    gl_FragColor = vec4(dither(col), 1.);
}
