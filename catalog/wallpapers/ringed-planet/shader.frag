// Ringed Planet - a banded gas giant and its rings, slowly rocking. Drag sideways to tilt it.
uniform float u_tilt;
uniform float u_ringSize;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    vec3 col = u_color1 + stars(aspect(uv) * 45.) * .8;
    p *= rot(u_tilt * .5 + .05 * sin(u_time * .1) + (u_touch.x - .5) * u_touch.z);
    float R = .16;
    float rr = length(vec2(p.x, p.y / .28)) / R;
    float ring = smoothstep(1.35, 1.4, rr) * smoothstep(2.3 * u_ringSize, 2.2 * u_ringSize, rr);
    ring *= (.6 + .4 * sin(rr * 40.) * sin(rr * 13. + 1.)) * (1. - .7 * smoothstep(1.74, 1.78, rr) * smoothstep(1.9, 1.86, rr));
    vec3 rc = mix(u_color3, u_color4, fract(rr * 1.5));
    float pm = smoothstep(R, R - .003, length(p));
    vec3 n = vec3(p / R, sqrt(max(0., 1. - dot(p, p) / (R * R))));
    float dif = max(dot(n, normalize(vec3(-.6, .4, .7))), 0.);
    float lat = n.y * 8. + .6 * noise(vec2(n.x * 3. + u_time * .05, n.y * 12.));
    vec3 pc = mix(u_color2, u_color3, .5 + .5 * sin(lat * 2.)) * (.06 + dif);
    col = mix(col, rc, ring * step(0., p.y));
    col = mix(col, pc, pm);
    col = mix(col, rc, ring * step(p.y, 0.));
    gl_FragColor = vec4(dither(col), 1.);
}
