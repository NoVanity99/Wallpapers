// Rain on Glass - drops slide down a fogged window in front of city lights. Wipe the fog with your finger.
vec3 lights(vec2 p, float blur) {
    vec3 c = mix(u_color1, u_color2, p.y);
    for (int i = 0; i < 2; i++) {
        float s = 3. + float(i) * 2.5;
        vec2 g = p * s + float(i) * 7.3;
        vec2 id = floor(g);
        vec2 h = hash22(id + 3.);
        float d = length(fract(g) - .5 - (h - .5) * .4);
        float r = .12 + .12 * h.x;
        vec3 lc = mix(u_color3, u_color4, hash21(id + 9.));
        c += lc * smoothstep(r + blur, r - blur, d) * .4 * step(.4, h.y) * (.7 + .3 * sin(u_time * .5 + h.x * 30.));
    }
    return c;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    vec2 n = vec2(0.);
    float wet = 0.;
    // small beads that sit still and slowly evaporate
    {
        vec2 g = p * 26.;
        vec2 h = hash22(floor(g));
        float life = fract(t * .05 * (.5 + h.x) + h.y);
        float r = .34 * h.x * smoothstep(1., .7, life) * smoothstep(0., .1, life);
        vec2 q = fract(g) - .5 - (h - .5) * .4;
        float m = smoothstep(r, r * .5, length(q));
        n += q * m * .04;
        wet += m;
    }
    // drops that run down the glass and leave a trail
    for (int i = 0; i < 2; i++) {
        float k = float(i);
        float cols = 7. + k * 5., rows = 1.6 + k;
        vec2 g = vec2(p.x * cols + k * 3.7, uv.y * rows);
        float cx = floor(g.x);
        float hx = hash11(cx + k * 31.);
        g.y += t * (.12 + .2 * hx) + hx * 10.;
        float cy = floor(g.y);
        vec2 f = vec2(fract(g.x) - .5, fract(g.y));
        float h2 = hash21(vec2(cx, cy) + k * 5.);
        float x0 = (h2 - .5) * .3 + .04 * sin(g.y * 6. + hx * 20.);
        float r = .17;
        vec2 q = vec2(f.x - x0, (f.y - .25) * cols / rows);
        float act = step(.3, h2);
        float m = smoothstep(r, r * .5, length(q * vec2(1., .8)));
        float tr = smoothstep(r * .7, r * .2, abs(f.x - x0)) * smoothstep(.25, .3, f.y) * smoothstep(1., .4, f.y);
        n += act * q * m * .12;
        wet += act * (m + tr * .55);
    }
    wet += u_touch.z * smoothstep(.13, .05, length(p - aspect(u_touch.xy)));
    wet = clamp(wet, 0., 1.);
    vec3 fog = lights(p, .1) * .85 + .05;
    vec3 clear = lights(p - n * 6., .025) * 1.25;
    vec3 col = mix(fog, clear, wet);
    col += .3 * smoothstep(0., .025, n.y - n.x);
    gl_FragColor = vec4(dither(col), 1.);
}
