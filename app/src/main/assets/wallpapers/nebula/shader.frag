// Drifting Nebula - layered gas clouds over a twinkling star field.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv) * 1.8;
    float t = u_time * .03;
    p.x += u_offset * .3;
    vec2 w = vec2(fbm(p * 1.2 + t), fbm(p * 1.2 + 7.3 - t));
    float n = fbm(p + 1.5 * w + vec2(t * 2., 0.));
    float n2 = fbm(p * 1.7 - 1.2 * w + 4.1);
    vec3 col = u_color1;
    col = mix(col, u_color2, smoothstep(.4, .85, n));
    col = mix(col, u_color3, smoothstep(.5, .9, n2) * .6);
    col += u_color4 * pow(smoothstep(.55, 1., n * n2 * 2.2), 2.) * .5;
    float s = stars(p * 34.) + .6 * stars(p * 70. + 11.);
    col += s * (1. - .6 * smoothstep(.4, .8, n));
    gl_FragColor = vec4(dither(col), 1.);
}
