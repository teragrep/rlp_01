package com.teragrep.rlp_01.client;

public class SocketConfigDefault implements SocketConfig {
    @Override
    public int readTimeout() {
        return 0;
    }

    @Override
    public int writeTimeout() {
        return 0;
    }

    @Override
    public int connectTimeout() {
        return 0;
    }

    @Override
    public boolean keepAlive() {
        return false;
    }
}
