// Checker Corridor - an endless checkerboard floor and ceiling rushing toward a glowing horizon.
uniform float u_tiles;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    float y = abs(p.y) + .001;
    float dz = 1. / y;
    vec2 g = vec2(p.x * dz, dz + u_time * 1.5) * u_tiles * .25 + .3 * ripples(uv);
    float ch = mod(floor(g.x) + floor(g.y), 2.);
    vec3 c = mix(u_color1, p.y < 0. ? u_color2 : u_color3, ch);
    vec3 col = mix(u_color4, c, exp(-dz * .07));
    gl_FragColor = vec4(dither(col), 1.);
}
