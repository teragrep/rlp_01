package com.teragrep.rlp_01.client;

import java.time.Duration;

public class RelpConfig {
    public final String relpTarget;
    public final int relpPort;
    public final int relpReconnectInterval;
    public final int rebindRequestAmount;
    public final boolean rebindEnabled;
    public final Duration maxIdle;
    public final boolean maxIdleEnabled;

    public RelpConfig(String relpTarget, int relpPort, int relpReconnectInterval, int rebindRequestAmount, boolean rebindEnabled, Duration maxIdle, boolean maxIdleEnabled) {
        this.relpTarget = relpTarget;
        this.relpPort = relpPort;
        this.relpReconnectInterval = relpReconnectInterval;
        this.rebindRequestAmount = rebindRequestAmount;
        this.rebindEnabled = rebindEnabled;
        this.maxIdle = maxIdle;
        this.maxIdleEnabled = maxIdleEnabled;
    }
}
