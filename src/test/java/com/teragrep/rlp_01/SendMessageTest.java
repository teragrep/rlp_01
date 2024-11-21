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
package com.teragrep.rlp_01;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.net_01.server.ServerFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * These are a copy from rlp_03 test suite
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SendMessageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendMessageTest.class);

    private final String hostname = "localhost";
    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private ExecutorService executorService;
    private static int port = 1236;

    private final List<byte[]> messageList = new LinkedList<>();

    @BeforeAll
    public void init() {
        port = getPort();

        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertAll(() -> eventLoop = eventLoopFactory.create());

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        executorService = Executors.newSingleThreadExecutor();
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(() -> new DefaultFrameDelegate((frame) -> messageList.add(frame.relpFrame().payload().toBytes())))
        );
        Assertions.assertAll(() -> serverFactory.create(port));
    }

    @AfterAll
    public void cleanup() {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertAll(eventLoopThread::join);
    }

    private synchronized int getPort() {
        return ++port;
    }

    @Test
    public void testSendMessage() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        String msg = "<14>1 2020-05-15T13:24:03.603Z CFE-16 capsulated - - [CFE-16-metadata@48577 authentication_token=\"AUTH_TOKEN_11111\" channel=\"CHANNEL_11111\" time_source=\"generated\"][CFE-16-origin@48577] \"Hello, world!\"\n";
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        RelpBatch batch = new RelpBatch();
        long reqId = batch.insert(data);
        Assertions.assertAll(() -> relpSession.commit(batch));
        // verify successful transaction
        Assertions.assertTrue(batch.verifyTransaction(reqId));
        Assertions.assertAll(relpSession::disconnect);

        // message must equal to what was send
        Assertions.assertEquals(msg, new String(messageList.get(0)));

        // clear received list
        messageList.clear();
    }

    @Test
    public void testSendSmallMessage() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        String msg = "<167>Mar  1 01:00:00 1um:\n";
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        RelpBatch batch = new RelpBatch();
        long reqId = batch.insert(data);
        Assertions.assertAll(() -> relpSession.commit(batch));
        // verify successful transaction
        Assertions.assertTrue(batch.verifyTransaction(reqId));
        Assertions.assertAll(relpSession::disconnect);

        // message must equal to what was send
        Assertions.assertEquals(msg, new String(messageList.get(0)));

        // clear received list
        messageList.clear();
    }

    @Test
    public void testOpenAndCloseSession() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        Assertions.assertAll(relpSession::disconnect);
    }

    @Test
    public void testSessionCloseTwice() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        Assertions.assertAll(relpSession::disconnect);
        Assertions.assertThrows(IllegalStateException.class, relpSession::disconnect);

    }

    @Test
    public void clientTestOpenSendClose() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        String msg = "clientTestOpenSendClose";
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        RelpBatch batch = new RelpBatch();
        batch.insert(data);
        Assertions.assertAll(() -> relpSession.commit(batch));
        Assertions.assertTrue(batch.verifyTransactionAll());
        Assertions.assertAll(relpSession::disconnect);

        // message must equal to what was send
        Assertions.assertEquals(msg, new String(messageList.get(0)));

        // clear received list
        messageList.clear();
    }

    @Test
    public void clientTestSendTwo() {
        RelpConnection relpSession = new RelpConnection();
        relpSession.setConnectionTimeout(5000);
        relpSession.setReadTimeout(5000);
        relpSession.setWriteTimeout(5000);
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("test> Connected");
            Assertions.assertAll(() -> Thread.sleep(1000));
        }
        String msg1 = "clientTestOpenSendClose 1";
        byte[] data1 = msg1.getBytes(StandardCharsets.UTF_8);
        RelpBatch batch1 = new RelpBatch();
        batch1.insert(data1);
        Assertions.assertAll(() -> relpSession.commit(batch1));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("test> Committed");
            Assertions.assertAll(() -> Thread.sleep(1000));
        }
        Assertions.assertTrue(batch1.verifyTransactionAll());

        String msg2 = "clientTestOpenSendClose 2";
        byte[] data2 = msg2.getBytes(StandardCharsets.UTF_8);
        RelpBatch batch2 = new RelpBatch();
        batch2.insert(data2);
        Assertions.assertAll(() -> relpSession.commit(batch2));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("test> Committed second");
            Assertions.assertAll(() -> Thread.sleep(1000));
        }
        Assertions.assertTrue(batch1.verifyTransactionAll());
        Assertions.assertAll(relpSession::disconnect);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("test> Disconnected");
            Assertions.assertAll(() -> Thread.sleep(1000));
        }

        // messages must equal to what was send
        Assertions.assertEquals(msg1, new String(messageList.get(0)));
        Assertions.assertEquals(msg2, new String(messageList.get(1)));

        // clear received list
        messageList.clear();
    }

    @Test
    public void testSendBatch() {
        RelpConnection relpSession = new RelpConnection();
        Assertions.assertAll(() -> relpSession.connect(hostname, port));
        String msg = "Hello, world!";
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        int n = 50;
        RelpBatch batch = new RelpBatch();
        for (int i = 0; i < n; i++) {
            batch.insert(data);
        }
        Assertions.assertAll(() -> relpSession.commit(batch));
        Assertions.assertTrue(batch.verifyTransactionAll());
        Assertions.assertAll(relpSession::disconnect);

        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(msg, new String(messageList.get(i)));
        }

        // clear afterwards
        messageList.clear();
    }

}
