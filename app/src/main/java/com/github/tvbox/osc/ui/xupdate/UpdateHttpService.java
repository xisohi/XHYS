package com.github.tvbox.osc.ui.xupdate;

import android.util.Log;

import androidx.annotation.NonNull;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.xuexiang.xupdate.proxy.IUpdateHttpService;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;


/**
 * @version 1.0
 * @auther lsj
 * @date 2023/08/14
 */
public class UpdateHttpService implements IUpdateHttpService {
    public static String baseUrl = Constants.UPDATE_URL;

    public UpdateHttpService() {
    }

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
//        Log.e("XUpdate", "asyncGet--- " + url);

        OkGo.<String>get(url)
                .tag(url)                    // 请求的 tag, 主要用于取消对应的请求
                .params(transform(params))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        //Log.e("XUpdate", "--- " + response);
                        callBack.onSuccess(response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        //Log.e("XUpdate", "onError--- " + response);
                        callBack.onError(response.getException());
                    }
                });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull Callback callBack) {
        //Log.e("XUpdate", "asyncPost--- " + url);
        OkGo.<String>post(url)
                .tag(url)                    // 请求的 tag, 主要用于取消对应的请求
                .params(transform(params))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        callBack.onSuccess(response.body());
                        Log.e("XUpdate", "onSuccess--- " + response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        Log.e("XUpdate", "onError--- " + response);
                        callBack.onError(response.getException());
                    }
                });

    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, final @NonNull DownloadCallback callback) {
//        Log.e("XUpdate", "download--- " + url);
//        Log.e("XUpdate", "download--- " + path);
//        Log.e("XUpdate", "download--- " + fileName);
        OkGo.<File>get(url)
                .tag(url)                    // 请求的 tag, 主要用于取消对应的请求
                .execute(new FileCallback(path, fileName) {
                    @Override
                    public void downloadProgress(Progress progress) {
                        super.downloadProgress(progress);
                        callback.onProgress(progress.fraction, progress.totalSize);

                    }

                    @Override
                    public void onStart(com.lzy.okgo.request.base.Request<File, ? extends com.lzy.okgo.request.base.Request> request) {
                        super.onStart(request);
                        //Log.e("XUpdate", "download--- 下载开始" + fileName);
                        callback.onStart();

                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        //Log.e("XUpdate", "download--- 下载完成" + fileName);
                    }


                    @Override
                    public void onSuccess(Response<File> response) {
                        //Log.e("XUpdate", "download--- 下载成功" + fileName);
                        callback.onSuccess(response.body());

                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        //Log.e("XUpdate", "download--- 下载失败");
                        callback.onError(response.getException());
                    }

                });
    }

    @Override
    public void cancelDownload(@NonNull String url) {
        OkGo.getInstance().cancelTag(url);
    }

    private Map<String, String> transform(Map<String, Object> params) {
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

    public static String getVersionCheckUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "update/checkVersion";
        } else {
            return baseUrl + "/update/checkVersion";
        }
    }

    public static String getDownLoadUrl(String url) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "update/apk/" + url;
        } else {
            return baseUrl + "/update/apk/" + url;
        }
    }

}