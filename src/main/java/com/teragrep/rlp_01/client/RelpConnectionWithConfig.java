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
import com.teragrep.rlp_01.RelpConnection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RelpConnectionWithConfig implements IRelpConnection {

    private final RelpConnection relpConnection;
    private final RelpConfig relpConfig;

    public RelpConnectionWithConfig(RelpConnection relpConnection, RelpConfig relpConfig) {
        this.relpConnection = relpConnection;
        this.relpConfig = relpConfig;
    }

    @Override
    public int getReadTimeout() {
        return relpConnection.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        relpConnection.setReadTimeout(readTimeout);
    }

    @Override
    public int getWriteTimeout() {
        return relpConnection.getWriteTimeout();
    }

    @Override
    public void setWriteTimeout(int writeTimeout) {
        relpConnection.setWriteTimeout(writeTimeout);
    }

    @Override
    public int getConnectionTimeout() {
        return relpConnection.getConnectionTimeout();
    }

    @Override
    public void setConnectionTimeout(int timeout) {
        relpConnection.setConnectionTimeout(timeout);
    }

    @Override
    public void setKeepAlive(boolean on) {
        relpConnection.setKeepAlive(on);
    }

    @Override
    public int getRxBufferSize() {
        return relpConnection.getRxBufferSize();
    }

    @Override
    public void setRxBufferSize(int size) {
        relpConnection.setRxBufferSize(size);
    }

    @Override
    public int getTxBufferSize() {
        return relpConnection.getTxBufferSize();
    }

    @Override
    public void setTxBufferSize(int size) {
        relpConnection.setTxBufferSize(size);
    }

    @Override
    public boolean connect(String hostname, int port) throws IOException, IllegalStateException, TimeoutException {
        return relpConnection.connect(hostname, port);
    }

    @Override
    public void tearDown() {
        relpConnection.tearDown();
    }

    @Override
    public boolean disconnect() throws IOException, IllegalStateException, TimeoutException {
        return relpConnection.disconnect();
    }

    @Override
    public void commit(RelpBatch relpBatch) throws IOException, IllegalStateException, TimeoutException {
        relpConnection.commit(relpBatch);
    }

    @Override
    public RelpConfig relpConfig() {
        return relpConfig;
    }

}
