"""Original, deterministic pixel atlases and synthesized sounds. No downloaded media.

Requires Pillow and ffmpeg on PATH. Run from any directory. Texture regions match
TurkeyModel.java. Sounds are synthesized rather than field recordings.
"""
from pathlib import Path
import math
import random
import struct
import subprocess
import tempfile
import wave
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/turkeytime"
TEXTURES = ASSETS / "textures/entity/turkey"
TEXTURES.mkdir(parents=True, exist_ok=True)


def shift(color, delta):
    return tuple(max(0, min(255, c + delta)) for c in color)


def atlas(sex, variation=0, autumn=False):
    baby = sex == "poult"
    male = sex == "tom"
    body = (100, 70, 43) if male else (125, 102, 76) if not baby else (172, 139, 83)
    body = shift(body, (-7, 0, 7)[variation])
    if autumn:
        body = (139, 78, 38) if male else (151, 102, 54) if not baby else (189, 143, 70)
    image = Image.new("RGB", (128, 128), shift(body, -28))
    px = image.load()
    rng = random.Random(1600 + variation)
    # Small staggered chevrons suggest layered feathers without noisy realism.
    for y in range(24):
        for x in range(44):
            row = y % 4
            feather = (x + (y // 4 % 2) * 2) % 5
            delta = (-19 if row == 3 else 14 if row == 0 and feather in (1, 2, 3) else 0)
            if baby:
                delta = -30 if ((x + y // 3) % 8 in (0, 1)) else rng.choice((-6, 0, 6))
            px[x, y] = shift(body, delta)
    draw = ImageDraw.Draw(image)
    # Wing coverts with darker bars and pale feather tips.
    for y in range(32, 52):
        for x in range(24):
            delta = -23 if (x + y // 3) % 5 == 0 else 15 if y % 4 == 0 else -2
            px[x, y] = shift(body, delta)
    # Tail feather UV island: tan terminal band, dark subterminal bar, bronze shaft.
    for y in range(32, 46):
        for x in range(32, 42):
            yy = y - 33
            color = (191, 157, 105) if yy <= 1 else (46, 35, 30) if yy <= 3 else shift(body, 8 if yy % 3 == 0 else -18)
            if baby:
                color = shift(body, -12 if yy % 3 else 15)
            px[x, y] = color
    # Neck and head islands: blue-grey crown, muted red skin; poult stays feathered.
    draw.rectangle((48, 0, 61, 12), fill=body if baby else (146, 69, 63) if male else (136, 109, 98))
    draw.rectangle((64, 0, 85, 12), fill=shift(body, 13) if baby else (122, 149, 157) if male else (146, 134, 115))
    for x in range(64, 85, 4):
        draw.point((x, 5), fill=shift(body, -12) if baby else (88, 111, 120))
    draw.rectangle((88, 0, 100, 7), fill=(180, 151, 86))
    draw.rectangle((104, 0, 110, 9), fill=(173, 48, 42) if male else (146, 79, 65))
    draw.rectangle((88, 16, 94, 23), fill=(158, 117, 88))
    draw.rectangle((96, 16, 111, 23), fill=(160, 119, 89))
    draw.rectangle((116, 0, 121, 4), fill=(22, 17, 13))
    # Breast has restrained warmer/iridescent accents; never a white chicken bib.
    draw.rectangle((0, 64, 18, 76), fill=shift(body, -10))
    for y in range(65, 76):
        for x in range(18):
            px[x, y] = shift(body, 15 if (x + y * 2) % 6 == 0 else -12)
    name = f"{sex}_autumn.png" if autumn else "poult.png" if baby else f"{sex}_{variation}.png"
    image.save(TEXTURES / name)


for sex in ("tom", "hen"):
    for variant in range(3):
        atlas(sex, variant)
atlas("poult", 1)
for sex in ("tom", "hen", "poult"):
    atlas(sex, 1, autumn=True)

# Hand-authored pixel silhouette, shading and spots for the creative egg.
egg = Image.new("RGBA", (16, 16))
draw = ImageDraw.Draw(egg)
draw.polygon([(7, 1), (9, 1), (11, 3), (13, 7), (14, 11), (13, 14), (10, 15),
              (5, 15), (2, 13), (2, 9), (4, 4)], fill=(66, 42, 29))
draw.polygon([(7, 2), (9, 2), (11, 5), (13, 10), (12, 13), (9, 14), (5, 14),
              (3, 12), (3, 9), (5, 4)], fill=(167, 124, 75))
draw.line([(7, 3), (5, 6), (4, 10)], fill=(210, 168, 107), width=1)
for rect in [(8, 4, 9, 5), (5, 8, 6, 9), (10, 9, 12, 10), (7, 12, 8, 13)]:
    draw.rectangle(rect, fill=(94, 53, 34))
draw.point((11, 6), fill=(145, 49, 41))
egg_path = ASSETS / "textures/item/turkey_spawn_egg.png"
egg.save(egg_path)


def render_sound(name, duration, kind, seed):
    rate = 22050
    rng = random.Random(seed)
    samples = []
    phase = 0.0
    filtered_noise = 0.0
    for i in range(int(rate * duration)):
        t = i / rate
        end = min(1.0, t / .012, (duration - t) / .045)
        noise = rng.uniform(-1, 1)
        filtered_noise = .65 * filtered_noise + .35 * noise
        if kind == "flutter":
            pulse = max(0, math.sin(2 * math.pi * 10 * t)) ** 2
            value = (noise - filtered_noise) * pulse * .5
        else:
            # Downward inflection, low harmonics and irregular rapid amplitude pulses.
            base = {"cluck": 570, "gobble": 410, "alarm": 820, "hurt": 710, "death": 530}[kind]
            freq = base * (1.2 - .48 * t / duration) + 45 * math.sin(t * 86)
            phase += 2 * math.pi * freq / rate
            voice = math.sin(phase) + .38 * math.sin(phase * 2) + .14 * math.sin(phase * 3)
            pulses = 1 if kind != "gobble" else .18 + .82 * max(0, math.sin(t * 2 * math.pi * 17))
            burst = math.exp(-t * (9 if kind == "cluck" else 2.3))
            value = (.25 * voice + .08 * filtered_noise) * pulses * burst
        samples.append(max(-.9, min(.9, value * end)))
    out = ASSETS / "sounds/entity/turkey"
    out.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="turkeytime-audio-") as tmp:
        wav_path = Path(tmp) / "source.wav"
        with wave.open(str(wav_path), "wb") as wav:
            wav.setparams((1, 2, rate, 0, "NONE", "not compressed"))
            wav.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in samples))
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav_path),
                        "-c:a", "libvorbis", "-q:a", "3", "-map_metadata", "-1", str(out / f"{name}.ogg")], check=True)


for name, seconds, kind, seed in [("cluck_1", .28, "cluck", 16), ("cluck_2", .36, "cluck", 17),
                                  ("gobble", .85, "gobble", 18), ("alarm", .38, "alarm", 19),
                                  ("hurt", .25, "hurt", 20), ("death", .62, "death", 21),
                                  ("flutter", .55, "flutter", 22)]:
    render_sound(name, seconds, kind, seed)
print("Generated 10 turkey atlases, spawn-egg icon and 7 original synthesized sounds.")
