// Breathing Rings - circles widening and thinning like slow breath. The centre follows your finger.
uniform float u_rings;
uniform float u_thickness;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float ar = u_resolution.x / u_resolution.y;
    float t = u_time;
    vec2 c = mix(vec2(ar * .5, .5), aspect(u_touch.xy), u_touch.z * .6);
    float r = length(p - c);
    float breath = .5 + .5 * sin(t * .6);
    float f = abs(fract(r * u_rings - t * .25) - .5);
    float w = u_thickness * (.08 + .12 * breath) * (1. - .6 * smoothstep(.2, .9, r));
    float e = u_rings * 1.5 / u_resolution.y;
    float m = smoothstep(w + e, w - e, f) * smoothstep(1., .3, r);
    vec3 col = mix(mix(u_color1, u_color2, min(r, 1.)), mix(u_color3, u_color4, smoothstep(0., .8, r)), m);
    gl_FragColor = vec4(dither(col), 1.);
}
