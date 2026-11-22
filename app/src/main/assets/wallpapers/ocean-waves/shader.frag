// Ocean Waves - moonlit swell rolling in layers. Taps make the water bob.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time * .5;
    vec3 col = mix(u_color2, u_color1, smoothstep(.45, 1., uv.y));
    col += stars(p * 55.) * smoothstep(.55, .9, uv.y) * .8;
    vec2 m = vec2(ar * .68, .8);
    float d = length(p - m);
    vec3 moon = vec3(1., .96, .86);
    col += moon * (smoothstep(.052, .048, d) + .1 * exp(-d * 7.));
    float rp = ripples(uv);
    for (int i = 0; i < 6; i++) {
        float k = float(i) / 5.;
        float y = .5 - .42 * k;
        float amp = .006 + .028 * k, fr = 9. - 5. * k, sp = .6 + 1.2 * k;
        float w = y + amp * sin(p.x * fr + t * sp + k * 12.) + amp * .5 * sin(p.x * fr * 2.3 - t * sp * 1.3 + k * 5.) + .03 * k * rp;
        vec3 wc = mix(u_color3, u_color4, k);
        wc += moon * smoothstep(.014, 0., w - uv.y) * .22;
        float glit = exp(-abs(p.x - m.x) * (9. - 4. * k)) * pow(noise(vec2(p.x * 50., uv.y * 160. - t * 2.)), 3.);
        wc += moon * glit * .7 * (1. - k * .6);
        col = mix(col, wc, smoothstep(w + .002, w - .002, uv.y));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
