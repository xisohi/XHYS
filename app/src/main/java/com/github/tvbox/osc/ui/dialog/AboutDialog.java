package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.os.Bundle;
import com.github.tvbox.osc.BuildConfig;
import androidx.annotation.NonNull;
import com.github.tvbox.osc.R;
import org.jetbrains.annotations.NotNull;
public class AboutDialog extends BaseDialog {

    private TextView appVersion;

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_about);

        // 初始化TextView
        appVersion = findViewById(R.id.app_version);

        // 获取版本号并设置到TextView
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            appVersion.setText("Version " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appVersion.setText("Version: Unknown");
        }
    }

    // 如果有必要，您可以覆盖其他Dialog方法，比如onShow, onDismiss等
}