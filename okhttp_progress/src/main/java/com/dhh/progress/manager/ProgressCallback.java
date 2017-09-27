package com.dhh.progress.manager;

import android.support.annotation.CallSuper;
import android.text.TextUtils;

import com.dhh.progress.bean.OkhttpProgressException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by dhh on 2017/9/18.
 */

public abstract class ProgressCallback implements Callback {

    @CallSuper
    @Override
    public void onFailure(Call call, IOException e) {
        String tag = call.request().header(RxProgressManager.PROGRESS_TAG);
        if (!TextUtils.isEmpty(tag)) {
            RxProgressManager.getInstance().postProgressError(new OkhttpProgressException(RxProgressManager.getProgressKey(call.request()), e));
        }
    }
}
