// Equalizer - bars dancing to music only they can hear. Bars jump under your finger.
uniform float u_bars;
uniform float u_height;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float x = uv.x * floor(u_bars);
    float id = floor(x), fx = fract(x);
    float lvl = u_height * (.08 + .5 * noise(vec2(id * .37, u_time * 1.3)) * (.6 + .4 * noise(vec2(id * 1.7, u_time * 3.))));
    lvl += u_touch.z * .25 * exp(-abs(uv.x - u_touch.x) * 8.);
    float y = uv.y - .3;
    float inBar = step(.12, fx) * step(fx, .88);
    float bar = inBar * step(0., y) * step(y, lvl) * step(.2, fract(y * 45.));
    vec3 bc = mix(u_color2, u_color3, clamp(y / .35, 0., 1.));
    bc = mix(bc, u_color4, smoothstep(.3, .45, y));
    float ry = -y;
    float refl = inBar * step(0., ry) * step(ry, lvl * .6) * step(.2, fract(ry * 45.)) * (1. - ry / (lvl * .6 + .001)) * .25;
    vec3 col = u_color1 + bc * bar + mix(u_color2, u_color3, .3) * refl + u_color2 * .15 * exp(-abs(y) * 10.);
    gl_FragColor = vec4(dither(col), 1.);
}
