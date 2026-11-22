// Lightning Storm - rolling clouds, rain and random strikes. Tap to call a bolt down to your finger.
float bolt(vec2 p, float seed, float x0, float yEnd) {
    float y = p.y;
    float x = x0 + (noise(vec2(y * 5., seed)) - .5) * .3 * (y - yEnd + .1)
                 + (noise(vec2(y * 24., seed + 3.)) - .5) * .05;
    float d = abs(p.x - x);
    return .003 / (d + .002) * smoothstep(.9, .75, y) * smoothstep(yEnd - .01, yEnd + .02, y);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec3 col = mix(u_color2, u_color1, uv.y);
    float c = fbm(p * vec2(2., 3.5) + vec2(t * .05, 0.));
    float cm = smoothstep(.3, .7, c + (uv.y - .6) * .9);
    col = mix(col, u_color1 * .7 + .04 * c, cm);

    float cyc = t / 4., id = floor(cyc), ph = fract(cyc) * 4.;
    float env = step(.4, hash11(id + .5)) * exp(-ph * 5.) * (.6 + .4 * sin(ph * 50.));
    float b = bolt(p, id, ar * (.2 + .6 * hash11(id * 7.1)), .12) * env;
    float flash = env;
    for (int i = 0; i < 4; i++) {
        vec4 tap = u_taps[i];
        float e = tap.w * exp(-tap.z * 5.) * (.6 + .4 * sin(tap.z * 50.));
        b += bolt(p, floor(tap.x * 100.) + float(i), aspect(tap.xy).x, tap.y) * e;
        flash += e;
    }
    col += u_color3 * b;
    col += u_color4 * flash * (.12 + .5 * cm);

    float rx = p.x * 170. + uv.y * 12.;
    float rain = smoothstep(.93, 1., fract(uv.y * 2.2 + t * 4. + hash11(floor(rx)) * 10.)) * step(.5, hash11(floor(rx) + 4.));
    col += rain * (.05 + .1 * flash);
    float ridge = .1 + .04 * noise(vec2(p.x * 4., 2.)) + .012 * noise(vec2(p.x * 20., 5.));
    col = mix(col, u_color1 * .12, smoothstep(ridge + .003, ridge, uv.y));
    gl_FragColor = vec4(dither(col), 1.);
}
