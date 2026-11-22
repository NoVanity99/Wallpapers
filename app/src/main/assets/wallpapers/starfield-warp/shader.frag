// Starfield Warp - fly through streaking stars. Touch to steer the vanishing point.
uniform float u_streak;

void main() {
    float ar = u_resolution.x / u_resolution.y;
    vec2 p = (gl_FragCoord.xy - .5 * u_resolution) / u_resolution.y;
    p -= (u_touch.xy - .5) * vec2(ar, 1.) * u_touch.z * .6;
    float t = u_time * .25;
    vec2 dir = normalize(p + 1e-5);
    vec2 nrm = vec2(-dir.y, dir.x);
    vec3 col = u_color1 + u_color4 * .35 * exp(-length(p) * 3.5);
    for (int i = 0; i < 18; i++) {
        float k = float(i) / 18.;
        float z = fract(k + t);
        float s = mix(18., .4, z);
        float fade = smoothstep(0., .25, z) * smoothstep(1., .85, z);
        vec2 g = p * rot(k * 40.) * s + k * 57.3;
        vec2 id = floor(g);
        vec2 h = hash22(id + k * 91.);
        vec2 q = fract(g) - .5 - (h - .5) * .5;
        vec2 dr = dir * rot(k * 40.), nr = nrm * rot(k * 40.);
        float d = length(vec2(dot(q, dr) / (1. + z * 5. * u_streak), dot(q, nr)));
        col += fade * step(.7, hash21(id + 3.1)) * smoothstep(.05, 0., d) * mix(u_color2, u_color3, h.x);
    }
    gl_FragColor = vec4(col, 1.);
}
