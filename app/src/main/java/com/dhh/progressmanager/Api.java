package com.dhh.progressmanager;

import com.dhh.progress.manager.RxProgressManager;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by dhh on 2017/9/18.
 */

public interface Api {
    @Headers(RxProgressManager.PROGRESS_TAG_URL)
    @POST("/upload")
    Observable<ResponseBody> upload(@Body RequestBody body);

    @Headers(RxProgressManager.PROGRESS_TAG_URL)
    @GET
    Observable<ResponseBody> get(@Url String url);
}
