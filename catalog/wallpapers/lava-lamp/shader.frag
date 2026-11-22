// Lava Lamp - soft blobs that merge and split. Touch the screen to add a blob under your finger.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time * .25;
    float f = 0.;
    for (int i = 0; i < 8; i++) {
        float k = float(i);
        vec2 c = vec2(ar * (.5 + .38 * sin(t * (.5 + .13 * k) + k * 2.4)),
                      .5 + .52 * sin(t * (.37 + .09 * k) + k * 1.7));
        float r = .07 + .035 * hash11(k + 3.);
        f += r * r / dot(p - c, p - c);
    }
    vec2 d = p - aspect(u_touch.xy);
    f += u_touch.z * .012 / (dot(d, d) + .001);
    f += .006 / ((uv.y + .03) * (uv.y + .03)) + .006 / ((1.03 - uv.y) * (1.03 - uv.y));
    vec3 bg = mix(u_color1, u_color2, uv.y) + u_color3 * .3 * smoothstep(.35, 1., f);
    vec3 blob = mix(u_color3, u_color4, clamp(uv.y + .25 * (f - 1.), 0., 1.));
    gl_FragColor = vec4(dither(mix(bg, blob, smoothstep(.97, 1.03, f))), 1.);
}
