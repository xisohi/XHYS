package com.github.tvbox.osc.update

import com.github.tvbox.osc.util.OkGoHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


//用于更新用的
object GithubProxy {

    private var okHttpClient: OkHttpClient? = null

    @JvmStatic
    private val GITHUB_URL = "https://github.com/"

    @JvmStatic
    private val PROXY_LIST: List<String> = listOf(
        "https://sciproxy.com/",
        "https://gh.idayer.com/",
        "https://mirror.ghproxy.com/",
        "https://gh-proxy.com/",
        "https://ghproxy.org/",
        "https://mirrors.chenby.cn/"

    )

    private fun initHttpClient() {
        if (okHttpClient == null) {
            okHttpClient =
                OkGoHelper.getDefaultClient().newBuilder().writeTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS).connectTimeout(2, TimeUnit.SECONDS).build()
        }
    }

    private fun isWebsiteAccessible(urlString: String): Boolean {
        initHttpClient()
        val request: Request = Request.Builder().url(urlString).build()

        try {
            okHttpClient?.newCall(request)?.execute().use { response ->
                return response?.isSuccessful ?: false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


    /**
     * 需要在子线程运行
     * @param [download]
     * @return [String]
     */
    @JvmStatic
    fun getGithubProxyUrl(download: String): String {
        //如果能直接访问
        val websiteAccessible = isWebsiteAccessible(GITHUB_URL)
        if (websiteAccessible) {
            return download;
        } else {
            PROXY_LIST.forEach {
                if (isWebsiteAccessible(it)) {
                    return it + download
                }
            }
        }
        return download
    }
}
