// Particle Galaxy - a tilted spiral of stars and dust turning slowly.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 c = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    vec3 col = u_color1 + stars(aspect(uv) * 45.) * .5;
    vec2 p = c * rot(.6);
    p.y *= 2.1;
    p *= 2.4;
    float t = u_time * .06;
    float r = length(p);
    vec2 q = p * rot(t - r * 1.8);
    float a = atan(q.y, q.x);
    float arms = pow(.5 + .5 * cos(2. * a), 2.);
    float dust = fbm(q * 5. + 3.);
    float dens = exp(-r * 2.2) * (.25 + arms) * (.5 + dust);
    float pts = stars(q * 26.) + stars(q * 55. + 5.) + .7 * stars(q * 90. + 9.);
    col += mix(u_color3, u_color2, smoothstep(0., .9, r)) * dens * 1.1;
    col += u_color4 * dens * smoothstep(.55, .8, dust) * arms;
    col += pts * dens * 5. * smoothstep(1.3, .2, r);
    col += u_color3 * exp(-r * 9.) * 1.6 + exp(-r * 30.);
    gl_FragColor = vec4(dither(col), 1.);
}
