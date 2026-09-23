package ru.kryu.yanavipeb.nav

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import ru.kryu.yanavipeb.watch.Protocol

/** Turns a Yandex Maps / Yandex Navigator notification into a [NotificationSnapshot]. Call it on the main thread. */
class NotificationSnapshotReader(private val context: Context) {

    fun read(sbn: StatusBarNotification): NotificationSnapshot {
        val notification = sbn.notification
        val extras = notification.extras
        val texts = HashMap<String, String>()
        val icons = HashMap<String, Drawable>()

        // bigContentView carries every field; fall back to the compact view if it is missing.
        @Suppress("DEPRECATION")
        val views = notification.bigContentView ?: notification.contentView
        if (views != null) {
            try {
                walk(views.apply(context, FrameLayout(context)), texts, icons)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot inflate the notification RemoteViews", e)
            }
        }

        val icon = icons[ICON_TINTED] ?: icons[ICON_PLAIN]
        return NotificationSnapshot(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            hasCustomView = extras.getBoolean(EXTRA_CONTAINS_CUSTOM_VIEW),
            viewTexts = texts,
            iconArgb = icon?.let { render(it) },
        )
    }

    private fun walk(view: View, texts: MutableMap<String, String>, icons: MutableMap<String, Drawable>) {
        val id = idName(view)
        if (id != null) {
            when (view) {
                is TextView -> {
                    val text = view.text?.toString()?.trim().orEmpty()
                    if (text.isNotEmpty()) texts.putIfAbsent(id, text)
                }
                is ImageView -> view.drawable?.let { icons.putIfAbsent(id, it) }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), texts, icons)
        }
    }

    private fun idName(view: View): String? {
        if (view.id == View.NO_ID) return null
        return try {
            view.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            null
        }
    }

    /** Draws the icon scaled into ICON_SIZE x ICON_SIZE and returns its ARGB pixels. */
    private fun render(drawable: Drawable): IntArray {
        val size = Protocol.ICON_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        return pixels
    }

    private companion object {
        const val TAG = "NavSnapshot"
        const val EXTRA_CONTAINS_CUSTOM_VIEW = "android.contains.customView"
        const val ICON_TINTED = "primaryIconTinted"
        const val ICON_PLAIN = "primaryIcon"
    }
}
