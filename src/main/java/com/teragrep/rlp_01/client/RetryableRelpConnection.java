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

/**
 * Decorator that retries both connect() and ensureSent() configured number of times (with an optional configured delay)
 * if the delegate connection throws an exception from these methods. Only throws when configured number of retries is
 * exceeded
 */
public class RetryableRelpConnection implements IManagedRelpConnection {

    private final IManagedRelpConnection relpConnection;

    public RetryableRelpConnection(final IManagedRelpConnection relpConnection) {
        this.relpConnection = relpConnection;
    }

    @Override
    public long ensureSent(final RelpBatch relpBatch) throws RelpCommitException {
        return relpConnection.ensureSent(relpBatch);
    }

    @Override
    public long ensureSent(final byte[] bytes) throws RelpCommitException {
        return relpConnection.ensureSent(bytes);
    }

    @Override
    public long connect() throws RelpConnectionException {
        return relpConnection.connect();
    }

    @Override
    public void close() throws IOException {
        relpConnection.close();
    }

    @Override
    public void tearDown() {
        relpConnection.tearDown();
    }

    @Override
    public boolean isStub() {
        return relpConnection.isStub();
    }

}
