package com.dhh.progress.manager;


import com.dhh.progress.bean.ProgressInfo;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

import static com.dhh.progress.manager.DownloadProgressResponseBody.PROGRESS_INFO_MAP;

/**
 * Created by dhh on 2017/9/14.
 */

public class UploadProgressRequestBody extends RequestBody {
    private String key;
    private RequestBody requestBody;
    private BufferedSink bufferedSink;
    private ProgressInfo progressInfo;

    public UploadProgressRequestBody(String key, RequestBody requestBody) {
        this.key = key;
        this.requestBody = requestBody;
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
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
            //包装
            bufferedSink = Okio.buffer(sink(sink));
        }
        //写入
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    /**
     * 写入，post进度
     *
     * @param sink Sink
     * @return Sink
     */
    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                //增加当前写入的字节数
                bytesWritten += byteCount;
                //回调
                progressInfo.setKey(key)
                        .setBytesRead(bytesWritten)
                        .setContentLength(contentLength)
                        .setDone(bytesWritten == contentLength);
                RxProgressManager.getInstance().postProgressInfo(progressInfo);
            }
        };
    }
}
