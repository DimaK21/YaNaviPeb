package ru.kryu.yanavipeb.nav

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.kryu.yanavipeb.watch.Protocol

class IconEncoderTest {
    private val edge = Protocol.ICON_SIZE

    @Test
    fun transparentImageEncodesToZeros() {
        assertArrayEquals(ByteArray(Protocol.ICON_BYTES), IconEncoder.encode(blank()))
    }

    @Test
    fun outputIs512Bytes() {
        assertEquals(512, IconEncoder.encode(blank()).size)
    }

    @Test
    fun topLeftPixelIsHighBitOfFirstByte() {
        val out = IconEncoder.encode(blank().put(0, 0, 0xFFFFFFFF.toInt()))
        assertEquals(0x80, out[0].toInt() and 0xFF)
        assertEquals(1, out.count { it.toInt() != 0 })
    }

    @Test
    fun bottomRightPixelIsLowBitOfLastByte() {
        val out = IconEncoder.encode(blank().put(63, 63, 0xFFFFFFFF.toInt()))
        assertEquals(0x01, out[511].toInt() and 0xFF)
    }

    @Test
    fun pixelInsideRowGoesToRightByteAndBit() {
        // x = 9 is bit 1 of the second byte of the row (bit 6 counting from the low end); y = 1 skips 8 bytes.
        val out = IconEncoder.encode(blank().put(9, 1, 0xFFFFFFFF.toInt()))
        assertEquals(0x40, out[8 + 1].toInt() and 0xFF)
        assertEquals(1, out.count { it.toInt() != 0 })
    }

    @Test
    fun alphaBelow128IsBackground() {
        val out = IconEncoder.encode(blank().put(0, 0, (127 shl 24) or 0xFFFFFF))
        assertArrayEquals(ByteArray(Protocol.ICON_BYTES), out)
    }

    @Test
    fun alphaFrom128IsForeground() {
        val out = IconEncoder.encode(blank().put(0, 0, (128 shl 24) or 0xFFFFFF))
        assertEquals(0x80, out[0].toInt() and 0xFF)
    }

    @Test
    fun colourIsIgnored() {
        val out = IconEncoder.encode(blank().put(0, 0, 0xFF000000.toInt()))
        assertEquals(0x80, out[0].toInt() and 0xFF)
    }

    @Test
    fun rejectsWrongPixelCount() {
        assertThrows(IllegalArgumentException::class.java) { IconEncoder.encode(IntArray(10)) }
    }

    private fun blank() = IntArray(edge * edge)

    private fun IntArray.put(x: Int, y: Int, argb: Int): IntArray {
        this[y * edge + x] = argb
        return this
    }
}
