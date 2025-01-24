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

public class ManagedRelpConnectionStub implements IManagedRelpConnection {

    @Override
    public void reconnect() {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }

    @Override
    public void connect() throws IOException {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }

    @Override
    public void forceReconnect() {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }

    @Override
    public void ensureSent(byte[] bytes) {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }

    @Override
    public void ensureSent(RelpBatch relpBatch) {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }

    @Override
    public boolean isStub() {
        return true;
    }

    @Override
    public void close() {
        throw new IllegalStateException("ManagedRelpConnectionStub does not support this");
    }
}
