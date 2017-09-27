package com.dhh.progress.manager;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.dhh.progress.bean.OkhttpProgressException;
import com.dhh.progress.bean.ProgressInfo;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * Created by dhh on 2017/9/11.
 */

public class RxProgressManager {
    public static final String PROGRESS_TAG = "progress_tag";
    /**
     * example: http://10.7.5.88:8080/gs-robot/data/map_png?map_name=gs_7;
     */
    public static final String PROGRESS_TAG_URL = "progress_tag: url";
    /**
     * example: /gs-robot/data/map_png?map_name=gs_7;
     */
    public static final String PROGRESS_TAG_FILE = "progress_tag: file";
    /**
     * example: /gs-robot/data/map_png;
     */
    public static final String PROGRESS_TAG_PATH = "progress_tag: path";
    /**
     * example: map_name=gs_7;
     */
    public static final String PROGRESS_TAG_QUERY = "progress_tag: query";


    private static RxProgressManager instance;
    private final ProgressInterceptor progressInterceptor;
    private final RedirectInterceptor redirectInterceptor;
    private SerializedSubject<ProgressInfo, ProgressInfo> bus;

    private RxProgressManager() {
        try {
            Class.forName("okhttp3.OkHttpClient");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency okhtt");
        }
        try {
            Class.forName("rx.Observable");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxjava 1");
        }
        try {
            Class.forName("rx.android.schedulers.AndroidSchedulers");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxandroid 1");
        }
        bus = new SerializedSubject<>(PublishSubject.<ProgressInfo>create());
        progressInterceptor = new ProgressInterceptor();
        redirectInterceptor = new RedirectInterceptor();
    }

    public static RxProgressManager getInstance() {
        if (instance == null) {
            synchronized (RxProgressManager.class) {
                if (instance == null) {
                    instance = new RxProgressManager();
                }
            }
        }
        return instance;
    }

    /**
     * 广播进度信息
     *
     * @param downInfo
     */
    void postProgressInfo(ProgressInfo downInfo) {
        bus.onNext(downInfo);
    }

    /**
     * 广播错误通知
     *
     * @param e
     */
    public void postProgressError(OkhttpProgressException e) {
        bus.onNext(new ProgressInfo(e.getKey(), e));
    }

    /**
     * 监听进度 download/upload
     *
     * @param key key
     * @return
     */
    public Observable<ProgressInfo> toObservableProgress(final String key) {
        return bus.asObservable()
                // 筛选出自己需要的 ProgressInfo
                .filter(new Func1<ProgressInfo, Boolean>() {
                    @Override
                    public Boolean call(ProgressInfo progressInfo) {
                        return key.equals(progressInfo.getKey());
                    }
                })
                //去抖,防止出现 MissingBackpressureException
                .throttleLast(200, TimeUnit.MILLISECONDS)
                // 过滤进度重复数据
                .distinct(new Func1<ProgressInfo, Float>() {
                    @Override
                    public Float call(ProgressInfo progressInfo) {
                        return progressInfo.getPercent();
                    }
                })
                //进度完成自动注销
                .takeUntil(new Func1<ProgressInfo, Boolean>() {
                    @Override
                    public Boolean call(ProgressInfo progressInfo) {
                        return progressInfo.isDone() || progressInfo.getPercent() == 1;
                    }
                })
                //判断是否是 OkhttpProgressException
                .doOnNext(new Action1<ProgressInfo>() {
                    @Override
                    public void call(ProgressInfo progressInfo) {
                        if (progressInfo.isException()) {
                            throw progressInfo.getOkhttpProgressException();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public OkHttpClient build(OkHttpClient.Builder builder) {
        return builder(builder).build();
    }

    public OkHttpClient.Builder builder(OkHttpClient.Builder builder) {
        // 防止对同一个builder多次添加进度拦截器
        if (builder.networkInterceptors().contains(progressInterceptor)) {
            return builder;
        }
        return builder.addNetworkInterceptor(redirectInterceptor)
                .addNetworkInterceptor(progressInterceptor);
    }

    private static class ProgressInterceptor implements Interceptor {

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request originalRequest = chain.request();
            //获取是否需要进度监听的标志
            String tag = originalRequest.header(PROGRESS_TAG);
            if (TextUtils.isEmpty(tag)) {
                return chain.proceed(originalRequest);
            }

            String key = getProgressKey(originalRequest);
            Request.Builder builder = originalRequest.newBuilder()
                    .removeHeader(PROGRESS_TAG)
                    // fix: unexpected end of stream exception
                    .header("Connection", "close");
            if (originalRequest.body() != null) {
                //说明是上传
                Request newRequest = builder.post(new UploadProgressRequestBody(key, originalRequest.body())).build();
                return chain.proceed(newRequest);
            }
            //GET request 下载
            Response originalResponse = chain.proceed(builder.build());
            return originalResponse.newBuilder()
                    .body(new DownloadProgressResponseBody(key, originalResponse.body()))
                    // fix: unexpected end of stream exception
                    .header("Connection", "close")
                    .build();
        }
    }


    private static final class RedirectInterceptor implements Interceptor {
        //重定向集合数据
        private Map<String, String> locatioon_key = new ArrayMap<>();

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String file = request.url().url().getFile();
            Response originalResponse = chain.proceed(request);
            //判断是否是从重定向发过来的
            if (locatioon_key.containsKey(file) && "GET".equals(request.method())) {
                DownloadProgressResponseBody body = new DownloadProgressResponseBody(locatioon_key.get(file), originalResponse.body());
                locatioon_key.remove(file);
                return originalResponse.newBuilder().body(body).build();
            }

            //重定向处理
            if (request.header(PROGRESS_TAG) != null && originalResponse.isRedirect()) {
                //location = url.getfile();重定向地址不包含主机地址
                String location = originalResponse.header("Location");
                if (!TextUtils.isEmpty(location)) {
                    locatioon_key.put(location, getProgressKey(request));
                }
            }
            return originalResponse;
        }
    }

    /**
     * 从request里 获取 key
     *
     * @param request
     * @return key
     */
    public static String getProgressKey(Request request) {
        String tag = request.header(RxProgressManager.PROGRESS_TAG);
        return getProgressKey(tag, request.url().url());
    }

    /**
     * 根据tag 生成 key 的对应策略
     *
     * @param tag 请求头中的tag
     * @param url request url
     * @return key
     */
    public static String getProgressKey(String tag, URL url) {
        if (TextUtils.isEmpty(tag)) return url.getFile();
        if (!tag.startsWith(PROGRESS_TAG)) {
            tag = PROGRESS_TAG + ": " + tag;
        }
        String key = url.toString();
        switch (tag) {
            case PROGRESS_TAG_URL:
                break;
            case PROGRESS_TAG_FILE:
                key = url.getFile();
                break;
            case PROGRESS_TAG_PATH:
                key = url.getPath();
                break;
            case PROGRESS_TAG_QUERY:
                key = url.getQuery();
                break;
        }
        return key;
    }
}
