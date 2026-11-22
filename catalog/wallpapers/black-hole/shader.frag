// Black Hole - a glowing disc of gas circling a point of nothing, bending the stars behind it.
uniform float u_size;
uniform float u_glow;

void main() {
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    float t = u_time * .3;
    float R = .09 * u_size;
    float r = length(p);
    vec2 dir = p / (r + 1e-4);
    vec2 sp = p - dir * R * R * 1.8 / (r + .02);
    vec3 col = u_color1 + stars(sp * 40. + 3.) * .9 + .6 * stars(sp * 80.);
    vec2 q = p * rot(.3);
    q.y *= 3.;
    float dr = length(q);
    float sw = fbm(rot(t * .8 - dr * 6.) * q * 9.);
    float band = smoothstep(R * 1.3, R * 1.8, dr) * smoothstep(R * 5., R * 2.2, dr);
    vec3 disc = mix(u_color3, u_color2, smoothstep(R * 1.5, R * 4.5, dr)) * (.4 + 1.2 * sw) * band;
    float hole = smoothstep(R * 1.02, R * .97, r);
    float front = step(q.y, 0.) * band;
    col += disc;
    col = mix(col, disc * front, hole);
    col += u_color4 * u_glow * .012 / (abs(r - R * 1.08) + .004) * (1. - front);
    gl_FragColor = vec4(dither(col), 1.);
}
