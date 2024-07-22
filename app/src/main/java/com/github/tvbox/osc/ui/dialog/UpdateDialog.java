package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.update.Github;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class UpdateDialog extends BaseDialog {

    private final TextView version, desc, confirm, cancel;
    private View.OnClickListener listenerConfirm;
    private View.OnClickListener listenerCancel;
    private Github github;

    public Github getGithub() {
        return github;
    }

    public void setGithub(Github github) {
        this.github = github;
    }

    public UpdateDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_update);
        setCancelable(false);
        version = findViewById(R.id.version);
        desc = findViewById(R.id.desc);
        confirm = findViewById(R.id.confirm);
        cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> {
            if (listenerCancel != null) {
                listenerCancel.onClick(v);
            }
            dismiss();
        });
        confirm.setOnClickListener(view -> {
            if (listenerConfirm != null) {
                listenerConfirm.onClick(view);
            }
        });
    }

    public void setButtonEnable(Boolean b) {
        confirm.setEnabled(b);
        cancel.setEnabled(b);
    }

    public void setConfirmText(CharSequence confirmText) {
        confirm.setText(confirmText);
    }

    public void setConfirmProgress(int progress) {
        confirm.setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    public UpdateDialog setVersionDesc(CharSequence version, CharSequence desc) {
        this.version.setText(version);
        this.desc.setText(desc);
        return this;
    }

    public void setConfirmClickListener(@NotNull View.OnClickListener l) {
        this.listenerConfirm = l;
    }

    public void setCancelClickListener(@NotNull View.OnClickListener l) {
        this.listenerCancel = l;
    }

}