// Wormhole - a spiral throat of light pulling you in. Touch to drift off-centre.
uniform float u_twist;
uniform float u_arms;

void main() {
    float ar = u_resolution.x / u_resolution.y;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    p -= (u_touch.xy - .5) * vec2(ar, 1.) * u_touch.z * .5;
    float t = u_time * .5;
    float r = length(p), a = atan(p.y, p.x);
    float z = .25 / (r + .02);
    float v = .5 + .5 * sin(a * floor(u_arms) + z * u_twist * 4. - t * 2.);
    v *= .5 + .5 * noise(vec2(z * 3. - t * 2., cos(a) * 2. + sin(a) * 2.));
    vec3 col = u_color1 + palette(z * .08 - t * .04 + .25) * pow(v, 1.5) * smoothstep(0., .25, r) * 1.4;
    col += u_color4 * exp(-r * 7.) * .9;
    gl_FragColor = vec4(dither(col), 1.);
}
