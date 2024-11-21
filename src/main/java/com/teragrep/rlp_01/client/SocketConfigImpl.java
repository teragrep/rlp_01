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

public class SocketConfigImpl implements SocketConfig {
    private final int readTimeout;
    private final int writeTimeout;
    private final int connectTimeout;
    private final boolean keepAlive;

    public SocketConfigImpl(int readTimeout, int writeTimeout, int connectTimeout, boolean keepAlive) {
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.connectTimeout = connectTimeout;
        this.keepAlive = keepAlive;
    }

    @Override
    public int readTimeout() {
        return readTimeout;
    }

    @Override
    public int writeTimeout() {
        return writeTimeout;
    }

    @Override
    public int connectTimeout() {
        return connectTimeout;
    }

    @Override
    public boolean keepAlive() {
        return keepAlive;
    }
}
