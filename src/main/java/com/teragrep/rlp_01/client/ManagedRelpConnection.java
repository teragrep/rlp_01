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
import java.util.concurrent.TimeoutException;

public class ManagedRelpConnection implements IManagedRelpConnection {

    private final IRelpConnection relpConnection;
    private boolean hasConnected;

    public ManagedRelpConnection(IRelpConnection relpConnection) {
        this.relpConnection = relpConnection;
        this.hasConnected = false;
    }

    /**
     * Tries to establish a relp connection. Throws RelpConnectionException if connection fails
     *
     * @return number of attempts required to connect
     */
    @Override
    public long connect() throws RelpConnectionException {
        try {
            this.hasConnected = true;
            relpConnection.connect(relpConnection.relpConfig().relpTarget, relpConnection.relpConfig().relpPort);
        }
        catch (Exception e) {
            System.err
                    .println(
                            "Failed to connect to relp server <[" + relpConnection.relpConfig().relpTarget + "]>:<["
                                    + relpConnection.relpConfig().relpPort + "]>: <" + e.getMessage() + ">"
                    );
            throw new RelpConnectionException("Failed to connect to relp server!", e);
        }
        return 1;
    }

    public void tearDown() {
        /*
         TODO remove: wouldn't need a check hasConnected but there is a bug in RLP-01 tearDown()
         see https://github.com/teragrep/rlp_01/issues/63 for further info
         */
        if (hasConnected) {
            relpConnection.tearDown();
        }
    }

    /**
     * Tries to commit a relp batch. Throws RelpCommitException if committing or verifying of batch fails.
     *
     * @param relpBatch relp batch to be commited
     * @return number of attempts required to commit a batch
     */
    @Override
    public long ensureSent(RelpBatch relpBatch) throws RelpCommitException {
        // avoid unnecessary exception for fresh connections

        long attempts = 0;
        try {
            relpConnection.commit(relpBatch);
        }
        catch (IllegalStateException | IOException | TimeoutException e) {
            System.err.println("Exception <" + e.getMessage() + "> while sending relpBatch");
            throw new RelpCommitException("Failed to commit relpBatch!", e);
        }
        if (!relpBatch.verifyTransactionAll()) {
            throw new RelpCommitException("Failed to verify committed relpBatch!");
        }
        return attempts;
    }

    @Override
    public long ensureSent(byte[] bytes) throws RelpCommitException {
        final RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(bytes);
        return ensureSent(relpBatch);
    }

    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public void close() {
        try {
            this.relpConnection.disconnect();
        }
        catch (IllegalStateException | IOException | TimeoutException e) {
            System.err.println("Forcefully closing connection due to exception <" + e.getMessage() + ">");
        }
        finally {
            tearDown();
        }
    }
}
