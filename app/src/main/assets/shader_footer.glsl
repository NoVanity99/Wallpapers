
#undef main
uniform vec4 u_adjust; // brightness, contrast, saturation, vignette (per-wallpaper user settings)

void main() {
    wallpaperMain();
    vec3 c = gl_FragColor.rgb;
    c = (c - .5) * u_adjust.y + .5;
    c = mix(vec3(dot(c, vec3(.299, .587, .114))), c, u_adjust.z) * u_adjust.x;
    vec2 v = gl_FragCoord.xy / u_resolution - .5;
    c *= 1. - u_adjust.w * dot(v, v) * 2.;
    gl_FragColor = vec4(clamp(c, 0., 1.), 1.);
}
