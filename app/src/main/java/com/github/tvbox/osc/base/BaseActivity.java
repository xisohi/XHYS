package com.github.tvbox.osc.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.blankj.utilcode.util.ActivityUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LocaleHelper;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import me.jessyan.autosize.AutoSizeCompat;
import me.jessyan.autosize.internal.CustomAdapt;
import xyz.doikki.videoplayer.util.CutoutUtil;

public abstract class BaseActivity extends AppCompatActivity implements CustomAdapt {
    protected Context mContext;
    private LoadService mLoadService;
    private static float screenRatio = -100.0f;
    private static final String WALLPAPER_URL = "wallpaper_url";
    private static BitmapDrawable globalWp = null;

    @Override
    protected void attachBaseContext(Context base) {
        Context newBase = base;
        if (App.viewPump != null) {
            newBase = ViewPumpContextWrapper.wrap(base, App.viewPump);
        }
        if (Hawk.get(HawkConfig.HOME_LOCALE, 0) == 0) {
            super.attachBaseContext(LocaleHelper.onAttach(newBase, "zh"));
        } else {
            super.attachBaseContext(LocaleHelper.onAttach(newBase, ""));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            if (screenRatio < 0) {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int screenWidth = dm.widthPixels;
                int screenHeight = dm.heightPixels;
                screenRatio = (float) Math.max(screenWidth, screenHeight) / (float) Math.min(screenWidth, screenHeight);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // takagen99 : Set Theme Color
        if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 0) {
            setTheme(R.style.NetfxTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 1) {
            setTheme(R.style.DoraeTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 2) {
            setTheme(R.style.PepsiTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 3) {
            setTheme(R.style.NarutoTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 4) {
            setTheme(R.style.MinionTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 5) {
            setTheme(R.style.YagamiTheme);
        } else {
            setTheme(R.style.SakuraTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(getLayoutResID());
        mContext = this;
        CutoutUtil.adaptCutoutAboveAndroidP(mContext, true);
        AppManager.getInstance().addActivity(this);
        init();
        setScreenOn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(true);
        changeWallpaper(false);
    }

    public void changeWallpaper(boolean force) {
        if (!force && globalWp != null) {
            Log.d("BaseActivity", "Using cached wallpaper.");
            getWindow().setBackgroundDrawable(globalWp);
            return;
        }

        // 获取纯 URL
        String wallpaperUrl = Hawk.get(HawkConfig.WALLPAPER_URL, "https://xhys.lcjly.cn/image/bg.jpg");
        Log.d("BaseActivity", "Wallpaper URL from Hawk: " + wallpaperUrl);

        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.w("BaseActivity", "No network connection available. Using default wallpaper.");
            useDefaultWallpaper();
            return;
        }

        // 在后台线程加载网络图片
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(wallpaperUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000); // 设置连接超时时间
                connection.setReadTimeout(5000); // 设置读取超时时间
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BitmapDrawable drawable = new BitmapDrawable(getResources(), BitmapFactory.decodeStream(inputStream));
                    runOnUiThread(() -> {
                        globalWp = drawable;
                        getWindow().setBackgroundDrawable(globalWp);
                        Log.d("BaseActivity", "Wallpaper loaded successfully from network.");
                    });
                } else {
                    Log.e("BaseActivity", "Failed to load wallpaper from network. Response code: " + responseCode);
                    runOnUiThread(() -> {
                        Log.w("BaseActivity", "Failed to load wallpaper from network. Using default.");
                        useDefaultWallpaper();
                    });
                }
            } catch (Exception e) {
                Log.e("BaseActivity", "Failed to load wallpaper from network: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Log.w("BaseActivity", "Failed to load wallpaper from network. Using default.");
                    useDefaultWallpaper();
                });
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void useDefaultWallpaper() {
        if (globalWp == null) {
            globalWp = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.app_bg));
        }
        getWindow().setBackgroundDrawable(globalWp);
    }

    protected abstract int getLayoutResID();
    protected abstract void init();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().finishActivity(this);
    }

    public void setScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void hideSystemUI(boolean shownavbar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            if (!shownavbar) {
                uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }
            getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
        }
    }

    public void hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            //    uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            //    uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    public void vidHideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    public void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            uiVisibility &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
        }
    }

    @Override
    public Resources getResources() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AutoSizeCompat.autoConvertDensityOfCustomAdapt(super.getResources(), this);
        }
        return super.getResources();
    }

    public boolean hasPermission(String permission) {
        boolean has = true;
        try {
            has = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return has;
    }

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    protected void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    protected void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz) {
        Intent intent = new Intent(mContext, clazz);
        startActivity(intent);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz, Bundle bundle) {
        if (DetailActivity.class.isAssignableFrom(clazz) && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            ActivityUtils.finishActivity(DetailActivity.class);
        }
        Intent intent = new Intent(mContext, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    protected String getAssetText(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assets = getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assets.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public float getSizeInDp() {
        return isBaseOnWidth() ? 1280 : 720;
    }

    @Override
    public boolean isBaseOnWidth() {
        return !(screenRatio >= 4.0f);
    }

    public boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public boolean supportsTouch() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }

    public void setScreenBrightness(float amt) {
        WindowManager.LayoutParams lparams = getWindow().getAttributes();
        lparams.screenBrightness = amt;
        getWindow().setAttributes(lparams);
    }

    public void setScreenOff() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public int getThemeColor() {
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.themeColor);
        int themeColor = a.getColor(R.styleable.themeColor_color_theme, 0);
        return themeColor;
    }
}