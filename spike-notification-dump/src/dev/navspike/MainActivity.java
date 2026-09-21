package dev.navspike;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView info = new TextView(this);
        info.setTextSize(16);
        info.setText("NavDump пишет уведомления Яндекс Карт и Навигатора в файл "
                + "Android/data/dev.navspike/files/dump.txt.\n\n"
                + "1. Включите NavDump в списке доступа к уведомлениям.\n"
                + "2. Запустите навигацию в Яндекс Картах или Навигаторе.");
        root.addView(info);

        Button open = new Button(this);
        open.setText("Открыть доступ к уведомлениям");
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        root.addView(open);

        setContentView(root);
    }
}
