package com.teragrep.rlp_01.client;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.Server;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.EventDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_03.frame.delegate.event.RelpEvent;
import com.teragrep.rlp_03.frame.delegate.event.RelpEventSyslog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

class TestServerFactory {

    public TestServer create(int port, ConcurrentLinkedDeque<byte[]> messageList, AtomicLong connectionOpenCount, AtomicLong connectionCleanCloseCount) throws IOException {
        EventLoopFactory eventLoopFactory = new EventLoopFactory();

        EventLoop eventLoop = eventLoopFactory.create();
        Thread eventLoopThread = new Thread(eventLoop);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        Server server = serverFactory.create(port);

        return new TestServer(eventLoop, eventLoopThread, executorService, server);
    }
}
