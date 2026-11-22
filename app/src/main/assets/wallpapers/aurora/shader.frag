// Aurora Waves - curtains of light rippling over a dark ridge. Taps make the curtains shiver.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time * .15;
    vec3 col = mix(u_color1 * 1.6, u_color1 * .4, uv.y);
    col += stars(p * 50.) * smoothstep(.2, .6, uv.y);
    float rp = ripples(uv);
    for (int i = 0; i < 3; i++) {
        float k = float(i);
        float x = p.x * (1.2 + k * .5) + k * 3.1 + .04 * rp;
        float base = .36 + .13 * k + .14 * noise(vec2(x * 1.2 + t * (1. + k * .3), k * 5.)) + .04 * sin(x * 2. + t * 2.);
        float h = uv.y - base;
        float rays = .5 + .5 * noise(vec2(x * 14. + t * .8, t * .7 + k));
        float body = smoothstep(0., .03, h) * exp(-h * (4.5 - k)) * rays;
        col += mix(u_color2, u_color3, smoothstep(0., .35, h)) * body * (.9 - .2 * k);
        col += u_color4 * smoothstep(0., .02, h) * exp(-h * 30.) * rays * .5;
    }
    float hill = .13 + .06 * noise(vec2(p.x * 3., 1.)) + .015 * noise(vec2(p.x * 14., 4.));
    col = mix(col, u_color1 * .15, smoothstep(hill + .003, hill, uv.y));
    gl_FragColor = vec4(dither(col), 1.);
}
