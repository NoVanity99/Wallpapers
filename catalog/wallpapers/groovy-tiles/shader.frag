// Groovy Tiles - 70s-style interlocking arcs gliding by. Taps bend the pattern.
uniform float u_scale;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float t = u_time;
    vec2 g = aspect(uv) * u_scale + vec2(t * .1, t * .07) + .12 * ripples(uv);
    vec2 id = floor(g), f = fract(g);
    if (hash21(id) > .5) f.x = 1. - f.x;
    float d = min(length(f), length(f - 1.));
    float x = abs(d - .5);
    float e = .012 * u_scale / 5.;
    float breathe = .5 + .5 * sin(t * .3 + (id.x + id.y) * .4);
    vec3 col = u_color1;
    col = mix(col, u_color2, smoothstep(.22 + e, .22 - e, x));
    col = mix(col, mix(u_color3, u_color4, breathe * .35), smoothstep(.15 + e, .15 - e, x));
    col = mix(col, mix(u_color4, u_color3, breathe * .35), smoothstep(.07 + e, .07 - e, x));
    col *= .96 + .08 * hash21(gl_FragCoord.xy * .5);
    gl_FragColor = vec4(col, 1.);
}
