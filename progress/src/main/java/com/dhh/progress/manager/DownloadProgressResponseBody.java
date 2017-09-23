package com.dhh.progress.manager;

import com.dhh.progress.bean.ProgressInfo;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by dhh on 2017/9/14.
 */
public class DownloadProgressResponseBody extends ResponseBody {

    private String key;
    private ProgressInfo progressInfo;
    private ResponseBody responseBody;
    private BufferedSource bufferedSource;
    public static final Map<String, ProgressInfo> PROGRESS_INFO_MAP = new WeakHashMap<>();

    public DownloadProgressResponseBody(String key, ResponseBody responseBody) {
        this.key = key;
        this.responseBody = responseBody;
        progressInfo = PROGRESS_INFO_MAP.get(key);
        if (progressInfo == null) {
            progressInfo = new ProgressInfo(key);
            PROGRESS_INFO_MAP.put(key, progressInfo);
        } else {
            progressInfo.reset();
        }
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                progressInfo.setKey(key)
                        .setBytesRead(totalBytesRead)
                        .setContentLength(responseBody.contentLength())
                        .setDone(bytesRead == -1);
                RxProgressManager.getInstance().postProgressInfo(progressInfo);
                return bytesRead;
            }
        };
    }
}
