package com.teragrep.rlp_01.pool;

public class RelpConfig {
    public final String relpTarget;
    public final int relpPort;
    public final int relpReconnectInterval;
    public final int rebindRequestAmount;
    public final boolean rebindEnabled;

    public RelpConfig(String relpTarget, int relpPort, int relpReconnectInterval, int rebindRequestAmount, boolean rebindEnabled) {
        this.relpTarget = relpTarget;
        this.relpPort = relpPort;
        this.relpReconnectInterval = relpReconnectInterval;
        this.rebindRequestAmount = rebindRequestAmount;
        this.rebindEnabled = rebindEnabled;
    }
}
