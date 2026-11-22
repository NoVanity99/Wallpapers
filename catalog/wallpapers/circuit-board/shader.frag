// Circuit Board - signals pulsing along glowing traces. Taps send a surge outward.
uniform float u_scale;
uniform float u_glow;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float ar = u_resolution.x / u_resolution.y;
    vec2 g = aspect(uv) * u_scale;
    vec2 id = floor(g), f = fract(g);
    float kind = floor(hash21(id) * 4.);
    float d;
    if (kind < 1.) d = abs(f.y - .5);
    else if (kind < 2.) d = abs(f.x - .5);
    else if (kind < 3.) {
        if (hash21(id + 5.) > .5) f.x = 1. - f.x;
        d = min(abs(length(f) - .5), abs(length(f - 1.) - .5));
    } else d = min(abs(f.x - .5), abs(f.y - .5));
    float e = u_scale * 1.5 / u_resolution.y;
    float line = smoothstep(.045 + e, .045, d);
    float node = kind >= 3. ? smoothstep(.16 + e, .16, length(f - .5)) : 0.;
    vec2 cell = (id + .5) / u_scale;
    float ph = fract(id.x * .37 + id.y * .61 + hash21(id + 2.) * .3 - u_time * .35);
    float pulse = smoothstep(0., .08, ph) * smoothstep(.45, .08, ph);
    pulse += abs(ripples(vec2(cell.x / ar, cell.y))) * 1.5;
    vec3 col = u_color1 + u_color2 * .04 * smoothstep(.5, .0, d);
    col += line * (u_color2 * .3 + u_color3 * pulse * u_glow);
    col += u_color3 * pulse * u_glow * .25 * exp(-d * 14.);
    col = mix(col, u_color4 * (.4 + .6 * pulse), node);
    gl_FragColor = vec4(dither(col), 1.);
}
