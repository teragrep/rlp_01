package com.teragrep.rlp_01.client;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_01.pool.Pool;
import com.teragrep.rlp_01.pool.UnboundPool;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.time.Period;
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

    @Test
    public void testFactoryProvisionedConnection() {
        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                500,
                0,
                false,
                Period.ZERO,
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
                Period.ZERO,
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
}
