// Falling Snow - soft flakes at six depths. Your finger pushes the nearest flakes aside.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    vec3 col = mix(u_color2, u_color1, uv.y);
    vec2 d = p - aspect(u_touch.xy);
    vec2 push = u_touch.z * .03 * d / (dot(d, d) + .02);
    float hill = .09 + .03 * sin(p.x * 4. + 1.) + .015 * sin(p.x * 11.);
    for (int i = 5; i >= 0; i--) {
        float k = float(i);
        float s = 6. + k * 5.;
        vec2 g = (p - push * (1. - k / 6.)) * s;
        g.y += t * (1.2 + .15 * k);
        g.x += sin(t * .4 + k * 2. + g.y * .3) * .6 + k * 13.7;
        vec2 id = floor(g);
        vec2 h = hash22(id + k * 17.);
        float r = .05 + .09 * h.x;
        float dd = length(fract(g) - .5 - (h - .5) * .6);
        col += u_color3 * smoothstep(r, r * .3, dd) * (1. - k / 8.) * step(.35, h.y) * .9;
        if (i == 3) col = mix(col, u_color4, smoothstep(hill + .004, hill, uv.y));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
