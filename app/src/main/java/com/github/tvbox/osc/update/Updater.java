package com.github.tvbox.osc.update;

import android.app.Activity;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.dialog.UpdateDialog;
import com.github.tvbox.osc.update.Download;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.update.Github;

import java.io.File;

//用于更新的 类
public class Updater implements Download.Callback {

    private UpdateDialog updateDialog;

    private boolean updating = false;
    private boolean force = false;

    // 私有构造函数防止实例化
    private Updater() {
    }

    // 静态内部类
    private static class UpdaterHolder {
        private static final Updater INSTANCE = new Updater();
    }

    // 提供公共的访问方法
    public static Updater get() {
        return UpdaterHolder.INSTANCE;
    }

    private File getFile() {
        return FileUtils.cache("update.apk");
    }

    public void update(Activity activity) {
        update(activity, false);
    }

    public Updater force() {
        force = true;
        return this;
    }

    //检查更新
    public void update(Activity activity, boolean showToast) {
        if (updating) {
            ToastUtils.showShort(R.string.update_check);
            return;
        }
        updating = true;
        if (showToast) {
            ToastUtils.showShort(R.string.update_check);
        }
        App.execute(() -> check(activity, showToast));
    }

    public void check(Activity activity, boolean showToast) {
        Github github = new Github();
        do {
            //查询到了对应的数据
            if (github.find()) {
                boolean checkUpdate = github.checkUpdate();
                //需要更新
                if (checkUpdate || force) {
                    App.post(() -> show(activity, github));
                    github.findTagApkUrl();
                } else {
                    updating = false;
                }
                if (showToast) {
                    App.post(() -> {
                        ToastUtils.showShort(checkUpdate ? R.string.update_version : R.string.update_no_new_version);
                    });
                }
                return;
            }
        } while (github.getNextPage());
        updating = false;
    }

    private void show(Activity activity, Github github) {
        dismiss();
        updateDialog = new UpdateDialog(activity).setVersionDesc(github.findVersion(), github.findDesc());
        updateDialog.setGithub(github);
        updateDialog.setConfirmClickListener(view -> {
            updateDialog.setButtonEnable(false);
            progress(0);
            github.setOnCurrentApkUrlListener(apkUrl -> {
                Download.create(apkUrl, getFile(), this).start();
            });
        });
        updateDialog.setCancelClickListener(view -> {
            //TODO 未实现 取消此版本更新
        });
        updateDialog.setOnDismissListener(dialogInterface -> updating = false);
        updateDialog.show();
    }

    private void dismiss() {
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
        updateDialog = null;
    }

    @Override
    public void progress(int progress) {
        if (updateDialog != null) updateDialog.setConfirmProgress(progress);
    }

    @Override
    public void error(String msg) {
        ToastUtils.showShort(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        //简单校验一下文件完整性
        if (updateDialog != null) {
            boolean checkFileSize = updateDialog.getGithub().checkFileSize(file);
            if (!checkFileSize) {
                error(App.get().getResources().getString(R.string.update_check_file_size));
                return;
            }
        }
        FileUtils.openFileBySystem(file);
        ToastUtils.showShort(R.string.update_install_tip);
        dismiss();
    }

}