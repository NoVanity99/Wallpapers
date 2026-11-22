// Retro Sunburst - poster-style rays turning around a rising sun. The sun leans toward your finger.
uniform float u_rays;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec2 sun = mix(vec2(ar * .5, .34), aspect(u_touch.xy), u_touch.z * .35);
    vec2 c = p - sun;
    float r = length(c), a = atan(c.y, c.x) + t * .08;
    float s = sin(a * u_rays);
    float m = smoothstep(-.012 / (r + .02), .012 / (r + .02), s);
    vec3 col = mix(u_color1, u_color2, m);
    col = mix(col, u_color2, .35 * smoothstep(.9, .1, r));
    float R = .13 + .006 * sin(t * .8);
    col = mix(col, u_color1, smoothstep(R + .062, R + .058, r));
    col = mix(col, u_color4, smoothstep(R + .032, R + .028, r));
    col = mix(col, u_color3, smoothstep(R + .002, R - .002, r));
    col *= 1. - .35 * smoothstep(.5, 1.2, length(p - vec2(ar * .5, .5)));
    col *= .95 + .1 * hash21(gl_FragCoord.xy * .5);
    gl_FragColor = vec4(col, 1.);
}
