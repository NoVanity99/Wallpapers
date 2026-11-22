// Retro Stripes - two bands of 70s racing stripes snaking across the screen.
uniform float u_width;
uniform float u_curve;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    float rp = ripples(uv);
    float sw = .05 * u_width;
    float e = 1.5 / (u_resolution.y * sw);
    vec3 col = u_color1;
    for (int j = 0; j < 2; j++) {
        float k = float(j);
        float c = .3 + .4 * k + u_curve * (.12 * sin(p.x * 2.5 + t * .4 + k * 2.) + .05 * sin(p.x * 6. - t * .3)) + .03 * rp;
        float d = (uv.y - c) / sw;
        vec3 sc = mix(mix(u_color2, u_color3, smoothstep(-.5 - e, -.5 + e, d)), u_color4, smoothstep(.5 - e, .5 + e, d));
        col = mix(col, sc, smoothstep(1.5 + e, 1.5 - e, abs(d)));
    }
    col *= .95 + .1 * hash21(gl_FragCoord.xy * .5);
    gl_FragColor = vec4(col, 1.);
}
