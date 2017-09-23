package com.dhh.progressmanager;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.dhh.progress.bean.OkhttpProgressException;
import com.dhh.progress.bean.ProgressInfo;
import com.dhh.progress.manager.ProgressCallback;
import com.dhh.progress.manager.ProgressRxJavaCallAdapterFactory;
import com.dhh.progress.manager.RxProgressManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String fileDirPath = Environment.getExternalStorageDirectory() + "/RxProgressManager";
        File fileDir = new File(fileDirPath);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        try {
            writeToDisk(getAssets().open("file"), fileDirPath + "/file2");
        } catch (IOException e) {
            e.printStackTrace();
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {

                Log.d("MainActivity", message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        builder.addNetworkInterceptor(loggingInterceptor);
        client = RxProgressManager.getInstance().build(builder);


        String url = "http://img01.sogoucdn.com/app/a/100540002/478534.jpg";
        String url2 = "https://timgsa.badddidu.com/timg?image&quality=80&size=b9999_10000&sec=150571253171" +
                "6&di=8de8ec66a34b9d092996a2a4238ee05f&imgtype=0&src=http%3A%2F%2Fbizhi.zhuoku.com%2F2013%2F05%2F01%2Fchichi%2Fchichi08.jpg";
        String url3 = "http://f5.market.mi-img.com/download/AppStore/0ef745dfdef0157c80cd3eb64ecd36a9f17403341/com.tencent.mobileqq.apk";
        String url4 = "http://f1.market.xiaomi.com/download/AppStore/0fbc485c256564ea73a70e59e7b6602a7440f6043/com.miui.calculator.apk";
        String uploadUrl = "http://api.nohttp.net/upload";

        String redirectUrl = "https://scrapy.org/doc";

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://api.nohttp.net/")
                .addCallAdapterFactory(ProgressRxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        Api api = retrofit.create(Api.class);
        String path = fileDirPath + "/file2";
        File file = new File(path);
//        api.upload(RequestBody.create(MultipartBody.FORM, file)).subscribe();

//        api.get(redirectUrl).subscribe();
//        download(url, "url");
//        download(url2, "url");
//        download(url3, "url");
        download(redirectUrl, "url");
//        upload(uploadUrl, "url");
        toObservableProgress(redirectUrl);
//        toObservableProgress(url2);
//        toObservableProgress(url3);
//        toObservableProgress(url4);
//        toObservableProgress(url4);
//        toObservableProgress(uploadUrl);
    }

    public void toObservableProgress(final String url) {
        final String name = url.substring(url.lastIndexOf("/"));

        RxProgressManager.getInstance().toObservableProgress(url)
                .subscribe(new Subscriber<ProgressInfo>() {
                    @Override
                    public void onCompleted() {
                        Log.d("MainActivity", name + "=====onCompleted:");
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof OkhttpProgressException) {
                            OkhttpProgressException exception = (OkhttpProgressException) e;
                            Log.e("MainActivity", "e:" + exception);
                        }
                    }

                    @Override
                    public void onNext(ProgressInfo progressInfo) {
                        Log.d("MainActivity", name + ": progressInfo.getPercent():" + progressInfo.getPercent());
                    }
                });
    }

    public void download(final String url, final String tagType) {
        Request request = new Request.Builder().get().url(url).addHeader(RxProgressManager.PROGRESS_TAG, tagType).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                OkhttpProgressException okhttpProgressException = new OkhttpProgressException(RxProgressManager.getProgressKey(call.request()), e);
                RxProgressManager.getInstance().postProgressError(okhttpProgressException);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                String path = Environment.getExternalStorageDirectory() + "/RxProgressManager" + url.substring(url.lastIndexOf("/"));
                File file = new File(new File(path).getParent());
                if (!file.exists()) {
                    file.mkdirs();
                }
                Log.d("MainActivity", path);
                assert body != null;
                writeToDisk(body.byteStream(), path);
            }
        });
    }

    public void upload(final String url, final String tagType) {
        String path = Environment.getExternalStorageDirectory() + "/RxProgressManager" + "/file";
        File file = new File(path);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        Request request = new Request.Builder()
                .url(url)
                .addHeader(RxProgressManager.PROGRESS_TAG, tagType)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new ProgressCallback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("MainActivity", response.toString());
            }
        });
    }

    public static boolean writeToDisk(InputStream is, String path) throws IOException {
        try {
            File file = new File(path);
            OutputStream os;
            byte[] reader = new byte[1024 * 4];
            os = new FileOutputStream(file);
            int read;
            while ((read = is.read(reader)) != -1) {
                os.write(reader, 0, read);
            }
            os.flush();
            is.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
