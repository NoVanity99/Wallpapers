// Data Streams - packets of light racing along parallel lanes.
uniform float u_lanes;
uniform float u_rush;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float y = uv.y * floor(u_lanes);
    float id = floor(y), fy = abs(fract(y) - .5);
    float h = hash11(id + 1.);
    float dir = h > .5 ? 1. : -1.;
    float x = p.x * (1. + 2. * h) - dir * u_time * (.3 + 1.2 * hash11(id + 7.)) * u_rush + h * 30.;
    float on = step(.55, hash21(vec2(floor(x), id)));
    float tail = dir > 0. ? fract(x) : 1. - fract(x);
    float line = smoothstep(.12, 0., fy) + .3 * smoothstep(.5, 0., fy);
    float near = 1. + 2. * u_touch.z * smoothstep(.25, 0., abs(uv.y - u_touch.y));
    vec3 col = u_color1 + mix(u_color2, u_color3, hash11(id + 3.)) * pow(tail, 4.) * on * line * near;
    col += u_color4 * on * smoothstep(.97, 1., tail) * smoothstep(.2, 0., fy);
    gl_FragColor = vec4(dither(col), 1.);
}
