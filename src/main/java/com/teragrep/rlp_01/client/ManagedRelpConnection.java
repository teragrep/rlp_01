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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeoutException;

public class ManagedRelpConnection implements IManagedRelpConnection {

    private final IRelpConnection relpConnection;
    private boolean hasConnected;

    public ManagedRelpConnection(IRelpConnection relpConnection) {
        this.relpConnection = relpConnection;
        this.hasConnected = false;
    }

    @Override
    public void forceReconnect() {
        tearDown();
        connect();
    }

    @Override
    public void reconnect() {
        close();
        connect();
    }

    /**
     * Tries to establish a relp connection once.
     *
     * @return number of attempts made, always 1
     */
    @Override
    public long connect() {
        boolean connected;
        try {
            connected = relpConnection
                    .connect(relpConnection.relpConfig().relpTarget, relpConnection.relpConfig().relpPort);
        }
        catch (Exception e) {
            throw new UncheckedIOException(
                    "Failed to connect to relp server <[" + relpConnection.relpConfig().relpTarget + "]>:<["
                            + relpConnection.relpConfig().relpPort + "]> <" + e.getMessage() + ">",
                    new IOException(e)
            );
        }
        if (!connected) {
            throw new UncheckedIOException(new IOException("Relp server refused to open session"));
        }
        this.hasConnected = true;
        return 1;
    }

    private void tearDown() {
        /*
         TODO remove: wouldn't need a check hasConnected but there is a bug in RLP-01 tearDown()
         see https://github.com/teragrep/rlp_01/issues/63 for further info
         */
        if (hasConnected) {
            relpConnection.tearDown();
        }
    }

    /**
     * Tries to commit a relp batch to a connection indefinitely until successful.
     *
     * @param relpBatch relp batch to be commited
     * @return number of attempts required to commit a batch
     */
    @Override
    public long ensureSent(RelpBatch relpBatch) {
        // avoid unnecessary exception for fresh connections
        if (!hasConnected) {
            connect();
        }

        boolean notSent = true;
        long attempts = 0;
        while (notSent) {
            try {
                relpConnection.commit(relpBatch);
            }
            catch (IllegalStateException | IOException | TimeoutException e) {
                System.err.println("Exception <" + e.getMessage() + "> while sending relpBatch. Will retry");
            }
            finally {
                attempts++;
            }
            if (!relpBatch.verifyTransactionAll()) {
                if (attempts >= relpConnection.relpConfig().maxSendAttempts) {
                    throw new UncheckedIOException(
                            new IOException("RelpBatch was not acknowledged in <[" + attempts + "]> attempts")
                    );
                }
                relpBatch.retryAllFailed();
                this.tearDown();
                this.connect();
            }
            else {
                notSent = false;
            }
        }
        return attempts;
    }

    @Override
    public long ensureSent(byte[] bytes) {
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
