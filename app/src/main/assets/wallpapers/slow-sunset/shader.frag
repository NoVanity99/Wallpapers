// Slow Sunset - the sun drifts down and back up over quiet hills, taking the sky colours with it.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float day = .5 + .5 * sin(u_time * .05);
    vec3 top = mix(u_color1, u_color2, day);
    vec3 hor = mix(u_color3, u_color4, day);
    vec3 col = mix(hor, top, smoothstep(.2, .95, uv.y));
    col += stars(p * 50.) * (1. - day) * smoothstep(.5, .9, uv.y);
    vec2 sun = vec2(ar * .5, .24 + .3 * day);
    float d = length(p - sun);
    col = mix(col, mix(u_color4, vec3(1., .97, .9), .6), smoothstep(.092, .088, d));
    col += hor * .35 * exp(-d * 5.);
    for (int i = 0; i < 3; i++) {
        float k = float(i);
        float y = .3 - .09 * k + .035 * sin(p.x * (3. + k * 1.7) + k * 2.3 + u_time * .01 * (k + 1.)) + .015 * sin(p.x * (9. + k * 3.) + k);
        vec3 hc = mix(hor, u_color1, .45 + .22 * k) * (.75 - .18 * k);
        col = mix(col, hc, smoothstep(y + .002, y - .002, uv.y));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
