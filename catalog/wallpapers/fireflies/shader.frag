// Fireflies - glowing dots wandering over a night meadow. Hold a finger down and they gather around it.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec3 col = mix(u_color1, u_color2, pow(uv.y, 1.5));
    col += stars(p * 55.) * .5 * smoothstep(.5, .9, uv.y);
    float grass = .1 + .05 * noise(vec2(p.x * 5., 3.)) + .035 * noise(vec2(p.x * 45. + sin(t * .5 + p.x * 6.) * .4, 7.));
    col = mix(col, u_color1 * .25, smoothstep(grass + .004, grass, uv.y));
    vec2 tp = aspect(u_touch.xy);
    for (int i = 0; i < 26; i++) {
        float k = float(i);
        vec2 h = hash22(vec2(k, k * 1.7 + 2.));
        vec2 c = vec2(ar * (h.x + .12 * sin(t * .23 * (1. + h.y) + k) + .05 * sin(t * .61 + k * 3.)),
                      .1 + .75 * h.y + .08 * sin(t * .31 * (1. + h.x) + k * 2.) + .03 * cos(t * .9 + k));
        vec2 o = .07 * vec2(sin(k * 2.4 + t * 1.3), cos(k * 1.9 + t * 1.1));
        c = mix(c, tp + o, u_touch.z * .85);
        float blink = smoothstep(.2, 1., .5 + .5 * sin(t * (.6 + h.x) + k * 4.));
        float d = length(p - c);
        col += blink * (u_color3 * smoothstep(.007, 0., d) + u_color4 * .0004 / (d * d + .0008));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
