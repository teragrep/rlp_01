package com.teragrep.rlp_01.client;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;

public class RenewableRelpConnection implements IManagedRelpConnection {

    private final IManagedRelpConnection managedRelpConnection;
    private final Duration maxIdle;
    private Instant lastAccess;

    public RenewableRelpConnection(IManagedRelpConnection managedRelpConnection, Duration maxIdle) {
        this.managedRelpConnection = managedRelpConnection;
        this.maxIdle = maxIdle;
        this.lastAccess = Instant.ofEpochSecond(0);
    }

    @Override
    public void connect() throws IOException {
        lastAccess = Instant.now();
        managedRelpConnection.connect();
    }

    @Override
    public void forceReconnect() {
        managedRelpConnection.forceReconnect();
    }

    @Override
    public void ensureSent(byte[] bytes) {
        if (lastAccess.plus(maxIdle).isAfter(Instant.now())) {
            forceReconnect();
        }
        lastAccess = Instant.now();
        managedRelpConnection.ensureSent(bytes);
    }

    @Override
    public boolean isStub() {
        return managedRelpConnection.isStub();
    }

    @Override
    public void close() throws IOException {
        managedRelpConnection.close();
    }
}
