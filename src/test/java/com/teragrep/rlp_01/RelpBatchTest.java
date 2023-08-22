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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RelpBatchTest {
    private static final String message = "syslog message";

    @Test
    public void testInsert() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        RelpFrameTX frame = batch.getRequest(id);
        Assertions.assertEquals(
                String.format(
                        "0 syslog %s %s",
                        message.length(),
                        message
                ),
                frame.toString(),
                "Did not receive expected value from getRequest"
        );
    }

    @Test
    public void testGetNullRequest() {
        RelpBatch batch = new RelpBatch();
        RelpFrameTX frame = batch.getRequest(0L);
        Assertions.assertNull(frame, "Frame was not null");
    }

    @Test
    public void testRemoveRequest() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        Assertions.assertNotNull(batch.getRequest(id), "Request was null");
        batch.removeRequest(id);
        Assertions.assertNull(batch.getRequest(id), "Request was not removed");
    }

    @Test
    public void testRemoveNullRequest() {
        RelpBatch batch = new RelpBatch();
        Assertions.assertDoesNotThrow(() -> {batch.removeRequest(1234L);});
    }

    @Test
    public void testGetNullResponse() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        RelpFrameRX frame = batch.getResponse(id);
        Assertions.assertNull(frame, "Got invalid response");
    }

    @Test
    public void testGetResponse() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        Assertions.assertNull(batch.getResponse(id), "Got a response but shouldn't have");
        batch.putResponse(id, new RelpFrameRX(id.intValue(), "X", 1, ByteBuffer.allocateDirect(1)));
        Assertions.assertNotNull(batch.getResponse(id), "Got a null response");
    }

    @Test
    public void testRemoveTransaction() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(batch.verifyTransactionAll(), "Verified transaction");
        batch.removeTransaction(id);
        Assertions.assertTrue(batch.verifyTransactionAll(), "Didn't verify transaction");
    }

    @Test
    public void testVerifyTransaction() {
        RelpBatch batch = new RelpBatch();
        Long id = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(batch.verifyTransaction(id), "Verified transaction");
        String response = "200 OK";
        ByteBuffer buffer = ByteBuffer.allocateDirect(response.length());
        buffer.put(response.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        batch.putResponse(id, new RelpFrameRX(id.intValue(), RelpCommand.SYSLOG, response.length(), buffer));
        Assertions.assertTrue(batch.verifyTransaction(id), "Didn't verify transaction");
    }

    @Test
    public void testVerifyTransactionAll() {
        RelpBatch batch = new RelpBatch();
        int messages = 5;
        Long[] ids = new Long[messages];
        for(int i=0; i<messages; i++) {
            ids[i] = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        }
        Assertions.assertFalse(batch.verifyTransactionAll(), "Verified transactions");
        for(int i=0; i<messages; i++) {
            Assertions.assertFalse(batch.verifyTransaction(ids[i]), "Verified transaction that was not completed");
            String response = "200 OK";
            ByteBuffer buffer = ByteBuffer.allocateDirect(response.length());
            buffer.put(response.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            batch.putResponse(ids[i], new RelpFrameRX(ids[i].intValue(), RelpCommand.SYSLOG, response.length(), buffer));
            Assertions.assertTrue(batch.verifyTransaction(ids[i]), "Verified transaction that was completed");
        }
        Assertions.assertTrue(batch.verifyTransactionAll(), "Did not verify all transactions");
    }

    @Test
    public void testWorkerQueueSize() {
        RelpBatch batch = new RelpBatch();
        Assertions.assertEquals(0, batch.getWorkQueueLength(), "Worker queue was not empty");
        int messages = 5;
        for(int i=0; i<messages; i++) {
            batch.insert(message.getBytes(StandardCharsets.UTF_8));
        }
        Assertions.assertEquals(5, batch.getWorkQueueLength(), "Worker queue was not as expected");
    }

    @Test
    public void testPopWorkQueue() {
        RelpBatch batch = new RelpBatch();
        int messages = 5;
        Long[] ids = new Long[messages];
        for(int i=0; i<messages;i++) {
            ids[i] = batch.insert(message.getBytes(StandardCharsets.UTF_8));
        }
        Assertions.assertEquals(messages, batch.getWorkQueueLength(), "Queue length was not as expected");
        // Try depleting
        for(int i=0; i<messages;i++) {
            Assertions.assertEquals(i, batch.popWorkQueue(), "Work queue popout returned unexpected values");
        }
        Assertions.assertEquals(0, batch.getWorkQueueLength(), "Queue length was not as expected");
    }
}
