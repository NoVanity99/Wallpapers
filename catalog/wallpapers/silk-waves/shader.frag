// Silk Waves - glowing ribbons sliding over each other. Taps send a shiver through them.
uniform float u_amplitude;
uniform float u_thickness;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time * .3;
    float rp = ripples(uv);
    vec3 col = mix(u_color1 * .5, u_color1, uv.y);
    for (int i = 0; i < 7; i++) {
        float k = float(i);
        float y = .2 + .1 * k + u_amplitude * (.12 * sin(p.x * (2.5 + k * .4) + t * (1. + k * .15) + k * 1.3)
                + .05 * sin(p.x * 6.3 - t * 1.4 + k * 2.1)) + .03 * rp;
        float d = abs(uv.y - y);
        float w = u_thickness * (.03 + .02 * sin(p.x * 3. + t + k));
        vec3 c = mix(mix(u_color2, u_color3, .5 + .5 * sin(k * 1.3 + t)), u_color4, .5 + .5 * sin(k * .9 + 2.));
        col += c * (.6 * smoothstep(w, 0., d) + .25 * exp(-d * 18.)) * .6;
    }
    gl_FragColor = vec4(dither(col), 1.);
}
