package com.teragrep.rlp_01.client;

import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.server.Server;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.ExecutorService;

class TestServer implements Runnable, AutoCloseable {
    private final EventLoop eventLoop;
    private final Thread eventLoopThread;
    private final ExecutorService executorService;
    private final Server server;

    public TestServer(EventLoop eventLoop, Thread eventLoopThread, ExecutorService executorService, Server server) {
        this.eventLoop = eventLoop;
        this.eventLoopThread = eventLoopThread;
        this.executorService = executorService;
        this.server = server;
    }

    @Override
    public void close() throws Exception {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
        server.close(); // closes port
    }

    @Override
    public void run() {
        eventLoopThread.start();
    }
}
