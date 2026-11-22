// Campfire Embers - sparks drifting up from a fire just out of view. Your finger stirs them.
uniform float u_amount;
uniform float u_flame;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    vec3 col = mix(u_color2, u_color1, smoothstep(0., .8, uv.y));
    float n = fbm(vec2(p.x * 5., uv.y * 4. - t * 1.5));
    float fl = smoothstep(0., 1., n * 1.3 - uv.y * (3.2 / u_flame) + .25);
    col += mix(u_color3, u_color4, fl) * fl * 1.2;
    vec2 d = p - aspect(u_touch.xy);
    vec2 q0 = p - u_touch.z * .04 * d / (dot(d, d) + .02);
    for (int i = 0; i < 4; i++) {
        float k = float(i);
        vec2 g = q0 * (10. + k * 7.);
        g.y -= t * (1. + .3 * k);
        g.x += .5 * sin(g.y * .7 + t + k * 3.) + k * 5.;
        vec2 h = hash22(floor(g) + k * 19.);
        float r = .04 + .06 * h.x;
        float dd = length(fract(g) - .5 - (h - .5) * .7);
        float flick = .6 + .4 * sin(t * 6. * h.y + h.x * 30.);
        col += u_color4 * smoothstep(r, 0., dd) * flick * step(h.y, .45 * u_amount) * (1.2 - uv.y);
    }
    gl_FragColor = vec4(dither(col), 1.);
}
