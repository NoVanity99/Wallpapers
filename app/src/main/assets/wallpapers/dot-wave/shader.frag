// Dot Wave - a calm grid of dots swelling as waves pass through. Dots grow under your finger.
uniform float u_density;
uniform float u_wave;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float ar = u_resolution.x / u_resolution.y;
    vec2 g = aspect(uv) * u_density;
    vec2 c = (floor(g) + .5) / u_density;
    float t = u_time;
    float w = .5 + .5 * sin(c.x * 5. * u_wave + c.y * 7. * u_wave - t * 1.5 + 2. * noise(c * 2. + t * .1));
    w += u_touch.z * smoothstep(.3, 0., length(c - aspect(u_touch.xy))) + abs(ripples(vec2(c.x / ar, c.y))) * 1.5;
    w = clamp(w, 0., 1.3);
    float r = .08 + .3 * w;
    float e = u_density * 1.5 / u_resolution.y;
    float m = smoothstep(r + e, r - e, length(fract(g) - .5));
    vec3 col = mix(mix(u_color1, u_color4, uv.y * .15), mix(u_color2, u_color3, min(w, 1.)), m);
    gl_FragColor = vec4(dither(col), 1.);
}
