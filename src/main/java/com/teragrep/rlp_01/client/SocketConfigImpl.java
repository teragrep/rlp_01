package com.teragrep.rlp_01.client;

public class SocketConfigImpl implements SocketConfig {
    private final int readTimeout;
    private final int writeTimeout;
    private final int connectTimeout;
    private final boolean keepAlive;

    public SocketConfigImpl(int readTimeout, int writeTimeout, int connectTimeout, boolean keepAlive) {
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.connectTimeout = connectTimeout;
        this.keepAlive = keepAlive;
    }

    @Override
    public int readTimeout() {
        return readTimeout;
    }

    @Override
    public int writeTimeout() {
        return writeTimeout;
    }

    @Override
    public int connectTimeout() {
        return connectTimeout;
    }

    @Override
    public boolean keepAlive() {
        return keepAlive;
    }
}
