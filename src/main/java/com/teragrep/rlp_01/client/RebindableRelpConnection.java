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

import com.teragrep.rlp_01.RelpBatch;

import java.io.IOException;

public class RebindableRelpConnection implements IManagedRelpConnection {
    private final IManagedRelpConnection managedRelpConnection;
    private int recordsSent;
    private final int rebindRequestAmount;

    public RebindableRelpConnection(IManagedRelpConnection managedRelpConnection, int rebindRequestAmount) {
        this.managedRelpConnection = managedRelpConnection;
        this.recordsSent = 0;
        this.rebindRequestAmount = rebindRequestAmount;
    }

    @Override
    public void reconnect() {
        managedRelpConnection.reconnect();
    }

    @Override
    public void connect() throws IOException {
        managedRelpConnection.connect();
    }

    @Override
    public void forceReconnect() {
        managedRelpConnection.forceReconnect();
    }

    @Override
    public void ensureSent(byte[] bytes) {
        RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(bytes);
        ensureSent(relpBatch);
    }

    @Override
    public void ensureSent(RelpBatch relpBatch) {
        if (recordsSent >= rebindRequestAmount) {
            reconnect();
            recordsSent = 0;
        }
        int batchLength = relpBatch.getWorkQueueLength();
        managedRelpConnection.ensureSent(relpBatch);
        recordsSent = recordsSent + batchLength;
    }

    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public void close() throws IOException {
        managedRelpConnection.close();
    }
}
