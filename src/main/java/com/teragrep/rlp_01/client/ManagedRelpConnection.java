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

    @Override
    public void connect() {
        boolean connected = false;
        while (!connected) {
            try {
                this.hasConnected = true;
                connected = relpConnection
                        .connect(relpConnection.relpConfig().relpTarget, relpConnection.relpConfig().relpPort);
            }
            catch (Exception e) {
                System.err.println(
                                "Failed to connect to relp server <["+relpConnection.relpConfig().relpTarget+"]>:<["+relpConnection.relpConfig().relpPort+"]>: <"+e.getMessage()+">");

                try {
                    Thread.sleep(relpConnection.relpConfig().relpReconnectInterval);
                }
                catch (InterruptedException exception) {
                    System.err.println("Reconnection timer interrupted, reconnecting now");
                }
            }
        }
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

    @Override
    public void ensureSent(RelpBatch relpBatch) {
        // avoid unnecessary exception for fresh connections
        if (!hasConnected) {
            connect();
        }


        boolean notSent = true;
        while (notSent) {
            try {
                relpConnection.commit(relpBatch);
            }
            catch (IllegalStateException | IOException | TimeoutException e) {
                System.err.println("Exception <"+e.getMessage()+"> while sending relpBatch. Will retry");
            }
            if (!relpBatch.verifyTransactionAll()) {
                relpBatch.retryAllFailed();
                this.tearDown();
                this.connect();
            }
            else {
                notSent = false;
            }
        }
    }

    @Override
    public void ensureSent(byte[] bytes) {
        final RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(bytes);
        ensureSent(relpBatch);
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
            System.err.println("Forcefully closing connection due to exception <"+e.getMessage()+">");
        }
        finally {
            tearDown();
        }
    }
}
