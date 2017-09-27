package com.dhh.progress.bean;

/**
 * Created by dhh on 2017/9/18.
 */

public class OkhttpProgressException extends RuntimeException {
    private String key;

    public OkhttpProgressException(String key, Throwable cause) {
        super(cause);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
