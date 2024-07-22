package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.github.tvbox.osc.update.Updater;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.BuildConfig;
import org.jetbrains.annotations.NotNull;
import android.app.Activity;
public class AboutDialog extends BaseDialog {

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_about);
        ((TextView) findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);
        findViewById(R.id.about_text).setOnClickListener(view -> Updater.get().update((Activity) context, true));
    }
}