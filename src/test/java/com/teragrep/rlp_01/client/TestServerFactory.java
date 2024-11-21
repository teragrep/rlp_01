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
