#!/usr/bin/env python3
"""Builds a test ICON payload: 64x64, 1 bit per pixel, rows of 8 bytes, MSB first, 1 = white (512 bytes).

  make_icon.py sample right|left|straight OUT.bin   draw a synthetic arrow
  make_icon.py png IN.png OUT.bin                   convert a PNG (light pixels become white)
"""
import sys

from PIL import Image, ImageDraw, ImageOps

SIZE = 64


def sample(kind):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    white = (255, 255, 255, 255)
    if kind == "straight":
        draw.rectangle((27, 20, 37, 62), fill=white)
        draw.polygon([(32, 2), (52, 24), (12, 24)], fill=white)
        return img
    draw.rectangle((20, 30, 30, 62), fill=white)
    draw.rectangle((20, 20, 44, 30), fill=white)
    draw.polygon([(44, 6), (62, 25), (44, 44)], fill=white)
    return ImageOps.mirror(img) if kind == "left" else img


def from_png(path):
    return Image.open(path).convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS)


def encode(img):
    out = bytearray()
    for y in range(SIZE):
        for x0 in range(0, SIZE, 8):
            byte = 0
            for bit in range(8):
                r, g, b, a = img.getpixel((x0 + bit, y))
                lit = a >= 128 and (r + g + b) / 3 >= 192
                byte |= (1 if lit else 0) << (7 - bit)
            out.append(byte)
    return bytes(out)


def main(argv):
    if len(argv) != 4 or argv[1] not in ("sample", "png"):
        sys.exit(__doc__)
    img = sample(argv[2]) if argv[1] == "sample" else from_png(argv[2])
    with open(argv[3], "wb") as f:
        f.write(encode(img))


main(sys.argv)
