package ru.kryu.yanavipeb.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import ru.kryu.yanavipeb.nav.IconEncoder
import ru.kryu.yanavipeb.watch.Protocol

/** Simple turn arrows drawn in code, encoded the same way as the icons taken from Yandex Maps. */
object DemoIcons {
    fun right(): ByteArray = draw(mirror = false)

    fun left(): ByteArray = draw(mirror = true)

    private fun draw(mirror: Boolean): ByteArray {
        val size = Protocol.ICON_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (mirror) canvas.scale(-1f, 1f, size / 2f, 0f)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        canvas.drawRect(20f, 30f, 30f, 62f, paint) // shaft
        canvas.drawRect(20f, 20f, 44f, 30f, paint) // turn
        val head = Path().apply {
            moveTo(44f, 6f)
            lineTo(62f, 25f)
            lineTo(44f, 44f)
            close()
        }
        canvas.drawPath(head, paint)

        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        return IconEncoder.encode(pixels)
    }
}
