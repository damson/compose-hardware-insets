"""Put a capture in a device frame, with the hardware drawn where the markers say it is.

The markers are the source of geometry: the red rectangles are the cutouts the
platform reported, so a lens is drawn inside each one, and the orange rails down
the sides are a waterfall's depth, so the glass is rolled off by exactly that.
Nothing here is positioned by hand, which is the point: the frame agrees with the
overlay because it is built from it.
"""
import sys
from PIL import Image, ImageDraw, ImageFilter

BODY = (10, 12, 16)
RIM = (46, 52, 60)
LENS = (4, 6, 9)
LENS_RING = (58, 66, 78)
GLINT = (150, 168, 190)

BEZEL = 20          # frame thickness around the screen
RIM_W = 4
SCREEN_R = 68       # screen corner radius
STEP = 2            # the sampling step the detector walks


def _is_red(p):
    return p[0] > 150 and p[1] < 90 and p[2] < 95


def _is_orange(p):
    return abs(p[0] - 224) < 30 and abs(p[1] - 138) < 30 and abs(p[2] - 30) < 40


def _runs(values, gap):
    values = sorted(values)
    out, run = [], [values[0]]
    for v in values[1:]:
        if v - run[-1] > gap:
            out.append(run)
            run = [v]
        else:
            run.append(v)
    out.append(run)
    return out


def cutouts(px, w, h):
    """The rectangles the marker overlay drew, as (left, top, right, bottom)."""
    red = [(x, y) for x in range(0, w, STEP) for y in range(0, h, STEP) if _is_red(px[x, y])]
    if not red:
        return []
    boxes = []
    for band in _runs({y for _, y in red}, 60):
        ys = set(band)
        xs = sorted({x for x, y in red if y in ys})
        for col in _runs(xs, 60):
            boxes.append((col[0], min(ys), col[-1], max(ys)))
    return boxes


def rails(px, w, h):
    """How deep the side insets run, from the marker line the overlay drew.

    The overlay draws the depth as a line at the depth, not as a filled band, so
    what is wanted is the innermost full-height orange column rather than a run
    from the edge. The outermost two columns are the window boundary and are not
    a depth.
    """
    def lit(x):
        return sum(1 for y in range(0, h, 4) if _is_orange(px[x, y])) > (h / 4) * 0.9

    reach = min(w // 6, 220)
    left = max((x for x in range(4, reach) if lit(x)), default=0)
    right = max((w - 1 - x for x in range(w - reach, w - 4) if lit(x)), default=0)
    return left + 1, right + 1


def frame(path, out):
    shot = Image.open(path).convert('RGB')
    w, h = shot.size
    px = shot.load()
    boxes = cutouts(px, w, h)
    left, right = rails(px, w, h)

    # A waterfall rolls the glass off at the sides: darken by that depth so the
    # edge reads as curved rather than as a flat panel with an orange stripe.
    if left > 6 or right > 6:
        glass = shot.copy()
        roll = ImageDraw.Draw(glass, 'RGBA')
        for d, span in ((0, left), (1, right)):
            for i in range(span):
                a = int(150 * (1 - i / max(span, 1)) ** 1.6)
                x = i if d == 0 else w - 1 - i
                roll.line([(x, 0), (x, h)], fill=(0, 0, 0, a))
        shot = glass

    # The lens itself, inside the rectangle the platform reported.
    lens = ImageDraw.Draw(shot, 'RGBA')
    for l, t, r, b in boxes:
        pad = 7
        box = (l + pad, t + pad, r - pad, b - pad)
        if box[2] - box[0] < 8 or box[3] - box[1] < 8:
            continue
        short = min(box[2] - box[0], box[3] - box[1])
        # A punch hole is a circle; a notch is a stadium hanging off the edge.
        if t <= 2 or b >= h - 3:
            radius = short // 2
            lens.rounded_rectangle(box, radius=radius, fill=LENS + (255,), outline=LENS_RING + (200,), width=3)
        else:
            lens.ellipse(box, fill=LENS + (255,), outline=LENS_RING + (200,), width=3)
        cx = box[0] + (box[2] - box[0]) * 0.34
        cy = box[1] + (box[3] - box[1]) * 0.34
        g = short * 0.13
        lens.ellipse((cx - g, cy - g, cx + g, cy + g), fill=GLINT + (120,))

    # Round the screen off, then set it in a body with a lighter rim.
    mask = Image.new('L', (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=SCREEN_R, fill=255)
    body_w, body_h = w + BEZEL * 2, h + BEZEL * 2
    body = Image.new('RGB', (body_w, body_h), BODY)
    d = ImageDraw.Draw(body)
    d.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=SCREEN_R + BEZEL, fill=BODY)
    d.rounded_rectangle((1, 1, body_w - 2, body_h - 2), radius=SCREEN_R + BEZEL - 1,
                        outline=RIM, width=RIM_W)
    body.paste(shot, (BEZEL, BEZEL), mask)

    # Outside the body is transparent, so the corners do not carry a black box.
    outer = Image.new('L', (body_w, body_h), 0)
    ImageDraw.Draw(outer).rounded_rectangle((0, 0, body_w - 1, body_h - 1),
                                            radius=SCREEN_R + BEZEL, fill=255)
    framed = Image.new('RGBA', (body_w, body_h), (0, 0, 0, 0))
    framed.paste(body, (0, 0), outer)
    framed.save(out)
    return boxes, left, right


if __name__ == '__main__':
    print(frame(sys.argv[1], sys.argv[2]))
