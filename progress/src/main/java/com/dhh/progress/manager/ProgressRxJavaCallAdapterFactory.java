package com.dhh.progress.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.dhh.progress.bean.OkhttpProgressException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by dhh on 2017/9/19.
 */

public class ProgressRxJavaCallAdapterFactory extends CallAdapter.Factory {

    private CallAdapter.Factory adapterFactory;

    private ProgressRxJavaCallAdapterFactory() {
        adapterFactory = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
    }

    public static ProgressRxJavaCallAdapterFactory create() {

        return new ProgressRxJavaCallAdapterFactory();
    }

    @Nullable
    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull Retrofit retrofit) {
        return new RxJavaCallAdapter<>(adapterFactory.get(returnType, annotations, retrofit));
    }

    private static final class RxJavaCallAdapter<R> implements CallAdapter<R, Observable<?>> {
        private CallAdapter<R, ?> callAdapter;

        public RxJavaCallAdapter(CallAdapter<R, ?> callAdapter) {
            this.callAdapter = callAdapter;
        }

        @Override
        public Type responseType() {
            return callAdapter.responseType();
        }

        @Override
        public Observable<?> adapt(final Call<R> call) {
            Observable observable = (Observable) callAdapter.adapt(call);
            return observable.onErrorResumeNext(new Func1<Throwable, Observable>() {
                @Override
                public Observable call(Throwable throwable) {
                    //这里可以做异常统一处理
                    return Observable.error(throwable);
                }
            }).doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    String tag = call.request().header(RxProgressManager.PROGRESS_TAG);
                    if (!TextUtils.isEmpty(tag)) {
                        OkhttpProgressException exception = new OkhttpProgressException(RxProgressManager.getProgressKey(call.request()), throwable);
                        RxProgressManager.getInstance().postProgressError(exception);
                    }
                }
            }).subscribeOn(Schedulers.io());
        }


    }
}
