package ru.kryu.yanavipeb.nav

import ru.kryu.yanavipeb.watch.Protocol

object IconEncoder {
    private const val ALPHA_THRESHOLD = 128

    /**
     * Packs [argb] (ICON_SIZE x ICON_SIZE pixels, row-major, as filled by Bitmap.getPixels) into
     * 1 bit per pixel: rows of ICON_SIZE / 8 bytes, most significant bit first, 1 = white.
     * Only alpha matters: the source icon is a white glyph on a transparent background.
     */
    fun encode(argb: IntArray): ByteArray {
        val size = Protocol.ICON_SIZE
        require(argb.size == size * size) { "Expected ${size * size} pixels, got ${argb.size}" }
        val out = ByteArray(Protocol.ICON_BYTES)
        for (i in argb.indices) {
            if ((argb[i] ushr 24) >= ALPHA_THRESHOLD) {
                val x = i % size
                val index = (i / size) * (size / 8) + x / 8
                out[index] = (out[index].toInt() or (0x80 shr (x % 8))).toByte()
            }
        }
        return out
    }
}
