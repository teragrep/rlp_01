/*
* Teragrep Reliable Event Logging Protocol (RELP) Library for Java
* Copyright (C) 2021-2026 Suomen Kanuuna Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.teragrep.rlp_01.client;

import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.exceptions.RelpCommitException;
import com.teragrep.rlp_01.exceptions.RelpConnectionException;

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
    public long connect() throws RelpConnectionException {
        lastAccess = Instant.now();
        return managedRelpConnection.connect();
    }

    @Override
    public long ensureSent(byte[] bytes) throws RelpCommitException {
        RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(bytes);
        return ensureSent(relpBatch);
    }

    @Override
    public long ensureSent(RelpBatch relpBatch) throws RelpCommitException {
        if (lastAccess.plus(maxIdle).isBefore(Instant.now())) {
            tearDown();
            connect();
        }
        lastAccess = Instant.now();
        return managedRelpConnection.ensureSent(relpBatch);
    }

    @Override
    public boolean isStub() {
        return managedRelpConnection.isStub();
    }

    @Override
    public void close() throws IOException {
        managedRelpConnection.close();
    }

    @Override
    public void tearDown() {
        managedRelpConnection.tearDown();
    }
}
