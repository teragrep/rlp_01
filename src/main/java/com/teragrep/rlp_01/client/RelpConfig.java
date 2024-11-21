package com.teragrep.rlp_01.client;

import java.time.Period;

public class RelpConfig {
    public final String relpTarget;
    public final int relpPort;
    public final int relpReconnectInterval;
    public final int rebindRequestAmount;
    public final boolean rebindEnabled;
    public final Period maxIdle;
    public final boolean maxIdleEnabled;

    public RelpConfig(String relpTarget, int relpPort, int relpReconnectInterval, int rebindRequestAmount, boolean rebindEnabled, Period maxIdle, boolean maxIdleEnabled) {
        this.relpTarget = relpTarget;
        this.relpPort = relpPort;
        this.relpReconnectInterval = relpReconnectInterval;
        this.rebindRequestAmount = rebindRequestAmount;
        this.rebindEnabled = rebindEnabled;
        this.maxIdle = maxIdle;
        this.maxIdleEnabled = maxIdleEnabled;
    }
}
