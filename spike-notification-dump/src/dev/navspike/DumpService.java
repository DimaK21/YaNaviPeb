package dev.navspike;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Throwaway spike: dumps every distinct notification of the Yandex apps to
 * getExternalFilesDir()/dump.txt so we can see what a navigation notification carries.
 */
public class DumpService extends NotificationListenerService {
    private static final String TAG = "NavDump";
    private static final Set<String> PACKAGES = new HashSet<>(Arrays.asList(
            "ru.yandex.yandexmaps", "ru.yandex.yandexnavi"));

    private final Map<String, String> lastDump = new HashMap<>();
    private final Set<String> savedImages = new HashSet<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private File dir;

    @Override
    public void onCreate() {
        super.onCreate();
        dir = getExternalFilesDir(null);
    }

    @Override
    public void onListenerConnected() {
        write("=== listener connected ===");
        StatusBarNotification[] active = getActiveNotifications();
        if (active != null) {
            for (StatusBarNotification sbn : active) handle(sbn, "ACTIVE");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handle(sbn, "POSTED");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (!PACKAGES.contains(sbn.getPackageName())) return;
        lastDump.remove(key(sbn));
        Log.i(TAG, "REMOVED " + key(sbn));
        write("=== REMOVED " + key(sbn) + " ===");
    }

    private void handle(StatusBarNotification sbn, String event) {
        if (!PACKAGES.contains(sbn.getPackageName())) return;
        String k = key(sbn);
        String body;
        try {
            body = describe(sbn);
        } catch (Throwable t) {
            body = "describe failed: " + Log.getStackTraceString(t);
        }
        if (body.equals(lastDump.put(k, body))) return;

        Bundle ex = sbn.getNotification().extras;
        Log.i(TAG, event + " " + k + " | " + ex.getCharSequence(Notification.EXTRA_TITLE)
                + " | " + ex.getCharSequence(Notification.EXTRA_TEXT));
        write("=== " + event + " " + k + " ===\n" + body);
    }

    private static String key(StatusBarNotification sbn) {
        return sbn.getPackageName() + "#" + sbn.getId() + "#" + sbn.getTag();
    }

    private String describe(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        StringBuilder sb = new StringBuilder();
        line(sb, 0, "package", sbn.getPackageName());
        line(sb, 0, "id / tag", sbn.getId() + " / " + sbn.getTag());
        line(sb, 0, "channelId", n.getChannelId());
        line(sb, 0, "category", n.category);
        line(sb, 0, "group", n.getGroup());
        line(sb, 0, "flags", flagNames(n.flags));
        line(sb, 0, "ongoing / clearable", sbn.isOngoing() + " / " + sbn.isClearable());
        line(sb, 0, "visibility", String.valueOf(n.visibility));
        line(sb, 0, "tickerText", String.valueOf(n.tickerText));

        line(sb, 0, "extras", "");
        Bundle ex = n.extras;
        for (String k : new TreeSet<>(ex.keySet())) {
            Object v = ex.get(k);
            String desc = v == null ? "null" : v.getClass().getSimpleName() + " = " + brief(v);
            if (v instanceof Bitmap) desc += " -> " + saveImage((Bitmap) v, "extra");
            line(sb, 1, k, desc);
        }

        if (n.actions != null) {
            for (Notification.Action a : n.actions) line(sb, 0, "action", String.valueOf(a.title));
        }
        line(sb, 0, "smallIcon", iconFile(n.getSmallIcon(), "small"));
        line(sb, 0, "largeIcon", iconFile(n.getLargeIcon(), "large"));

        dumpRemote(sb, "contentView", n.contentView);
        dumpRemote(sb, "bigContentView", n.bigContentView);
        dumpRemote(sb, "headsUpContentView", n.headsUpContentView);
        return sb.toString();
    }

    private void dumpRemote(StringBuilder sb, String name, RemoteViews rv) {
        if (rv == null) {
            line(sb, 0, name, "null");
            return;
        }
        line(sb, 0, name, "layoutId=" + rv.getLayoutId() + " package=" + rv.getPackage());
        try {
            walk(sb, rv.apply(this, new FrameLayout(this)), 1);
        } catch (Throwable t) {
            line(sb, 1, "apply failed", t.toString());
        }
    }

    private void walk(StringBuilder sb, View v, int depth) {
        StringBuilder d = new StringBuilder(v.getClass().getSimpleName());
        d.append(" id=").append(idName(v));
        if (v.getContentDescription() != null) {
            d.append(" desc=\"").append(v.getContentDescription()).append('"');
        }
        if (v instanceof TextView) {
            d.append(" text=\"").append(((TextView) v).getText()).append('"');
        }
        if (v instanceof ProgressBar) {
            ProgressBar p = (ProgressBar) v;
            d.append(" progress=").append(p.getProgress()).append('/').append(p.getMax());
        }
        if (v instanceof ImageView) {
            d.append(" image=").append(drawableFile(((ImageView) v).getDrawable(), "view"));
        }
        line(sb, depth, d.toString());
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(sb, g.getChildAt(i), depth + 1);
        }
    }

    private static String idName(View v) {
        int id = v.getId();
        if (id == View.NO_ID) return "-";
        try {
            return v.getResources().getResourceEntryName(id);
        } catch (Throwable t) {
            return "0x" + Integer.toHexString(id);
        }
    }

    private String iconFile(Icon icon, String prefix) {
        if (icon == null) return "null";
        try {
            return icon + " -> " + drawableFile(icon.loadDrawable(this), prefix);
        } catch (Throwable t) {
            return icon + " -> load failed: " + t;
        }
    }

    private String drawableFile(Drawable d, String prefix) {
        if (d == null) return "none";
        try {
            int w = d.getIntrinsicWidth() > 0 ? Math.min(d.getIntrinsicWidth(), 512) : 128;
            int h = d.getIntrinsicHeight() > 0 ? Math.min(d.getIntrinsicHeight(), 512) : 128;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, w, h);
            d.draw(new Canvas(bmp));
            return saveImage(bmp, prefix);
        } catch (Throwable t) {
            return "render failed: " + t;
        }
    }

    /** Saves the bitmap over a mid-gray background so both white and black glyphs stay visible. */
    private String saveImage(Bitmap src, String prefix) {
        try {
            Bitmap soft = src.copy(Bitmap.Config.ARGB_8888, false);
            int w = soft.getWidth();
            int h = soft.getHeight();
            int[] px = new int[w * h];
            soft.getPixels(px, 0, w, 0, 0, w, h);
            String name = prefix + "-" + Integer.toHexString(Arrays.hashCode(px) ^ (w * 31 + h)) + ".png";
            if (savedImages.add(name)) {
                Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                out.eraseColor(0xFF808080);
                new Canvas(out).drawBitmap(soft, 0, 0, null);
                try (FileOutputStream fos = new FileOutputStream(new File(dir, name))) {
                    out.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
            }
            return name + " (" + w + "x" + h + ")";
        } catch (Throwable t) {
            return "save failed: " + t;
        }
    }

    private static String brief(Object v) {
        if (v instanceof CharSequence) return "\"" + v + "\"";
        if (v instanceof Bitmap) return ((Bitmap) v).getWidth() + "x" + ((Bitmap) v).getHeight();
        if (v instanceof Bundle) return "keys " + ((Bundle) v).keySet();
        if (v instanceof Object[]) return Arrays.toString((Object[]) v);
        return String.valueOf(v);
    }

    private static String flagNames(int f) {
        String[] names = {"ONGOING_EVENT", "NO_CLEAR", "FOREGROUND_SERVICE", "LOCAL_ONLY",
                "GROUP_SUMMARY", "ONLY_ALERT_ONCE", "AUTO_CANCEL"};
        int[] bits = {Notification.FLAG_ONGOING_EVENT, Notification.FLAG_NO_CLEAR,
                Notification.FLAG_FOREGROUND_SERVICE, Notification.FLAG_LOCAL_ONLY,
                Notification.FLAG_GROUP_SUMMARY, Notification.FLAG_ONLY_ALERT_ONCE,
                Notification.FLAG_AUTO_CANCEL};
        StringBuilder sb = new StringBuilder("0x" + Integer.toHexString(f) + " [");
        for (int i = 0; i < bits.length; i++) {
            if ((f & bits[i]) != 0) sb.append(names[i]).append(' ');
        }
        return sb.append(']').toString();
    }

    private static void line(StringBuilder sb, int depth, String text) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(text).append('\n');
    }

    private static void line(StringBuilder sb, int depth, String key, String value) {
        line(sb, depth, key + ": " + value);
    }

    private void write(String s) {
        try (FileWriter w = new FileWriter(new File(dir, "dump.txt"), true)) {
            w.write("[" + clock.format(new Date()) + "] " + s + "\n");
        } catch (Exception e) {
            Log.e(TAG, "write failed", e);
        }
    }
}
