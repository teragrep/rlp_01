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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SendMessageTest {
    /*
    private String hostname;
    private int port;
    
    @Before
    public void init() throws IOException, InterruptedException {
        this.hostname = "localhost";
        this.port = 1235;
    }
    
    @After
    public void cleanup() throws IOException {
    }

    @Test
    public void testSendBatch() throws IllegalStateException, IOException, TimeoutException {
        RelpConnection relpSession = new RelpConnection();
        relpSession.connect(this.hostname, this.port);
        String msg = "<14>1 2020-05-15T13:24:03.603Z CFE-16 capsulated - - [CFE-16-metadata@48577 authentication_token=\"AUTH_TOKEN_11111\" channel=\"CHANNEL_11111\" time_source=\"generated\"][CFE-16-origin@48577] \"Hello, world!\"\n";
        byte[] data = msg.getBytes("UTF-8");
        int n = 200;
        RelpBatch batch = new RelpBatch();
        for (int i = 0; i < n; i++) {
            batch.insert(data);
        }
        relpSession.commit(batch);
        assertTrue(batch.verifyTransactionAll());
        relpSession.disconnect();
    }

    @Test
    public void testSendMessage() throws IOException, TimeoutException {
        RelpConnection relpSession = new RelpConnection();
        relpSession.connect(this.hostname, this.port);
        String msg = "<14>1 2020-05-15T13:24:03.603Z CFE-16 capsulated - - [CFE-16-metadata@48577 authentication_token=\"AUTH_TOKEN_11111\" channel=\"CHANNEL_11111\" time_source=\"generated\"][CFE-16-origin@48577] \"Hello, world!\"\n";
        byte[] data = msg.getBytes("UTF-8");
        RelpBatch batch = new RelpBatch();
        long reqId = batch.insert(data);
        relpSession.commit(batch);
        // verify successful transaction
        assertTrue(batch.verifyTransaction(reqId));
        relpSession.disconnect();
    }

    @Test
    public void testOpenAndCloseSession() throws IOException, TimeoutException {
        RelpConnection relpSession = new RelpConnection();
        relpSession.connect(this.hostname, this.port);
        relpSession.disconnect();        
    }
    
    @Test
    public void testSessionCloseTwice() throws IOException, TimeoutException {
        RelpConnection relpSession = new RelpConnection();
        relpSession.connect(this.hostname, this.port);
        relpSession.disconnect();  
        try {
            relpSession.disconnect();
            fail("IllegalStateException should have been thrown.");
        } catch (IllegalStateException e) {
            // this is expected
        }
    }
     */
}
