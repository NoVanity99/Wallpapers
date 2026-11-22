// Neon Tunnel - glowing hexagonal rings rushing past. Touch to lean into the turn.
void main() {
    float ar = u_resolution.x / u_resolution.y;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    float t = u_time;
    p -= (u_touch.xy - .5) * vec2(ar, 1.) * u_touch.z * .5;
    p += .03 * vec2(sin(t * .4), cos(t * .31));
    float seg = 6.2832 / 6.;
    float a = atan(p.y, p.x) + t * .08;
    float rr = length(p) * cos(mod(a, seg) - seg * .5);
    float z = .3 / (rr + .001);
    float w = abs(fract(z * 1.2 - t * .9) - .5);
    float ring = .035 / (w + .035) - .065;
    float sw = abs(mod(a, seg) - seg * .5);
    float spoke = smoothstep(.5, .523, sw) * .6;
    float fog = smoothstep(0., .22, rr);
    vec3 col = u_color1 + palette(z * .12 + t * .04) * (ring * 1.3 + spoke) * fog;
    col += u_color4 * .15 * exp(-rr * 9.);
    gl_FragColor = vec4(dither(col), 1.);
}
