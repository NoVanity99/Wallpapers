// Neon Grid Horizon - striped sun over an endless glowing grid. Taps ripple the floor.
void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    float t = u_time;
    float hz = -.04;
    vec3 col = mix(u_color2, u_color1, smoothstep(hz, .5, p.y));
    col += stars(aspect(uv) * 45.) * smoothstep(.1, .4, p.y);

    vec2 sc = vec2(0., hz + .19);
    float R = .17, d = length(p - sc), sy = (p.y - sc.y) / R;
    float sun = smoothstep(R, R - .004, d);
    float band = fract((p.y - hz) * 34. - t * .5);
    float cut = mix(.6, 0., smoothstep(-.95, .3, sy));
    sun *= smoothstep(cut - .04, cut + .04, band);
    col = mix(col, mix(u_color3, u_color4, smoothstep(-1., .9, sy)), sun);
    col += u_color3 * .3 * exp(-d * 5.5);

    if (p.y < hz) {
        float dz = 1. / (hz - p.y);
        vec2 g = vec2(p.x * dz * 1.6, dz * .45 + t * 1.2);
        g += .25 * ripples(uv);
        vec2 f = abs(fract(g) - .5);
        float lx = smoothstep(.015 + .01 * dz, 0., f.x);
        float lz = smoothstep(.02 + .03 * dz, 0., f.y);
        float fade = exp(-dz * .05);
        col = u_color1 * .45 + mix(u_color3, u_color2, .25) * max(lx, lz) * fade * 1.3;
        col += u_color3 * .08 * fade;
    }
    col += u_color3 * .7 * exp(-abs(p.y - hz) * 70.);
    gl_FragColor = vec4(dither(col), 1.);
}
