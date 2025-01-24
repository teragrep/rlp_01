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

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.pool.Pool;
import com.teragrep.rlp_01.pool.UnboundPool;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.EventDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_03.frame.delegate.event.RelpEvent;
import com.teragrep.rlp_03.frame.delegate.event.RelpEventSyslog;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ManagedConnectionTest {
    private final String hostname = "localhost";
    private final int port = 33601;

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private ExecutorService executorService;

    private final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();

    private final AtomicLong connectionOpenCount = new AtomicLong();
    private final AtomicLong connectionCleanCloseCount = new AtomicLong();

    @BeforeAll
    public void init() {
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertDoesNotThrow(() -> eventLoop = eventLoopFactory.create());

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        executorService = Executors.newSingleThreadExecutor();

        Supplier<FrameDelegate> frameDelegateSupplier = () -> {

            Map<String, RelpEvent> relpCommandConsumerMap = new HashMap<>();

            relpCommandConsumerMap.put("close", new RelpEventCloseCounting(connectionCleanCloseCount));

            relpCommandConsumerMap.put("open", new RelpEventOpenCounting(connectionOpenCount));

            relpCommandConsumerMap.put("syslog", new RelpEventSyslog((frame) -> messageList.add(frame.relpFrame().payload().toBytes())));

            return new EventDelegate(relpCommandConsumerMap);
        };

        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(port));
    }

    @AfterAll
    public void cleanup() {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
    }


    @Test
    public void testFactoryProvisionedConnection() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                0,
                false,
                Duration.ZERO,
                false
        );

        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(relpConfig);

        IManagedRelpConnection relpConnection = relpConnectionFactory.get();

        Assertions.assertDoesNotThrow(relpConnection::connect);

        String heyRelp = "hey this is relp";

        relpConnection.ensureSent(heyRelp.getBytes(StandardCharsets.UTF_8));

        Assertions.assertDoesNotThrow(relpConnection::close);

        Assertions.assertEquals(heyRelp, new String(messageList.remove(), StandardCharsets.UTF_8));
    }

    @Test
    public void testPooledConnections() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                0,
                false,
                Duration.ZERO,
                false
        );

        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(relpConfig);

        Pool<IManagedRelpConnection> relpConnectionPool = new UnboundPool<>(relpConnectionFactory, new ManagedRelpConnectionStub());

        int testCycles = 1_000;
        CountDownLatch countDownLatch = new CountDownLatch(testCycles);

        for (int i = 0; i < testCycles; i++) {
            final String heyRelp = "hey this is relp " + i;
            ForkJoinPool.commonPool().submit(() -> {
                IManagedRelpConnection connection = relpConnectionPool.get();

                connection.ensureSent(heyRelp.getBytes(StandardCharsets.UTF_8));
                relpConnectionPool.offer(connection);
                countDownLatch.countDown();
            });
        }

        Assertions.assertDoesNotThrow(() -> countDownLatch.await());

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }

        Assertions.assertTrue(connectionOpenCount.get() > 1);
        Assertions.assertEquals(connectionOpenCount.get(), connectionCleanCloseCount.get());
        connectionOpenCount.set(0);
        connectionCleanCloseCount.set(0);
    }

    @Test
    public void testPooledRenewedConnections() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                0,
                false,
                Duration.of(5, ChronoUnit.MILLIS),
                true
        );

        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(relpConfig);

        Pool<IManagedRelpConnection> relpConnectionPool = new UnboundPool<>(relpConnectionFactory, new ManagedRelpConnectionStub());

        int testCycles = 20;
        CountDownLatch countDownLatch = new CountDownLatch(testCycles);

        for (int i = 0; i < testCycles; i++) {
            final String heyRelp = "hey this is renewed relp " + i;
            ForkJoinPool.commonPool().submit(() -> {
                IManagedRelpConnection connection = relpConnectionPool.get();

                // will set timer to 5 millis
                connection.ensureSent(heyRelp.getBytes(StandardCharsets.UTF_8));
                // exceed 5 millis
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                relpConnectionPool.offer(connection);
                countDownLatch.countDown();
            });
        }


        Assertions.assertDoesNotThrow(() -> countDownLatch.await());

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is renewed relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }

        Assertions.assertTrue(connectionOpenCount.get() > 1);
        // renewable uses forceReconnect
        Assertions.assertTrue(connectionCleanCloseCount.get() < connectionOpenCount.get());
        connectionOpenCount.set(0);
        connectionCleanCloseCount.set(0);
    }

    @Test
    public void testPooledReboundConnections() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                1,
                true,
                Duration.ZERO,
                false
        );

        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(relpConfig);

        Pool<IManagedRelpConnection> relpConnectionPool = new UnboundPool<>(relpConnectionFactory, new ManagedRelpConnectionStub());

        int testCycles = 20;
        CountDownLatch countDownLatch = new CountDownLatch(testCycles);

        for (int i = 0; i < testCycles; i++) {
            final String heyRelp = "hey this is rebound relp " + i;
            ForkJoinPool.commonPool().submit(() -> {
                IManagedRelpConnection connection = relpConnectionPool.get();

                // will set timer to 5 millis
                connection.ensureSent(heyRelp.getBytes(StandardCharsets.UTF_8));
                // exceed 5 millis
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                relpConnectionPool.offer(connection);
                countDownLatch.countDown();
            });
        }


        Assertions.assertDoesNotThrow(() -> countDownLatch.await());

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is rebound relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }

        Assertions.assertTrue(connectionOpenCount.get() > 1);
        Assertions.assertEquals(connectionOpenCount.get(), connectionCleanCloseCount.get());
        connectionOpenCount.set(0);
        connectionCleanCloseCount.set(0);
    }

    @Test
    public void testRelpBatchSend() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                0,
                false,
                Duration.ZERO,
                false
        );

        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(relpConfig);

        Pool<IManagedRelpConnection> relpConnectionPool = new UnboundPool<>(relpConnectionFactory, new ManagedRelpConnectionStub());

        int testCycles = 20;
        CountDownLatch countDownLatch = new CountDownLatch(testCycles);



        for (int i = 0; i < testCycles; i++) {
            final String heyRelp = "hey this is batched relp 0 " + i;
            final String heyThisBeRelpToo = "hey this is batched relp 1 " + i;
            ForkJoinPool.commonPool().submit(() -> {
                RelpBatch relpBatch  = new RelpBatch();
                relpBatch.insert(heyRelp.getBytes(StandardCharsets.UTF_8));
                relpBatch.insert(heyThisBeRelpToo.getBytes(StandardCharsets.UTF_8));

                IManagedRelpConnection connection = relpConnectionPool.get();

                // will set timer to 5 millis
                connection.ensureSent(relpBatch);

                relpConnectionPool.offer(connection);
                countDownLatch.countDown();
            });
        }

        Assertions.assertDoesNotThrow(() -> countDownLatch.await());

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles*2, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is batched relp \\d \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }

        Assertions.assertTrue(connectionOpenCount.get() > 1);
        Assertions.assertEquals(connectionOpenCount.get(), connectionCleanCloseCount.get());
        connectionOpenCount.set(0);
        connectionCleanCloseCount.set(0);
    }
}
