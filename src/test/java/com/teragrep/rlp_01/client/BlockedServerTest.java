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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class BlockedServerTest {

    @Test
    public void testServerNotResponding() {
        final String hostname = "localhost";
        final int port = 34601;

        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                100,
                0,
                false,
                Duration.ZERO,
                false
        );

        RelpConnectionFactory connectionFactory = new RelpConnectionFactory(relpConfig);

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();

        final AtomicLong connectionOpenCount = new AtomicLong();
        final AtomicLong connectionCleanCloseCount = new AtomicLong();

        TestServerFactory testServerFactory = new TestServerFactory();
        Assertions.assertDoesNotThrow(() -> {
            String testString = "hello server, i wait for your reply";

            TestClient testClient = new TestClient(connectionFactory.get(), testString);

            Thread thread = new Thread(testClient);
            TestServer testServer = testServerFactory.create(port, messageList, connectionOpenCount, connectionCleanCloseCount);
            thread.start();

            Thread.sleep(500);

            testServer.run();
            thread.join();

            Assertions.assertEquals(1, messageList.size());
            Assertions.assertEquals(testString, new String(messageList.removeFirst(), StandardCharsets.UTF_8) );

            testServer.close();
        });

    }

    @Test
    public void testServerNotRespondingAndRetried() {
        final String hostname = "localhost";
        final int port = 34601;

        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                100,
                0,
                false,
                Duration.ZERO,
                false
        );

        SocketConfig socketConfig = new SocketConfigImpl(50, 50,50, false);

        RelpConnectionFactory connectionFactory = new RelpConnectionFactory(relpConfig, socketConfig);

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();

        final AtomicLong connectionOpenCount = new AtomicLong();
        final AtomicLong connectionCleanCloseCount = new AtomicLong();

        TestServerFactory testServerFactory = new TestServerFactory();
        Assertions.assertDoesNotThrow(() -> {
            String testString = "hello server, i wait your reply for some time";

            TestClient testClient = new TestClient(connectionFactory.get(), testString);

            Thread thread = new Thread(testClient);
            TestServer testServer = testServerFactory.create(port, messageList, connectionOpenCount, connectionCleanCloseCount);
            thread.start();

            Thread.sleep(500);

            testServer.run();
            thread.join();

            Assertions.assertEquals(1, messageList.size());
            Assertions.assertEquals(testString, new String(messageList.removeFirst(), StandardCharsets.UTF_8) );

            testServer.close();
        });
    }

    @Test
    public void testServerNotRespondingAndWriteRetried() {
        final String hostname = "localhost";
        final int port = 34601;

        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                100,
                0,
                false,
                Duration.ZERO,
                false
        );

        SocketConfig socketConfig = new SocketConfigImpl(50, 1,50, false);

        RelpConnectionFactory connectionFactory = new RelpConnectionFactory(relpConfig, socketConfig);

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();

        final AtomicLong connectionOpenCount = new AtomicLong();
        final AtomicLong connectionCleanCloseCount = new AtomicLong();

        TestServerFactory testServerFactory = new TestServerFactory();
        Assertions.assertDoesNotThrow(() -> {
            String stringTemplate = "hello server, i wait your reply for some time, but this message is long ";

            String testString = String.join("", Collections.nCopies(1024*1024, stringTemplate));

            TestClient testClient = new TestClient(connectionFactory.get(), testString);

            Thread thread = new Thread(testClient);
            TestServer testServer = testServerFactory.create(port, messageList, connectionOpenCount, connectionCleanCloseCount);
            thread.start();

            Thread.sleep(500);

            testServer.run();
            thread.join();

            Assertions.assertFalse(messageList.isEmpty()); // write timeout based resends cause duplicates
            Assertions.assertEquals(testString, new String(messageList.removeFirst(), StandardCharsets.UTF_8) );

            testServer.close();
        });
    }

    @Test
    public void testServerClosed() {
        final String hostname = "localhost";
        final int port = 34601;

        RelpConfig relpConfig = new RelpConfig(
                hostname,
                port,
                100,
                0,
                false,
                Duration.ZERO,
                false
        );

        SocketConfig socketConfig = new SocketConfigImpl(50, 50,50, false);

        RelpConnectionFactory connectionFactory = new RelpConnectionFactory(relpConfig, socketConfig);

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();

        final AtomicLong connectionOpenCount = new AtomicLong();
        final AtomicLong connectionCleanCloseCount = new AtomicLong();

        TestServerFactory testServerFactory = new TestServerFactory();
        Assertions.assertDoesNotThrow(() -> {
            String testString = "hello closed server";

            TestClient testClient = new TestClient(connectionFactory.get(), testString);

            Thread thread = new Thread(testClient);

            thread.start();

            Thread.sleep(500);

            TestServer testServer = testServerFactory.create(port, messageList, connectionOpenCount, connectionCleanCloseCount);

            testServer.run();
            thread.join();

            Assertions.assertEquals(1, messageList.size());
            Assertions.assertEquals(testString, new String(messageList.removeFirst(), StandardCharsets.UTF_8) );

            testServer.close();
        });
    }

    private static class TestClient implements Runnable, Closeable {

        private final IManagedRelpConnection connection;
        private final String testString;

        TestClient(IManagedRelpConnection connection, String testString) {
            this.connection = connection;
            this.testString = testString;
        }

        @Override
        public void close() throws IOException {
            connection.close();
        }

        @Override
        public void run() {
            connection.ensureSent(testString.getBytes(StandardCharsets.UTF_8));
        }
    }
}
