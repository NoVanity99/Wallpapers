// Meteor Shower - shooting stars over quiet hills. Tap to send one from your finger.
uniform float u_rate;

vec3 meteor(vec2 p, vec2 start, float life, float len) {
    if (life <= 0. || life >= 1.) return vec3(0.);
    vec2 dir = normalize(vec2(-.55, -.8));
    vec2 q = p - (start + dir * life * .9);
    float along = dot(q, -dir), across = abs(dot(q, vec2(-dir.y, dir.x)));
    float tail = smoothstep(len, 0., along) * step(0., along);
    float fade = sin(life * 3.14159);
    return (mix(u_color3, u_color4, tail) * tail * smoothstep(.0025 * (1. + tail), 0., across)
            + u_color4 * .00003 / (dot(q, q) + .00002)) * fade;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    vec3 col = mix(u_color2, u_color1, uv.y);
    col += stars(p * 55.) * smoothstep(.15, .6, uv.y) + .5 * stars(p * 90. + 4.);
    for (int i = 0; i < 8; i++) {
        float k = float(i);
        float T = u_time * (.25 + .07 * k) * u_rate + k * 3.7;
        vec2 h = hash22(vec2(floor(T), k));
        col += meteor(p, vec2(ar * (.2 + 1.1 * h.x), .6 + .5 * h.y), fract(T) * 2.5, .25 + .2 * h.y);
    }
    for (int i = 0; i < 4; i++) {
        vec4 tap = u_taps[i];
        col += tap.w * meteor(p, aspect(tap.xy), tap.z * 1.5, .3);
    }
    float hill = .1 + .05 * noise(vec2(p.x * 3., 1.)) + .015 * noise(vec2(p.x * 16., 4.));
    col = mix(col, u_color1 * .2, smoothstep(hill + .003, hill, uv.y));
    gl_FragColor = vec4(dither(col), 1.);
}
