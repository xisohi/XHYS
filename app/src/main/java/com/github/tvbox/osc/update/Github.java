package com.github.tvbox.osc.update;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.update.GithubTagEntity;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.List;

import io.noties.markwon.Markwon;
import okhttp3.Request;
import okhttp3.Response;

//用于更新用的
public class Github {
    //Tag名称
    private static final String TAG_NAME = "takagen99";

    private static final String PREFIX = "1.0.";
    //最大分页数
    private static final int MAX_PAGE = 10;

    private int currentPage = 0;
    //获取详情地址
    private static final String UPDATE_LIST_URL = "https://api.github.com/repos/o0HalfLife0o/TVBoxOSC/releases?per_page=5&page=";
    //https://api.github.com/repos/o0HalfLife0o/TVBoxOSC/releases/latest
    //当前分页数据
    private List<GithubTagEntity> currentPageList;
    //选择的Tag数据
    private GithubTagEntity currentTagEntity;
    private String currentApkUrl;
    private long currentApkSize;

    private OnCurrentApkUrlListener l;

    public void setOnCurrentApkUrlListener(OnCurrentApkUrlListener l) {
        this.l = l;
        if (currentApkUrl != null) {
            l.onCurrentApkUrlListener(currentApkUrl);
        }
    }

    //查找合适的Item 如果这个值查询最后一个就好了
    public void findTagItem() {
        if (currentPageList == null) return;
        for (GithubTagEntity githubTagEntity : currentPageList) {
            for (GithubTagEntity.AssetsBean asset : githubTagEntity.getAssets()) {
                if (asset.getName().contains(TAG_NAME)) {
                    currentTagEntity = githubTagEntity;
                    return;
                }
            }
        }
    }

    public boolean find() {
        return currentTagEntity != null;
    }

    //检查是否需要更新 这里 经过排查 这个感觉是不对劲的 但是好像又没啥问题
    public boolean checkUpdate() {
        if (find()) {
            long newVersion = Long.parseLong(currentTagEntity.getName().replace("-", "").replace("_", ""));
            long oldVersion = Long.parseLong(BuildConfig.VERSION_NAME.replace(PREFIX, "").replace("_", "").replace("-", ""));
            return newVersion > oldVersion;
        }
        return false;
    }


    //获取更新Url
    public void findTagApkUrl() {
        if (find()) {
            for (GithubTagEntity.AssetsBean asset : currentTagEntity.getAssets()) {
                String assetName = asset.getName();
                if (assetName.contains(BuildConfig.FLAVOR_abi) && assetName.contains(BuildConfig.FLAVOR_brand)) {
                    currentApkUrl = GithubProxy.getGithubProxyUrl(asset.getBrowser_download_url());
                    currentApkSize = asset.getSize();
                    if (this.l != null) {
                        this.l.onCurrentApkUrlListener(currentApkUrl);
                    }
                    return;
                }
            }
        }
    }

    // 获取更新描述
    public CharSequence findDesc() {
        if (find()) {
            Markwon markwon = Markwon.create(App.get());
            return markwon.toMarkdown(currentTagEntity.getBody());
        }
        return "";
    }

    //返回值表示当前分页数据是否获取成功
    public boolean getNextPage() {
        currentPageList = getNextPageList();
        findTagItem();
        return currentPageList != null;
    }

    //获取下一页数据
    private List<GithubTagEntity> getNextPageList() {
        currentPage++;
        if (currentPage > MAX_PAGE) {
            return null;
        }
        try (Response execute = OkGoHelper.getDefaultClient().newCall(new Request.Builder().get().url(UPDATE_LIST_URL + currentPage).build()).execute()) {
            String body = execute.body().string();
            return App.getGson().fromJson(body, new TypeToken<List<GithubTagEntity>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public CharSequence findVersion() {
        if (!find()) return "";
        return currentTagEntity.getName();
    }

    public boolean checkFileSize(File file) {
        return currentApkSize == file.length();
    }

    public interface OnCurrentApkUrlListener {
        void onCurrentApkUrlListener(String apkUrl);
    }
}
