package com.teragrep.rlp_01.client;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_01.pool.Pool;
import com.teragrep.rlp_01.pool.UnboundPool;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ManagedConnectionTest {
    private final String hostname = "localhost";
    private final int port = 33601;

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private ExecutorService executorService;

    private final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
    @BeforeAll
    public void init() {
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

        Assertions.assertAll(relpConnection::connect);

        String heyRelp = "hey this is relp";

        relpConnection.ensureSent(heyRelp.getBytes(StandardCharsets.UTF_8));

        Assertions.assertAll(relpConnection::close);

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

        Assertions.assertAll(countDownLatch::await);

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }
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


        Assertions.assertAll(countDownLatch::await);

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is renewed relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }
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


        Assertions.assertAll(countDownLatch::await);

        relpConnectionPool.close();

        Assertions.assertEquals(testCycles, messageList.size());

        Pattern heyPattern = Pattern.compile("hey this is rebound relp \\d+");
        while(!messageList.isEmpty()) {
            byte[] payload = messageList.removeFirst();
            Assertions.assertTrue(heyPattern.matcher(new String(payload, StandardCharsets.UTF_8)).matches());
        }
    }
}
