# Wallpaper Selector

An Android app for browsing, downloading, previewing and setting animated (live) wallpapers.
Every wallpaper is a small OpenGL ES shader, so it stays a few kilobytes, loops forever and is sharp at any resolution.

- 24 original wallpapers in six categories (Abstract, Space, Nature, Neon/Cyber, Minimal, Retro); 21 of them react to touch
- Live previews, search, category filters, offline downloads, a "My Wallpapers" tab
- Home screen, lock screen or both (separate lock screen wallpapers need Android 14+)
- Speed, 30/60 fps cap, battery saver, light/dark theme
- New wallpapers come from the catalog in this repository, with no app update needed
- No ads, no tracking, one permission (internet)

Kotlin, Jetpack Compose, Material 3. Android 8.0 (API 26) and newer.

## Install it on your phone

1. On your phone, open **https://github.com/NoVanity99/Wallpapers/releases/latest**
2. Under **Assets**, tap **WallpaperSelector.apk**.
3. Open the downloaded file. The first time, Android asks you to allow your browser to "install unknown apps": allow it, go back, and tap **Install**.
4. Open **Wallpaper Selector**.

To update later, download the newest APK the same way and install it over the old one. Your downloads and settings are kept.

> While this repository is **private**, you have to be signed in to GitHub in your phone's browser to download the APK, and the app cannot read the online catalog (it shows only the three built-in wallpapers). See [Hosting](#hosting-the-catalog).

## Using the app

| | |
|---|---|
| **Browse** | All wallpapers grouped by category. Pull down to refresh the catalog. |
| **A wallpaper** | Full-screen live preview. Touch the preview to play with it; the eye button hides the panel. **Download** saves it for offline use, **Set as Wallpaper** asks Home / Lock / Both and then opens Android's own preview, where you tap **Set wallpaper**. |
| **My Wallpapers** | Everything stored on the phone (built-in and downloaded), with delete. |
| **Settings** | Animation speed, frame rate limit, battery saver, live previews in lists, theme. |

The wallpaper only draws while it is actually visible. It stops when the screen is off or an app covers it. With battery saver on (the default) it also freezes at 15% battery or when Android's Battery Saver is active.

## Add a new wallpaper

A wallpaper is a folder in `catalog/wallpapers/` with three files:

```
catalog/wallpapers/my-wallpaper/
├── wallpaper.json    the definition
├── shader.frag       the animation (GLSL ES fragment shader)
└── thumbnail.webp    preview image, 360×640 (png or jpg also work)
```

1. **Copy an existing folder** that is close to what you want, for example `catalog/wallpapers/plasma`, and rename it. The folder name is the wallpaper's id (letters, digits, `-`, `_`).

2. **Edit `wallpaper.json`:**

   ```json
   {
     "id": "my-wallpaper",
     "name": "My Wallpaper",
     "category": "Abstract",
     "description": "One line shown on the detail screen.",
     "version": 1,
     "format": 1,
     "shader": "shader.frag",
     "thumbnail": "thumbnail.webp",
     "interactive": true,
     "colors": ["#0b1354", "#7b2ff7", "#f72585", "#ffb703"],
     "speed": 1.0,
     "renderScale": 1.0,
     "parameters": { "scale": 1.6 }
   }
   ```

   | Field | Meaning |
   |---|---|
   | `id` | Must equal the folder name. |
   | `category` | One of `Abstract`, `Space`, `Nature`, `Neon/Cyber`, `Minimal`, `Retro`. |
   | `version` | Raise it when you change a wallpaper; phones that already downloaded it get an **Update** button. |
   | `interactive` | Shows the "reacts to touch" badge. |
   | `colors` | Up to four colours, available in the shader as `u_color1` … `u_color4`. |
   | `speed` | Time multiplier for this wallpaper (the user's speed setting is applied on top). |
   | `renderScale` | 0.25–1. Heavy shaders can render at a lower resolution and be scaled up; 0.5–0.75 is usually invisible on soft, blurry designs and saves a lot of battery. |
   | `parameters` | Any numbers you like. `"scale": 1.6` becomes `uniform float u_scale;` in the shader. |

3. **Write `shader.frag`.** The app adds a header in front of your file (see `app/src/main/assets/shader_header.glsl`), so you only write `main()` and whatever helpers you need. The header gives you:

   | Name | What it is |
   |---|---|
   | `u_resolution` | Size of the drawing area in pixels |
   | `u_time` | Seconds, already scaled by the speed settings |
   | `u_touch` | `xy` finger position (0–1, origin bottom-left), `z` is 1 while touching and fades out after |
   | `u_taps[4]` | The last four touch points: `xy` position, `z` age in seconds, `w` 1 if valid |
   | `u_offset` | Home screen page scroll, 0–1 |
   | `u_color1` … `u_color4` | Your `colors` |
   | `hash11`, `hash21`, `hash22`, `noise`, `fbm`, `rot` | Random numbers, smooth noise, rotation |
   | `aspect(uv)`, `palette(t)`, `ripples(uv)`, `stars(p)`, `dither(col)` | Aspect-correct coordinates, a blend through your four colours, ready-made tap ripples, a star field, anti-banding |

   A complete wallpaper can be this short:

   ```glsl
   void main() {
       vec2 uv = gl_FragCoord.xy / u_resolution;
       float wave = sin(uv.y * 8. + u_time + ripples(uv) * 3.);
       gl_FragColor = vec4(dither(palette(uv.x * .3 + wave * .1)), 1.);
   }
   ```

   Keep to GLSL ES 1.00 (the WebGL 1 dialect): loops need a fixed number of iterations.

4. **Make the thumbnail.** With Node.js installed:

   ```
   npm install playwright && npx playwright install chromium   # once
   node tools/render_thumbnails.js my-wallpaper
   ```

   This also tells you if the shader has a compile error. Alternatively save any 360×640 image as `thumbnail.webp`.

5. **Rebuild the catalog and push:**

   ```
   python3 tools/build_catalog.py
   git add catalog app/src/main/assets
   git commit -m "Add My Wallpaper"
   git push
   ```

The app picks up the new entry the next time it starts or you pull to refresh (GitHub's raw file cache can take about five minutes). No new APK is needed.

To change which three wallpapers ship inside the APK, edit `BUNDLED` in `tools/build_catalog.py`. That one does need a new build.

### Try a wallpaper before publishing it

Serve the repository from your computer and point a debug build at it:

```
python3 -m http.server 8000                # in the repository root
./gradlew installDebug -PcatalogBaseUrl=http://10.0.2.2:8000/catalog/
```

`10.0.2.2` is your computer as seen from the Android emulator; for a real phone on the same Wi-Fi use your computer's IP address.

## Hosting the catalog

The app reads `catalog.json` and the wallpaper files from

```
https://raw.githubusercontent.com/NoVanity99/Wallpapers/HEAD/catalog/
```

(`HEAD` means the repository's default branch.) Raw URLs only work without a login when the repository is **public**: *Settings → General → Danger Zone → Change repository visibility*. Until then the app works with its three built-in wallpapers and shows a "can't reach the catalog" notice.

To host the catalog somewhere else (GitHub Pages, another repository, your own server), upload the `catalog/` folder there and build with `-PcatalogBaseUrl=https://example.com/catalog/`, or change the default in `app/build.gradle.kts`.

## Build it yourself

You need JDK 17 or newer and the Android SDK (installing Android Studio gives you both).

```
git clone https://github.com/NoVanity99/Wallpapers.git
cd Wallpapers
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`. With a phone connected over USB (USB debugging on), `adb install -r app/build/outputs/apk/release/app-release.apk` installs it. Or open the folder in Android Studio and press Run.

## Automatic builds

`.github/workflows/build.yml` runs on every push. It builds the signed release APK, uploads it as a workflow artifact and creates a GitHub Release named `v1.0.<build number>` with `WallpaperSelector.apk` attached. Pushes to the default branch become the **latest** release; other branches are published as pre-releases. Each build has a higher version code than the one before, so it installs as an update.

## Signing

Android only installs an update over an existing app when both are signed with the same key. To make that work out of the box, this repository contains a signing key (`keystore/sideload.jks`, password `wallpaperselector`) that local builds and CI both use.

That key is not secret: anyone who can read the repository could sign an APK that your phone would accept as an update of this app. That is fine for a personal sideloaded app, but if you make the repository public and want to rule it out, switch to a private key:

1. Create one: `keytool -genkeypair -keystore my.jks -alias wallpaperselector -keyalg RSA -keysize 2048 -validity 20000`
2. In the repository, open *Settings → Secrets and variables → Actions* and add `KEYSTORE_BASE64` (output of `base64 -w0 my.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.
3. Delete `keystore/sideload.jks` if you like. For local builds set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` as environment variables.

Switching keys means uninstalling the app once before installing the first build signed with the new key.

## Project layout

```
app/src/main/java/.../wallpaperselector/
  data/      catalog models, downloads and storage, settings
  gl/        EGL setup, the shader renderer, the vsync-driven render thread, touch tracking
  service/   LiveWallpaperService (the live wallpaper itself)
  ui/        Compose screens, theme, the live preview view
app/src/main/assets/
  shader_header.glsl    uniforms and helpers shared by every wallpaper
  wallpapers/           the three bundled wallpapers (copied here by tools/build_catalog.py)
catalog/
  catalog.json          generated list of all wallpapers
  wallpapers/<id>/      one folder per wallpaper
tools/
  build_catalog.py      regenerates catalog.json and the bundled copies
  render_thumbnails.js  compiles shaders in a headless browser and renders thumbnails
```

## Troubleshooting

- **"App not installed" when updating:** the installed copy was signed with a different key. Uninstall it, then install the new APK.
- **Only three wallpapers show up:** the phone can't reach the catalog. Check the connection and that the repository is public (see [Hosting](#hosting-the-catalog)).
- **A new wallpaper is missing:** run `python3 tools/build_catalog.py`, commit `catalog/catalog.json`, push, wait a few minutes, pull to refresh.
- **A wallpaper shows a plain dark gradient:** its shader failed to compile on the phone. `node tools/render_thumbnails.js <id>` prints the error.
- **Stutter on an older phone:** set the frame rate limit to 30 fps, or lower that wallpaper's `renderScale`.
- **Lock screen shows something else:** on Android 13 and older the lock screen follows the home screen unless a separate lock screen picture is set in system settings.

All wallpapers, shaders and artwork in this repository are original work created for this project.
