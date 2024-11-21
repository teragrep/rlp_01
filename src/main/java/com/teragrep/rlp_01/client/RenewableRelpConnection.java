/*
   Java Reliable Event Logging Protocol Library RLP-01
   Copyright (C) 2021-2024  Suomen Kanuuna Oy

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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
    public void reconnect() {
        lastAccess = Instant.now();
        managedRelpConnection.reconnect();
    }

    @Override
    public void connect() throws IOException {
        lastAccess = Instant.now();
        managedRelpConnection.connect();
    }

    @Override
    public void forceReconnect() {
        lastAccess = Instant.now();
        managedRelpConnection.forceReconnect();
    }

    @Override
    public void ensureSent(byte[] bytes) {
        if (lastAccess.plus(maxIdle).isBefore(Instant.now())) {
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
