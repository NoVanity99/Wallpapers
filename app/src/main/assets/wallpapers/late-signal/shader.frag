// Late Night Signal - colour bars from a worn-out tape, with tracking noise rolling through.
uniform float u_noise;
uniform float u_wobble;

vec3 bars(float x) { return palette(floor(clamp(x, 0., .999) * 7.) / 7. + .1) * .75; }

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float t = u_time;
    float wob = u_wobble * .01 * sin(uv.y * 40. + t * 5.) * noise(vec2(t * .7, uv.y * 3.));
    float trk = fract(uv.y * .8 - t * .12);
    float band = smoothstep(0., .03, trk) * smoothstep(.1, .03, trk);
    wob += band * .04 * u_wobble * (hash21(vec2(floor(uv.y * 200.), floor(t * 30.))) - .3);
    float x = uv.x + wob + .03 * ripples(uv);
    vec3 col = vec3(bars(x + .004).r, bars(x).g, bars(x - .004).b);
    col *= .5 + .5 * smoothstep(.22, .26, uv.y);
    col += u_noise * (hash21(gl_FragCoord.xy * .5 + fract(t) * 100.) - .5) * .25 * (1. + 2. * band);
    col *= .9 + .1 * sin(gl_FragCoord.y * 1.2);
    vec2 v = uv - .5;
    col *= 1. - dot(v, v) * .8;
    gl_FragColor = vec4(col, 1.);
}
