// Flowing Ink - domain-warped noise that looks like ink curling through water.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv) * 2.2;
    float t = u_time * .08;
    p += .06 * ripples(uv);
    vec2 q = vec2(fbm(p + t), fbm(p + vec2(5.2, 1.3) - t));
    vec2 r = vec2(fbm(p + 3. * q + vec2(1.7, 9.2) + t * 1.5), fbm(p + 3. * q + vec2(8.3, 2.8) - t * 1.2));
    float f = fbm(p + 3. * r);
    vec3 col = mix(u_color1, u_color2, smoothstep(.2, .75, f));
    col = mix(col, u_color3, smoothstep(.45, .95, length(q)) * .7);
    col = mix(col, u_color4, smoothstep(.45, .85, r.x) * .6);
    col *= .55 + .7 * f;
    gl_FragColor = vec4(dither(col), 1.);
}
