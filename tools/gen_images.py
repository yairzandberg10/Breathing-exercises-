"""Procedural artwork generator for Iron Breath (landscapes, silhouettes, app icon)."""
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1] if len(sys.argv) > 1 else "app/res/drawable-nodpi"
rng = np.random.default_rng(7)

def hexc(h):
    h = h.lstrip('#'); return np.array([int(h[i:i+2], 16) for i in (0, 2, 4)], float) / 255.0

def vgrad(w, h, stops):
    ys = np.linspace(0, 1, h)
    pos = [s[0] for s in stops]; cols = [hexc(s[1]) for s in stops]
    out = np.zeros((h, 3))
    for c in range(3):
        out[:, c] = np.interp(ys, pos, [col[c] for col in cols])
    return np.repeat(out[:, None, :], w, axis=1)

def noise1d(n, octaves=6, seed=0, rough=0.5):
    r = np.random.default_rng(seed); x = np.zeros(n); amp = 1.0; tot = 0
    for o in range(octaves):
        k = 2 ** (o + 1) + 1
        pts = r.uniform(-1, 1, k)
        xs = np.linspace(0, k - 1, n)
        i0 = np.floor(xs).astype(int); i1 = np.minimum(i0 + 1, k - 1); t = xs - i0
        t = t * t * (3 - 2 * t)
        x += amp * (pts[i0] * (1 - t) + pts[i1] * t); tot += amp; amp *= rough
    return x / tot

def glow(img, cx, cy, radius, color, strength=1.0, power=2.0):
    h, w, _ = img.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / radius
    g = np.clip(1 - d, 0, 1) ** power * strength
    img += g[..., None] * hexc(color)
    return img

def disc(img, cx, cy, r, color, soft=2.0):
    h, w, _ = img.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    a = np.clip((r - d) / soft, 0, 1)
    img[:] = img * (1 - a[..., None]) + a[..., None] * hexc(color)
    return img

def stars(img, count, ymax, seed=1, size=1.0):
    h, w, _ = img.shape; r = np.random.default_rng(seed)
    for _ in range(count):
        x = r.integers(0, w); y = int(r.uniform(0, 1) ** 1.6 * ymax * h)
        b = r.uniform(0.25, 1.0) * (1 - y / (ymax * h + 1)) ** 0.5
        img[y, x] += b
        if r.uniform() < 0.08 * size:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < w and 0 <= y + dy < h: img[y + dy, x + dx] += b * 0.45
    return img

def ridge_layer(img, base, amp, color, seed, octaves=7, rough=0.52, fog=None, fogh=0.12, sharp=False):
    h, w, _ = img.shape
    n = noise1d(w, octaves, seed, rough)
    if sharp: n = 1 - np.abs(n) * 2
    ridge = (base - amp * n) * h
    yy = np.arange(h)[:, None]
    mask = np.clip((yy - ridge[None, :]) / 1.5, 0, 1)
    col = np.broadcast_to(hexc(color), img.shape).copy()
    if fog is not None:
        depth = np.clip((yy - ridge[None, :]) / (fogh * h), 0, 1)
        f = (1 - depth) ** 1.5
        col = col * (1 - f[..., None] * 0.0) + 0  # base
        col = col * (1 - (1 - f[..., None]) * 0) 
        fogc = hexc(fog)
        col = col * (1 - (depth[..., None]) * 0.0)
        col = col + (fogc - col) * (depth[..., None] * 0.55)
    img[:] = img * (1 - mask[..., None]) + col * mask[..., None]
    return ridge

def haze(img, y0, y1, color, strength):
    h, w, _ = img.shape
    ys = np.arange(h); a = np.clip(1 - np.abs(ys - (y0 + y1) / 2 * h) / ((y1 - y0) / 2 * h), 0, 1) ** 1.5 * strength
    img[:] = img + (hexc(color) - img) * a[:, None, None]

def finish(img, name, vignette=0.55, grain=0.035, q=86):
    h, w, _ = img.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt(((xx - w / 2) / (w / 2)) ** 2 + ((yy - h / 2) / (h / 2)) ** 2)
    img *= (1 - vignette * np.clip(d - 0.45, 0, 1) ** 1.6)[..., None]
    img += rng.normal(0, grain, (h, w, 1))
    img = np.clip(img, 0, 1) ** (1 / 1.05)
    im = Image.fromarray((img * 255).astype(np.uint8))
    return im

# ---------------- figure silhouettes (same joint model as the app) ----------------
# order: head neck shoulder elbowB handB elbowF handF hip kneeB footB kneeF footF toeB toeF mid
POSES = {
 'lotus': [(0,.55),(0,.46),(0,.40),(-.17,.25),(-.23,.12),(.17,.25),(.23,.12),(0,.10),(-.24,.07),(.08,.04),(.24,.07),(-.08,.04),(.12,.05),(-.12,.05),(0,.25)],
 'warrior2': [(.02,.84),(0,.76),(0,.70),(-.19,.70),(-.37,.71),(.19,.70),(.37,.71),(0,.40),(-.18,.215),(-.36,.03),(.24,.26),(.26,.03),(-.40,.02),(.33,.02),(0,.55)],
 'standing': [(0,.93),(0,.85),(0,.80),(-.01,.63),(0,.47),(.01,.63),(.02,.47),(0,.50),(0,.27),(-.01,.03),(.01,.27),(0,.03),(.07,.01),(.08,.01),(0,.65)],
}

def draw_figure(im, pose, cx, groundy, scale, color=(8, 8, 10), glowc=None):
    d = ImageDraw.Draw(im)
    P = [(cx + x * scale, groundy - y * scale) for x, y in POSES[pose]]
    head, neck, sh, eB, hB, eF, hF, hip, kB, fB, kF, fF, tB, tF, mid = P
    def cap(a, b, wdt):
        d.line([a, b], fill=color, width=int(wdt))
        r = wdt / 2
        for p in (a, b): d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=color)
    lw = scale * 0.075
    cap(hip, kB, lw * 1.25); cap(kB, fB, lw); cap(fB, tB, lw * 0.6)
    cap(hip, kF, lw * 1.25); cap(kF, fF, lw); cap(fF, tF, lw * 0.6)
    cap(sh, eB, lw * 0.95); cap(eB, hB, lw * 0.8)
    # torso: broad shoulders tapering to waist
    tw = scale * 0.13
    d.polygon([(sh[0] - tw, sh[1]), (sh[0] + tw, sh[1]), (hip[0] + tw * 0.6, hip[1]), (hip[0] - tw * 0.6, hip[1])], fill=color)
    cap(sh, mid, lw * 1.9); cap(mid, hip, lw * 1.7)
    cap((sh[0] - tw * .8, sh[1] + lw * .3), (sh[0] + tw * .8, sh[1] + lw * .3), lw * 1.1)
    cap(neck, sh, lw * 0.9)
    cap(sh, eF, lw * 0.95); cap(eF, hF, lw * 0.8)
    r = scale * 0.062
    d.ellipse([head[0] - r, head[1] - r * 1.1, head[0] + r, head[1] + r * 1.05], fill=color)

def to_np(im): return np.asarray(im).astype(float) / 255.0

# ---------------- scenes ----------------
def hero():
    W, H = 1080, 1350
    img = vgrad(W, H, [(0, '#05080b'), (0.30, '#0f1c24'), (0.50, '#25414a'), (0.62, '#5f857f'), (0.68, '#9dbab0'), (1, '#0a1213')])
    stars(img, 900, 0.45, seed=3)
    glow(img, W * 0.68, H * 0.30, 360, '#a9c9c4', 0.25, 2.0)
    disc(img, W * 0.68, H * 0.30, 46, '#e9efe8', 2.5)
    glow(img, W * 0.5, H * 0.66, 800, '#bcd6cc', 0.22, 2.2)
    ridge_layer(img, 0.60, 0.10, '#4f716f', 11, fog='#a7c2b9')
    haze(img, 0.58, 0.70, '#b5cdc4', 0.28)
    ridge_layer(img, 0.66, 0.12, '#2e4a4b', 12, fog='#6f918b', sharp=True)
    ridge_layer(img, 0.74, 0.12, '#1a2d2f', 13, fog='#3c5a59', sharp=True)
    haze(img, 0.74, 0.86, '#5d7f7a', 0.22)
    ridge_layer(img, 0.84, 0.10, '#0d1718', 14, rough=0.6)
    ridge_layer(img, 0.93, 0.05, '#070c0d', 15, rough=0.65)
    im = finish(img, 'hero')
    draw_figure(im, 'standing', W * 0.30, H * 0.845, 150, color=(6, 10, 11))
    return im

def ocean():
    W, H = 1080, 1080
    img = vgrad(W, H, [(0, '#05070d'), (0.35, '#0e1a2c'), (0.55, '#27415a'), (0.58, '#3a5670'), (0.6, '#0d1826'), (1, '#03060a')])
    stars(img, 700, 0.5, seed=5)
    glow(img, W * 0.70, H * 0.22, 420, '#6fa0c8', 0.35, 2)
    disc(img, W * 0.70, H * 0.22, 60, '#e8eef2', 2.5)
    # moon craters shade
    glow(img, W * 0.68, H * 0.21, 30, '#b8c4cc', -0.25, 1)
    h = H; yy, xx = np.mgrid[0:H, 0:W]
    sea = yy > H * 0.585
    # moon path shimmer
    shimmer = np.exp(-((xx - W * 0.70) / (60 + (yy - H * 0.58) * 0.9)) ** 2) * sea
    waves = (np.sin(yy * 0.9 + np.sin(xx * 0.02) * 3) * 0.5 + 0.5) ** 6
    img += (shimmer * waves * 0.9)[..., None] * hexc('#cfe0ee')
    img += (sea * (np.sin(yy * 0.35 + xx * 0.004) * 0.5 + 0.5) ** 12 * 0.08)[..., None]
    # cliff left
    ridge_layer(img, 0.60, 0.02, '#0a111b', 31, rough=0.4)
    n = noise1d(H, 7, 21, 0.6)
    ys = np.arange(H) / H
    edge = (0.34 + 0.05 * n + np.clip(ys - 0.45, 0, 1) * 0.25) * W
    topn = noise1d(W, 6, 22, 0.5)
    top = (0.44 + 0.015 * topn + (xx[0] / W) * 0.03) * H
    mask = (xx < edge[:, None]) & (yy > top[None, :])
    img[mask] = hexc('#040507')
    rim = mask & (xx > edge[:, None] - 3)
    img[rim] = img[rim] + hexc('#3a5670') * 0.6
    img[yy > H * 0.62] = img[yy > H * 0.62]
    im = finish(img, 'ocean')
    draw_figure(im, 'lotus', W * 0.20, H * 0.452, 190)
    return im

def forest():
    W, H = 1080, 1080
    img = vgrad(W, H, [(0, '#04060a'), (0.4, '#0b1512'), (0.7, '#16241c'), (1, '#050705')])
    stars(img, 500, 0.4, seed=9)
    glow(img, W * 0.5, H * 0.84, 480, '#d9955a', 0.35, 2.0)
    glow(img, W * 0.5, H * 0.86, 130, '#f0c487', 0.6, 1.5)
    im = finish(img, 'forest', grain=0.02)
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(4)
    for layer, (col, base, hmin, hmax, cnt) in enumerate([((22, 34, 28), 0.62, 0.25, 0.4, 16), ((12, 20, 16), 0.72, 0.35, 0.55, 12), ((5, 8, 6), 0.86, 0.5, 0.8, 9)]):
        for i in range(cnt):
            x = r.uniform(-0.05, 1.05) * W
            if layer == 2 and abs(x - W * 0.5) < W * 0.22: continue
            th = r.uniform(hmin, hmax) * H; tw = th * r.uniform(0.22, 0.3); by = base * H
            for k in range(7):
                t = k / 7
                y1 = by - th * t; ww = tw * (1 - t) * 0.9 + 6
                d.polygon([(x - ww, y1), (x + ww, y1), (x, y1 - th * 0.28)], fill=col)
            d.rectangle([x - 5, by - 10, x + 5, by + 40], fill=col)
    d.rectangle([0, H * 0.86, W, H], fill=(4, 5, 4))
    # campfire logs & flames
    cx, cy = W * 0.5, H * 0.87
    d.polygon([(cx - 90, cy + 8), (cx + 80, cy - 20), (cx + 90, cy - 4), (cx - 80, cy + 22)], fill=(30, 16, 10))
    d.polygon([(cx + 90, cy + 8), (cx - 80, cy - 20), (cx - 90, cy - 4), (cx + 80, cy + 22)], fill=(40, 22, 12))
    for s, col in ((1.0, (214, 128, 72)), (0.7, (236, 176, 110)), (0.4, (248, 226, 186))):
        d.polygon([(cx - 55 * s, cy - 5), (cx - 20 * s, cy - 120 * s), (cx, cy - 70 * s), (cx + 18 * s, cy - 170 * s), (cx + 55 * s, cy - 5)], fill=col)
    arr = to_np(im)
    for _ in range(45):
        x = r.normal(W * 0.5, 160); y = r.uniform(H * 0.35, H * 0.8)
        glow(arr, x, y, r.uniform(3, 7), '#e8c48e', r.uniform(0.2, 0.6), 1.5)
    im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8))
    draw_figure(im, 'lotus', W * 0.24, H * 0.9, 260)
    return im

def cliff_sunrise():
    W, H = 1080, 1350
    img = vgrad(W, H, [(0, '#080d12'), (0.28, '#1c2c36'), (0.48, '#4c6670'), (0.58, '#9fb6b2'), (0.64, '#dcdccb'), (1, '#0a1012')])
    stars(img, 300, 0.25, seed=12)
    glow(img, W * 0.5, H * 0.63, 800, '#e6e2c8', 0.30, 2.2)
    disc(img, W * 0.5, H * 0.635, 85, '#f4f1e2', 3)
    ridge_layer(img, 0.64, 0.04, '#7f9a9a', 41, fog='#cbd5cb')
    haze(img, 0.6, 0.72, '#d4ddd3', 0.38)
    ridge_layer(img, 0.70, 0.07, '#47605f', 42, fog='#8ca39e')
    ridge_layer(img, 0.78, 0.06, '#203233', 43, fog='#4f6865')
    haze(img, 0.76, 0.84, '#6f8984', 0.25)
    yy, xx = np.mgrid[0:H, 0:W]
    top = H * (0.80 + 0.02 * np.sin(xx / W * 3))
    img[yy > top] = hexc('#070b0c')
    im = finish(img, 'cliff')
    draw_figure(im, 'lotus', W * 0.5, H * 0.80, 230, color=(6, 10, 11))
    return im

def dunes():
    W, H = 1080, 1350
    img = vgrad(W, H, [(0, '#0b1116'), (0.3, '#2b3c44'), (0.55, '#8ea39c'), (0.66, '#e2d6b6'), (1, '#1b1a14')])
    glow(img, W * 0.28, H * 0.58, 700, '#eadcb4', 0.3, 2.2)
    disc(img, W * 0.28, H * 0.585, 72, '#f6efda', 3)
    stars(img, 250, 0.2, seed=17)
    yy, xx = np.mgrid[0:H, 0:W]
    for base, amp, col, seed, fr in ((0.66, 0.05, '#a99c7e', 51, 1.3), (0.74, 0.07, '#7b745e', 52, 1.0), (0.84, 0.08, '#3c3b31', 53, 0.8), (0.93, 0.06, '#141512', 54, 0.6)):
        n = noise1d(W, 4, seed, 0.35)
        ridge = (base - amp * n) * H
        m = yy > ridge[None, :]
        # dune light side vs shadow side
        depth = np.clip((yy - ridge[None, :]) / (0.12 * H), 0, 1)
        shade = 1.0 - 0.35 * depth + 0.25 * np.exp(-((yy - ridge[None, :]) / 6.0) ** 2)
        c = hexc(col)
        img[m] = (c[None, None, :] * shade[..., None])[m]
    im = finish(img, 'dunes')
    draw_figure(im, 'warrior2', W * 0.64, H * 0.815, 260, color=(10, 12, 12))
    return im

def night_lake():
    W, H = 1080, 1080
    img = vgrad(W, H, [(0, '#020308'), (0.5, '#0c1430'), (0.62, '#1a2448'), (0.64, '#070b18'), (1, '#020308')])
    stars(img, 1500, 0.62, seed=33, size=2)
    # milky way band
    yy, xx = np.mgrid[0:H, 0:W]
    band = np.exp(-((yy - (H * 0.1 + xx * 0.45)) / 90) ** 2) * (yy < H * 0.62)
    img += band[..., None] * hexc('#5a6aa0') * 0.35
    glow(img, W * 0.25, H * 0.2, 60, '#fff6e0', 0.9, 1.2)
    disc(img, W * 0.25, H * 0.2, 36, '#fdf6e6', 2)
    disc(img, W * 0.25 + 14, H * 0.2 - 8, 32, '#0a1028', 2)
    ridge_layer(img, 0.63, 0.08, '#05070f', 61, sharp=True)
    lake = yy > H * 0.64
    refl = np.flipud(img[int(H * 0.28):int(H * 0.64)])
    hh = min(refl.shape[0], H - int(H * 0.64))
    img[int(H * 0.64):int(H * 0.64) + hh] = refl[:hh] * 0.45
    return finish(img, 'lake')

def icon(size):
    s = size * 4
    img = vgrad(s, s, [(0, '#12201f'), (0.55, '#1d3533'), (1, '#0a1213')])
    glow(img, s * 0.5, s * 0.56, s * 0.55, '#8cc4ac', 0.45, 2.0)
    im = finish(img, 'icon', vignette=0.4, grain=0.0)
    d = ImageDraw.Draw(im)
    cx, cy = s / 2, s * 0.5
    for i, (r, wd, a) in enumerate(((0.36, 0.022, 255), (0.28, 0.014, 180), (0.21, 0.01, 120))):
        R = r * s; d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=(150, 205, 180, a), width=int(wd * s))
    for k in range(60):
        ang = k / 60 * 2 * math.pi; r1 = 0.42 * s; r2 = (0.46 if k % 5 == 0 else 0.44) * s
        d.line([(cx + math.cos(ang) * r1, cy + math.sin(ang) * r1), (cx + math.cos(ang) * r2, cy + math.sin(ang) * r2)], fill=(205, 182, 140), width=int(s * (0.012 if k % 5 == 0 else 0.006)))
    d.polygon([(cx - 0.2 * s, cy + 0.1 * s), (cx - 0.05 * s, cy - 0.1 * s), (cx + 0.02 * s, cy - 0.01 * s), (cx + 0.08 * s, cy - 0.07 * s), (cx + 0.2 * s, cy + 0.1 * s)], fill=(10, 20, 20))
    d.ellipse([cx - 0.04 * s, cy - 0.21 * s, cx + 0.04 * s, cy - 0.13 * s], fill=(236, 240, 228))
    im = im.resize((size, size), Image.LANCZOS)
    mask = Image.new('L', (size, size), 0); ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0)); out.paste(im, (0, 0), mask)
    return out

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (('img_hero', hero), ('img_ocean', ocean), ('img_forest', forest), ('img_cliff', cliff_sunrise), ('img_dunes', dunes), ('img_lake', night_lake)):
        fn().save(os.path.join(OUT, name + '.jpg'), quality=84, optimize=True, progressive=True)
        print('wrote', name)
    res = os.path.dirname(OUT.rstrip('/'))
    for d, sz in (('mipmap-mdpi', 48), ('mipmap-hdpi', 72), ('mipmap-xhdpi', 96), ('mipmap-xxhdpi', 144), ('mipmap-xxxhdpi', 192)):
        os.makedirs(os.path.join(res, d), exist_ok=True)
        icon(sz).save(os.path.join(res, d, 'ic_launcher.png'))
    print('icons done')

def stat_icon(size):
    s = size * 4
    im = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = s / 2
    d.ellipse([s * 0.06, s * 0.06, s * 0.94, s * 0.94], outline=(255, 255, 255, 255), width=int(s * 0.08))
    d.polygon([(s * 0.22, s * 0.68), (s * 0.42, s * 0.36), (s * 0.52, s * 0.50), (s * 0.60, s * 0.42), (s * 0.78, s * 0.68)], fill=(255, 255, 255, 255))
    d.ellipse([s * 0.58, s * 0.2, s * 0.72, s * 0.34], fill=(255, 255, 255, 255))
    return im.resize((size, size), Image.LANCZOS)

if __name__ == '__main__':
    res = os.path.dirname(OUT.rstrip('/'))
    for dname, sz in (('drawable-mdpi', 24), ('drawable-hdpi', 36), ('drawable-xhdpi', 48), ('drawable-xxhdpi', 72), ('drawable-xxxhdpi', 96)):
        os.makedirs(os.path.join(res, dname), exist_ok=True)
        stat_icon(sz).save(os.path.join(res, dname, 'ic_stat.png'))
    print('stat icons done')
