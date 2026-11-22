// Rotating Crystals - three faceted gems, ray-marched. Drag sideways to tilt them.
float octa(vec3 p, float s) { p = abs(p); return (p.x + p.y * .6 + p.z - s) * .5; }

float map(vec3 p) {
    float t = u_time * .4;
    float tilt = (u_touch.x - .5) * u_touch.z;
    vec3 a = p; a.xz *= rot(t); a.xy *= rot(.3 + tilt);
    float d = octa(a, .2);
    vec3 b = p - vec3(.13, .34, 0.); b.xz *= rot(-t * 1.3); b.yz *= rot(.5 + tilt);
    d = min(d, octa(b, .09));
    vec3 c = p - vec3(-.12, -.35, 0.); c.xz *= rot(t * .8 + 1.); c.xy *= rot(-.4 - tilt);
    return min(d, octa(c, .11));
}

void main() {
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    vec3 ro = vec3(0., 0., -2.), rd = normalize(vec3(p, 2.));
    float glow = exp(-length(p) * 3.);
    vec3 col = mix(u_color1, u_color2, glow * .6);
    float z = 1.4;
    bool hit = false;
    for (int i = 0; i < 40; i++) {
        float d = map(ro + rd * z);
        if (d < .001) { hit = true; break; }
        z += d;
        if (z > 2.7) break;
    }
    if (hit) {
        vec3 q = ro + rd * z;
        vec2 e = vec2(.002, -.002);
        vec3 n = normalize(e.xyy * map(q + e.xyy) + e.yyx * map(q + e.yyx) + e.yxy * map(q + e.yxy) + e.xxx * map(q + e.xxx));
        vec3 l = normalize(vec3(.5, .8, -.6));
        float dif = max(dot(n, l), 0.);
        float spec = pow(max(dot(reflect(rd, n), l), 0.), 24.);
        float fres = pow(1. - max(dot(n, -rd), 0.), 3.);
        col = palette(dot(n, vec3(.35, .5, .25)) * .6 + u_time * .03 + .3) * (.35 + .75 * dif);
        col += spec * .8 + fres * u_color4 * .7;
    }
    gl_FragColor = vec4(dither(col), 1.);
}
