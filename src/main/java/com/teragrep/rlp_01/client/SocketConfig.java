package com.teragrep.rlp_01.client;

public interface SocketConfig {
    int readTimeout();
    int writeTimeout();
    int connectTimeout();
    boolean keepAlive();
}
