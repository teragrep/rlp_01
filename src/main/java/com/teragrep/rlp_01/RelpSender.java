/*
   Java Reliable Event Logging Protocol Library RLP-01
   Copyright (C) 2021, 2022  Suomen Kanuuna Oy

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

package com.teragrep.rlp_01;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * A RELP message sender interface that specifies sending
 * of a window of messages in a batch.
 * 
 */
public interface RelpSender {

    /**
     * Connects to the RELP server.
     * 
     * @param hostname
     *  Hostname of IP address of the server.
     *  
     * @param port
     *  Port of the server.
     *  
     * @throws IOException
     * @throws IllegalStateException
     *  If the connection was already made.
     */
    public boolean connect(String hostname, int port) throws IOException, IllegalStateException, TimeoutException;

    /**
     * Sends a batch of syslog RELP messages and wait receipts from the server.
     *
     * @throws IOException
     * @throws IllegalStateException
     */
    public void commit(RelpBatch relpBatch) throws IOException, IllegalStateException, TimeoutException;
 
    /**
     * Closes the RELP session. This must be called after RELP transactions are done.
     * 
     * @throws IOException
     * @throws IllegalStateException
     *  If the session was already closesd.
     */
    public boolean disconnect() throws IOException, IllegalStateException, TimeoutException;
}
