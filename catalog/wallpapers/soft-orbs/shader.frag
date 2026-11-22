// Soft Orbs - a few big blurred circles drifting past each other. One follows your finger.
uniform float u_size;
uniform float u_blur;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec2 tp = aspect(u_touch.xy);
    vec3 col = u_color1;
    for (int i = 0; i < 5; i++) {
        float k = float(i);
        vec2 c = vec2(ar * (.5 + .45 * sin(t * (.11 + .03 * k) + k * 2.1)), .5 + .45 * cos(t * (.09 + .025 * k) + k * 1.3));
        c = mix(c, tp, u_touch.z * (i == 0 ? .9 : .15));
        float r = u_size * (.2 + .06 * sin(k * 3.));
        float m = smoothstep(r, r * (1. - u_blur), length(p - c));
        float pick = mod(k, 3.);
        vec3 oc = pick < .5 ? u_color2 : (pick < 1.5 ? u_color3 : u_color4);
        col = mix(col, oc, m * .75);
    }
    gl_FragColor = vec4(dither(col), 1.);
}
