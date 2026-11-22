// Floating Shapes - flat circles, squares, triangles and rings drifting upward. They shy away from your finger.
float shape(vec2 q, float kind, float r) {
    if (kind < 1.) return length(q) - r;
    if (kind < 2.) { vec2 d = abs(q) - r * .8; return length(max(d, 0.)) + min(max(d.x, d.y), 0.); }
    if (kind < 3.) {
        const float k = 1.732;
        q.x = abs(q.x) - r; q.y = q.y + r / k;
        if (q.x + k * q.y > 0.) q = vec2(q.x - k * q.y, -k * q.x - q.y) / 2.;
        q.x -= clamp(q.x, -2. * r, 0.);
        return -length(q) * sign(q.y);
    }
    return abs(length(q) - r) - r * .22;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec3 col = mix(u_color1 * .92, u_color1, uv.y);
    vec2 tp = aspect(u_touch.xy);
    for (int i = 0; i < 10; i++) {
        float k = float(i);
        vec2 h = hash22(vec2(k, 3.));
        vec2 c = vec2(ar * (h.x + .1 * sin(t * .2 + k * 2.)), fract(h.y + t * .02 * (.5 + h.x)) * 1.4 - .2);
        vec2 away = c - tp;
        c += u_touch.z * .12 * normalize(away + 1e-4) * smoothstep(.4, 0., length(away));
        float ang = t * .3 * (h.x - .5) + k;
        float r = .04 + .05 * h.y;
        float kind = floor(hash11(k + 7.) * 4.);
        float d = shape((p - c) * rot(ang), kind, r);
        float sh = shape((p - c - vec2(.012, -.014)) * rot(ang), kind, r);
        col *= 1. - .16 * smoothstep(.02, -.005, sh);
        float pick = hash11(k + 11.);
        vec3 sc = pick < .34 ? u_color2 : (pick < .67 ? u_color3 : u_color4);
        col = mix(col, sc, smoothstep(.002, -.002, d));
    }
    gl_FragColor = vec4(dither(col), 1.);
}
