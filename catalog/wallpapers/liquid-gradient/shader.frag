// Liquid Gradient - four colours slowly folding into each other.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv) * 2.4;
    float t = u_time * .12;
    p += .03 * ripples(uv);
    vec2 q = vec2(noise(p + vec2(t, -t * .7)), noise(p * 1.3 + vec2(-t * .8, t) + 5.2));
    vec2 r = p + 1.6 * q;
    float a = noise(r + t), b = noise(r * .8 - t + 9.1);
    vec3 col = mix(u_color1, u_color2, smoothstep(.2, .8, a));
    col = mix(col, u_color3, smoothstep(.3, .9, b) * .8);
    col = mix(col, u_color4, smoothstep(.55, 1.2, q.x * q.y * 2.2) * .7);
    gl_FragColor = vec4(dither(col), 1.);
}
