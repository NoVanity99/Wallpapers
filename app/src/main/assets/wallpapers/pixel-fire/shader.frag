// Pixel Fire - a chunky old-school flame. Hold a finger down to feed it.
uniform float u_pixels;
uniform float u_height;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 q = floor(aspect(uv) * u_pixels) / u_pixels;
    float t = u_time;
    float n = fbm(vec2(q.x * 6., q.y * 5. - t * 2.)) * 1.2 + .3 * noise(vec2(q.x * 14., q.y * 9. - t * 3.5));
    float v = n - q.y * (1.9 / u_height) + .35;
    v += u_touch.z * .6 * smoothstep(.25, 0., length(q - aspect(u_touch.xy)));
    v = floor(clamp(v, 0., 1.) * 6.) / 6.;
    vec3 col = mix(u_color1, u_color2, smoothstep(0., .33, v));
    col = mix(col, u_color3, smoothstep(.33, .66, v));
    col = mix(col, u_color4, smoothstep(.66, 1., v));
    gl_FragColor = vec4(col, 1.);
}
