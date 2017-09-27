package com.dhh.progress.bean;

/**
 * Created by dhh on 2017/9/11.
 */

public class ProgressInfo {
    private String key;
    private long bytesRead;
    private long contentLength;
    private boolean done;
    private OkhttpProgressException okhttpProgressException;

    public ProgressInfo() {
    }

    public ProgressInfo(String key) {
        this.key = key;
    }

    public ProgressInfo(String key, long bytesRead, long contentLength, boolean done) {
        this.key = key;
        this.bytesRead = bytesRead;
        this.contentLength = contentLength;
        this.done = done;
    }

    public ProgressInfo(String key, OkhttpProgressException okhttpProgressException) {
        this.key = key;
        this.okhttpProgressException = okhttpProgressException;
    }

    public ProgressInfo reset() {
        setBytesRead(0);
        setContentLength(0);
        setDone(false);
        setKey(null);
        okhttpProgressException = null;
        return this;
    }

    public String getKey() {
        return key;
    }

    public ProgressInfo setKey(String key) {
        this.key = key;
        return this;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public ProgressInfo setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
        return this;
    }

    public long getContentLength() {
        return contentLength;
    }

    public ProgressInfo setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public boolean isDone() {
        return done;
    }

    public ProgressInfo setDone(boolean done) {
        this.done = done;
        return this;
    }

    public OkhttpProgressException getOkhttpProgressException() {
        return okhttpProgressException;
    }

    public void setOkhttpProgressException(OkhttpProgressException e) {
        this.okhttpProgressException = e;
    }

    public boolean isException() {
        return okhttpProgressException != null;
    }

    /**
     * 获取进度百分比
     *
     * @return [0.0, 1.0]
     */
    public float getPercent() {
        float percent = ((int) (bytesRead * 1.0F / contentLength * 10000)) / 10000F;
        if (percent < 0.005F) {
            percent = 0F;
        }
        return percent;
    }
}
