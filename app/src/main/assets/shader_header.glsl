// Wallpaper Selector shader header (format 1).
// The app puts this text in front of every wallpaper's shader.frag, so a
// wallpaper only has to declare its own extra parameters and write main().
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec2 u_resolution;  // render target size in pixels
uniform float u_time;       // seconds, already multiplied by the speed settings
uniform vec3 u_touch;       // xy = finger position (0..1, origin bottom-left), z = presence (1 while touching, fades out after)
uniform vec4 u_taps[4];     // recent touch points: xy = position, z = age in seconds, w = 1 when valid
uniform float u_offset;     // home-screen page scroll, 0..1
uniform vec3 u_color1;      // "colors" from wallpaper.json
uniform vec3 u_color2;
uniform vec3 u_color3;
uniform vec3 u_color4;

float hash11(float p) { p = fract(p * .1031); p *= p + 33.33; p *= p + p; return fract(p); }
float hash21(vec2 p) { vec3 p3 = fract(vec3(p.xyx) * .1031); p3 += dot(p3, p3.yzx + 33.33); return fract((p3.x + p3.y) * p3.z); }
vec2 hash22(vec2 p) { vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973)); p3 += dot(p3, p3.yzx + 33.33); return fract((p3.xx + p3.yz) * p3.zy); }

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3. - 2. * f);
    return mix(mix(hash21(i), hash21(i + vec2(1., 0.)), f.x),
               mix(hash21(i + vec2(0., 1.)), hash21(i + vec2(1., 1.)), f.x), f.y);
}

float fbm(vec2 p) {
    float a = .5, s = 0.;
    for (int i = 0; i < 5; i++) { s += a * noise(p); p = p * 2.02 + vec2(13.1, 7.7); a *= .5; }
    return s;
}

mat2 rot(float a) { float c = cos(a), s = sin(a); return mat2(c, -s, s, c); }

// 0..1 screen coordinates -> aspect-corrected (y stays 0..1, x is scaled by width/height)
vec2 aspect(vec2 uv) { return vec2(uv.x * u_resolution.x / u_resolution.y, uv.y); }

// Blend smoothly through the four wallpaper colours, t wraps at 1
vec3 palette(float t) {
    t = fract(t) * 4.;
    vec3 a = u_color1, b = u_color2;
    if (t >= 3.) { a = u_color4; b = u_color1; }
    else if (t >= 2.) { a = u_color3; b = u_color4; }
    else if (t >= 1.) { a = u_color2; b = u_color3; }
    return mix(a, b, smoothstep(0., 1., fract(t)));
}

// Expanding ring ripples from recent taps. uv is 0..1; returns a signed wave height.
float ripples(vec2 uv) {
    float s = 0.;
    for (int i = 0; i < 4; i++) {
        vec4 t = u_taps[i];
        float d = distance(aspect(uv), aspect(t.xy));
        float r = t.z * .6;
        s += t.w * sin((d - r) * 40.) * exp(-abs(d - r) * 8.) * exp(-t.z * 1.2);
    }
    return s;
}

// One twinkling star per grid cell; scale p to choose the density (e.g. stars(p * 60.))
float stars(vec2 p) {
    vec2 id = floor(p), f = fract(p) - .5;
    vec2 h = hash22(id);
    float b = pow(hash21(id + 7.3), 6.);
    float d = length(f - (h - .5) * .7);
    float tw = .65 + .35 * sin(u_time * (1. + 3. * h.x) + h.y * 40.);
    return tw * (.25 + b) * smoothstep(.05 + .09 * b, 0., d);
}

// Tiny noise that hides colour banding in smooth gradients
vec3 dither(vec3 c) { return c + (hash21(gl_FragCoord.xy) - .5) / 255.; }

