// Planet Horizon - sunrise creeping around the edge of a world, seen from orbit.
uniform float u_atmosphere;
uniform float u_clouds;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    vec2 c = vec2(ar * .5, -1.2);
    float R = 1.52;
    float d = length(p - c) - R;
    vec3 col = u_color1 + stars(p * 50. + u_offset) * smoothstep(0., .1, d);
    float sa = .3 * sin(u_time * .05);
    vec2 sun = c + R * vec2(sin(sa), cos(sa));
    float sd = length(p - sun);
    col += u_color4 * .02 / (sd + .02) * smoothstep(-.02, 0., d);
    col += u_color3 * u_atmosphere * exp(-max(d, 0.) * 28.) * (.35 + .9 * exp(-sd * 3.));
    vec2 s = (p - c) / R;
    float n = fbm(s * 14. + vec2(u_time * .01, 0.));
    vec3 sc = mix(u_color2 * .25, u_color2, n) * (.15 + .8 * exp(-sd * 2.5));
    sc += u_clouds * smoothstep(.5, .8, fbm(s * 22. + vec2(u_time * .02, 3.))) * .3 * exp(-sd * 2.);
    sc += u_color3 * exp(d * 30.) * .6 * u_atmosphere;
    col = mix(col, sc, smoothstep(.002, -.002, d));
    gl_FragColor = vec4(dither(col), 1.);
}
