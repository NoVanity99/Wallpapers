// Misty Mountains - ridges fading into drifting fog. Swiping between home screens shifts the view.
uniform float u_fog;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    vec3 col = mix(u_color2, u_color1, uv.y);
    float sd = length(p - vec2(ar * .7, .74));
    col += u_color4 * (smoothstep(.07, .064, sd) * .8 + .25 * exp(-sd * 5.));
    vec3 haze = mix(u_color2, vec3(1.), .3);
    for (int i = 0; i < 6; i++) {
        float k = float(i) / 5.;
        float x = p.x * (1.5 + 2. * k) + k * 17. + u_offset * (.2 + .5 * k) + u_time * .004 * (1. + 3. * k);
        float h = .62 - .5 * k + (.16 - .08 * k) * (fbm(vec2(x, k * 3.)) - .5) * 2.;
        vec3 mc = mix(mix(u_color2, u_color1, .5), u_color3, k);
        col = mix(col, mc, smoothstep(h + .003, h - .003, uv.y));
        float f = u_fog * .5 * smoothstep(h, h - .25, uv.y) * noise(vec2(p.x * 2. + u_time * .03 * (k + 1.), k * 7. + uv.y * 3.));
        col = mix(col, haze, f * (1. - k));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
