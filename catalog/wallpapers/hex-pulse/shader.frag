// Pulsing Hex Grid - waves of light travel across a honeycomb. Taps send bright ripples through the cells.
uniform float u_scale;

float hexDist(vec2 p) { p = abs(p); return max(dot(p, vec2(.5, .866)), p.x); }

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float ar = u_resolution.x / u_resolution.y;
    vec2 p = aspect(uv) * u_scale;
    vec2 r = vec2(1., 1.732), h = r * .5;
    vec2 a = mod(p, r) - h, b = mod(p - h, r) - h;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    vec2 cell = (p - g) / u_scale;
    float e = hexDist(g);
    float t = u_time;
    float pulse = .5 + .5 * sin(length(cell - vec2(ar * .5, .5)) * 9. - t * 1.6 + hash21(floor((p - g) * 10.)) * 1.5);
    pulse = .12 + .88 * pulse * pulse;
    float rp = abs(ripples(vec2(cell.x / ar, cell.y)));
    rp += u_touch.z * smoothstep(.2, 0., length(cell - aspect(u_touch.xy)));
    float line = smoothstep(.4, .5, e);
    float fill = smoothstep(.46, .2, e);
    vec3 col = u_color1;
    col += u_color2 * line * (.12 + .88 * pulse);
    col += u_color3 * fill * pulse * .3;
    col += u_color4 * rp * (line + .45);
    gl_FragColor = vec4(dither(col), 1.);
}
