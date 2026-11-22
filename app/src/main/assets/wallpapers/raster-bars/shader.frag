// Raster Bars - shaded bars weaving up and down over a scrolling star field, demo-scene style.
uniform float u_bars;
uniform float u_size;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 p = aspect(uv);
    float t = u_time;
    float n = floor(u_bars);
    vec3 col = u_color1 + stars(vec2(p.x * 30. + t * 2., p.y * 30.)) * .6;
    for (int i = 0; i < 8; i++) {
        float k = float(i);
        if (k >= n) break;
        float y = .5 + .36 * sin(t * 1.1 + k * .45) + .04 * sin(t * 2.3 + k);
        float d = (uv.y - y) / (.035 * u_size);
        if (abs(d) < 1.) {
            float sh = cos(d * 1.5708);
            float f = k / max(n - 1., 1.);
            vec3 bc = mix(mix(u_color2, u_color3, clamp(f * 2., 0., 1.)), u_color4, clamp(f * 2. - 1., 0., 1.));
            col = bc * (.25 + .75 * sh) + pow(sh, 8.) * .4;
        }
    }
    col *= .9 + .1 * step(.5, fract(gl_FragCoord.y * .5));
    gl_FragColor = vec4(col, 1.);
}
