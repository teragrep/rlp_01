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
import com.teragrep.rlp_01.client.exceptions.RelpAttemptsExhaustedException;
import com.teragrep.rlp_01.client.exceptions.RelpException;

import java.io.IOException;

//Retries the connection when it fails and throws once the attempts have been spent
public class RetryableRelpConnection implements IManagedRelpConnection {

    private final IManagedRelpConnection managedRelpConnection;
    private final long maxConnectionAttempts;
    private final int reconnectInterval;

    public RetryableRelpConnection(
            IManagedRelpConnection managedRelpConnection,
            long maxConnectionAttempts,
            int reconnectInterval
    ) {
        this.managedRelpConnection = managedRelpConnection;
        this.maxConnectionAttempts = maxConnectionAttempts;
        this.reconnectInterval = reconnectInterval;
    }

    /**
     * Tries to tear down and establish a relp connection until attempts have been spent.
     *
     * @throws RelpAttemptsExhaustedException once the attempts have been spent, with the last failure as the cause
     */
    @Override
    public void forceReconnect() throws RelpAttemptsExhaustedException {
        long attempts = 0;
        boolean notConnected = true;
        while (notConnected) {
            attempts++;
            try {
                managedRelpConnection.forceReconnect();
                notConnected = false;
            }
            catch (RelpException e) {
                if (attempts >= maxConnectionAttempts) {
                    throw new RelpAttemptsExhaustedException(
                            "forceReconnect() gave up after <[" + maxConnectionAttempts + "]> attempts.",
                            e
                    );
                }
                sleep();
            }
        }
    }

    /**
     * Tries to reconnect until the attempts have been spent.
     *
     * @throws RelpAttemptsExhaustedException once the attempts have been spent, with the last failure as the cause
     */
    @Override
    public void reconnect() throws RelpAttemptsExhaustedException {
        long attempts = 0;
        boolean notConnected = true;
        while (notConnected) {
            attempts++;
            try {
                managedRelpConnection.reconnect();
                notConnected = false;
            }
            catch (RelpException e) {
                if (attempts >= maxConnectionAttempts) {
                    throw new RelpAttemptsExhaustedException(
                            "reconnect() gave up after <[" + maxConnectionAttempts + "]> attempts.",
                            e
                    );
                }
                sleep();
            }
        }
    }

    /**
     * Tries to establish a relp connection until the attempts have been spent.
     *
     * @return number of attempts made
     * @throws RelpAttemptsExhaustedException once the attempts have been spent, with the last failure as the cause
     */
    @Override
    public long connect() throws RelpAttemptsExhaustedException {
        long attempts = 0;
        boolean notConnected = true;
        while (notConnected) {
            attempts++;
            try {
                managedRelpConnection.connect();
                notConnected = false;
            }
            catch (RelpException e) {
                if (attempts >= maxConnectionAttempts) {
                    throw new RelpAttemptsExhaustedException(
                            "connect() gave up after <[" + maxConnectionAttempts + "]> attempts.",
                            e
                    );
                }
                sleep();
            }
        }
        return attempts;
    }

    /**
     * Tries to commit a relp batch until attempts have been spent.
     *
     * @param relpBatch relp batch to be commited
     * @return number of attempts needed to commit the batch
     * @throws RelpAttemptsExhaustedException once the attempts have been spent, with the last failure as the cause
     */
    @Override
    public long ensureSent(RelpBatch relpBatch) throws RelpAttemptsExhaustedException {
        long attempts = 0;
        boolean notSent = true;
        while (notSent) {
            attempts++;
            try {
                attempts = managedRelpConnection.ensureSent(relpBatch);
                notSent = false;
            }
            catch (RelpException e) {
                if (attempts >= maxConnectionAttempts) {
                    throw new RelpAttemptsExhaustedException(
                            "ensureSent() gave up after <[" + maxConnectionAttempts + "]> attempts.",
                            e
                    );
                }
                sleep();
            }
        }
        return attempts;
    }

    @Override
    public long ensureSent(byte[] bytes) throws RelpAttemptsExhaustedException {
        final RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(bytes);
        return ensureSent(relpBatch);
    }

    @Override
    public boolean isStub() {
        return managedRelpConnection.isStub();
    }

    @Override
    public void close() throws IOException {
        managedRelpConnection.close();
    }

    private void sleep() {
        try {
            Thread.sleep(reconnectInterval);
        }
        catch (InterruptedException exception) {
            System.err.println("Reconnection timer interrupted");
        }
    }
}
