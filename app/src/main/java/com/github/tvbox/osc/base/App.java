package com.github.tvbox.osc.base;

import androidx.multidex.MultiDexApplication;
import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LocaleHelper;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;
import java.io.File;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;
import com.github.tvbox.osc.ui.xupdate.UpdateHttpService;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.entity.UpdateError;
import com.xuexiang.xupdate.listener.OnUpdateFailureListener;
import com.xuexiang.xupdate.utils.UpdateUtils;
import com.lzy.okgo.OkGo;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import com.github.tvbox.osc.R;
/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;
    private static P2PClass p;
    public static String burl;
    private static String dashData;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initParams();
        // takagen99 : Initialize Locale
        initLocale();
        // OKGo
        OkGoHelper.init();
        // Get EPG Info
        EpgUtil.init();
        // 初始化Web服务器
        ControlManager.init(this);
        //初始化数据库
        AppDataManager.init();
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .commit();
        AutoSizeConfig.getInstance().setCustomFragment(true).getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();

        // Delete Cache
        /*File dir = getCacheDir();
        FileUtils.recursiveDelete(dir);
        dir = getExternalCacheDir();
        FileUtils.recursiveDelete(dir);*/

        FileUtils.cleanPlayerCache();

        // Add JS support
        QuickJSLoader.init();

        //初始化更新
        initUpdate();
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(instance.getExternalCacheDir().getAbsolutePath());
            }
            return p;
        } catch (Exception e) {
            LOG.e(e.toString());
            return null;
        }
    }


    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        // 首页选项
        putDefault(HawkConfig.HOME_SHOW_SOURCE, true);       //数据源显示: true=开启, false=关闭
        putDefault(HawkConfig.HOME_SEARCH_POSITION, false);  //按钮位置-搜索: true=上方, false=下方
        putDefault(HawkConfig.HOME_MENU_POSITION, true);     //按钮位置-设置: true=上方, false=下方
        putDefault(HawkConfig.HOME_REC, 2);                  //推荐: 0=豆瓣热播, 1=站点推荐, 2=观看历史
        putDefault(HawkConfig.HOME_NUM, 4);                  //历史条数: 0=20条, 1=40条, 2=60条, 3=80条, 4=100条
        // 播放器选项
        putDefault(HawkConfig.SHOW_PREVIEW, true);           //窗口预览: true=开启, false=关闭
        putDefault(HawkConfig.PLAY_SCALE, 0);                //画面缩放: 0=默认, 1=16:9, 2=4:3, 3=填充, 4=原始, 5=裁剪
        putDefault(HawkConfig.BACKGROUND_PLAY_TYPE, 0);      //后台：0=关闭, 1=开启, 2=画中画
        putDefault(HawkConfig.PLAY_TYPE, 1);                 //播放器: 0=系统, 1=IJK, 2=Exo, 3=MX, 4=Reex, 5=Kodi
        putDefault(HawkConfig.IJK_CODEC, "硬解码");           //IJK解码: 软解码, 硬解码
        // 系统选项
        putDefault(HawkConfig.HOME_LOCALE, 0);               //语言: 0=中文, 1=英文
        putDefault(HawkConfig.THEME_SELECT, 0);              //主题: 0=奈飞, 1=哆啦, 2=百事, 3=鸣人, 4=小黄, 5=八神, 6=樱花
        putDefault(HawkConfig.SEARCH_VIEW, 1);               //搜索展示: 0=文字列表, 1=缩略图
        putDefault(HawkConfig.PARSE_WEBVIEW, true);          //嗅探Webview: true=系统自带, false=XWalkView
        putDefault(HawkConfig.DOH_URL, 0);                   //安全DNS: 0=关闭, 1=腾讯, 2=阿里, 3=360, 4=Google, 5=AdGuard, 6=Quad9
    }
    /**
     * 初始化更新组件服务
     */
    private void initUpdate() {
        XUpdate.get()
                .debug(false)
                //默认设置只在wifi下检查版本更新
                .isWifiOnly(false)
                //默认设置使用get请求检查版本
                .isGet(true)
                //默认设置非自动模式，可根据具体使用配置
                .isAutoMode(false)
//                .setApkCacheDir("/storage/sdcard0/Android/data/ta.hai/files")
                .setApkCacheDir(getDiskCachePath(instance))
                //设置默认公共请求参数
                .param("VersionCode", UpdateUtils.getVersionCode(this))
                .param("VersionName", getPackageName())
                //设置版本更新出错的监听
                .setOnUpdateFailureListener(new OnUpdateFailureListener() {
                    @Override
                    public void onFailure(UpdateError error) {
                        error.printStackTrace();
                        // 对不同错误进行处理
//                        if (error.getCode() != CHECK_NO_NEW_VERSION) {
////                            ToastUtils.showShort(application,error.toString() + "");
//                        }
                        updateString(error);
                    }
                })
                //设置是否支持静默安装，默认是true
                .supportSilentInstall(true)
                //这个必须设置！实现网络请求功能。
                .setIUpdateHttpService(new UpdateHttpService())
                //这个必须初始化
                .init(this);
    }
    /**
     * 获取cache路径
     */
    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    private void initLocale() {
        if (Hawk.get(HawkConfig.HOME_LOCALE, 0) == 0) {
            LocaleHelper.setLocale(App.this, "zh");
        } else {
            LocaleHelper.setLocale(App.this, "");
        }
    }

    public static App getInstance() {
        return instance;
    }

    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.load();
    }

    public void setDashData(String data) {
        dashData = data;
    }
    public String getDashData() {
        return dashData;
    }
    public void updateString(UpdateError error) {
        switch (error.getCode()) {
            case 2000:
                // ToastUtils.showShort("查询更新失败");
                Toast.makeText(this, getString(R.string.update_code_2000), Toast.LENGTH_SHORT).show();
                break;
            case 2001:
                // ToastUtils.showShort( "没有wifi");
                Toast.makeText(this, getString(R.string.update_code_2001), Toast.LENGTH_SHORT).show();
                break;
            case 2002:
                // ToastUtils.showShort("没有网络");
                Toast.makeText(this, getString(R.string.update_code_2001), Toast.LENGTH_SHORT).show();
                break;
            case 2003:
                // ToastUtils.showShort( "正在进行版本更新");
                Toast.makeText(this, getString(R.string.update_code_2003), Toast.LENGTH_SHORT).show();
                break;
            case 2004:
                // ToastUtils.showShort( "无最新版本");
                Toast.makeText(this, getString(R.string.update_code_2004), Toast.LENGTH_SHORT).show();
                break;
            case 2005:
                // ToastUtils.showShort( "版本检查返回空");
                Toast.makeText(this, getString(R.string.update_code_2005), Toast.LENGTH_SHORT).show();
                break;
            case 2006:
                // ToastUtils.showShort( "版本检查返回json解析失败");
                Toast.makeText(this, getString(R.string.update_code_2006), Toast.LENGTH_SHORT).show();
                break;
            case 2007:
                // ToastUtils.showShort( "已经被忽略的版本");
                Toast.makeText(this, getString(R.string.update_code_2007), Toast.LENGTH_SHORT).show();
                break;
            case 2008:
                // ToastUtils.showShort( "应用下载的缓存目录为空");
                Toast.makeText(this, getString(R.string.update_code_2008), Toast.LENGTH_SHORT).show();
                break;
            case 3000:
                // ToastUtils.showShort( "版本提示器异常错误");
                Toast.makeText(this, getString(R.string.update_code_3000), Toast.LENGTH_SHORT).show();
                break;
            case 3001:
                // ToastUtils.showShort( "版本提示器所在Activity页面被销毁");
                Toast.makeText(this, getString(R.string.update_code_3001), Toast.LENGTH_SHORT).show();
                break;
            case 4000:
                // ToastUtils.showShort( "新应用安装包下载失败");
                Toast.makeText(this, getString(R.string.update_code_4000), Toast.LENGTH_SHORT).show();
                break;
            case 5000:
                // ToastUtils.showShort( "apk安装失败");
                Toast.makeText(this, getString(R.string.update_code_5000), Toast.LENGTH_SHORT).show();
                break;
            case 5100:
                // ToastUtils.showShort( "未知错误");
                Toast.makeText(this, getString(R.string.update_code_5100), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}