/*
* Teragrep Reliable Event Logging Protocol (RELP) Library for Java
* Copyright (C) 2021-2026 Suomen Kanuuna Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.teragrep.rlp_01.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RetryableConnectionTest {

    private final String hostname = "localhost";
    private final SocketConfig socketConfig = new SocketConfigImpl(1000, 1000, 100, false);
    private final TestServerFactory serverFactory = new TestServerFactory();
    private final ConcurrentLinkedDeque<byte[]> messages = new ConcurrentLinkedDeque<>();
    private final AtomicLong opens = new AtomicLong();
    private final AtomicLong closes = new AtomicLong();

    @Test
    public void testConnectAttemptsUntilServerIsAvailable() {
        final int port = 35602;
        RelpConfig relpConfig = new RelpConfig(hostname, port, 100, 0, false, Duration.ZERO, false, 100);
        IManagedRelpConnection connection = new RelpConnectionFactory(relpConfig, socketConfig).get();
        Assertions.assertDoesNotThrow(() -> {
            Future<Long> connecting = ForkJoinPool.commonPool().submit(connection::connect);
            Thread.sleep(500);

            TestServer server = serverFactory.create(port, messages, opens, closes);
            server.run();

            long attempts = connecting.get(5, TimeUnit.SECONDS);
            Assertions.assertNotEquals(1, attempts, "should connect after server is available");
            connection.close();
            server.close();
        }, "connect should be retried until the server is available");
    }

    @Test
    public void testConnectThrowsWhenAttemptsRunout() {
        final int port = 35603;
        RelpConfig relpConfig = new RelpConfig(hostname, port, 100, 0, false, Duration.ZERO, false, 10);
        IManagedRelpConnection connection = new RelpConnectionFactory(relpConfig, socketConfig).get();
        Exception thrown = Assertions
                .assertThrows(IOException.class, connection::connect, "should throw after reaching max attempts");
        Assertions
                .assertEquals(
                        "connect() gave up after <[10]> attempts.", thrown.getMessage(),
                        "exception message should match expected"
                );
    }

    @Test
    public void testReconnectThrowsWhenAttemptsRunout() {
        final int port = 35605;
        RelpConfig relpConfig = new RelpConfig(hostname, port, 100, 0, false, Duration.ZERO, false, 10);
        IManagedRelpConnection connection = new RelpConnectionFactory(relpConfig, socketConfig).get();
        RuntimeException thrown = Assertions
                .assertThrows(RuntimeException.class, connection::reconnect, "should throw after reaching max attempts");
        Assertions
                .assertEquals(
                        "reconnect() gave up after <[10]> attempts.", thrown.getMessage(),
                        "exception should match expected message"
                );
    }

    @Test
    public void testForceReconnectThrowsWhenAttemptsRunout() {
        final int port = 35607;
        RelpConfig relpConfig = new RelpConfig(hostname, port, 100, 0, false, Duration.ZERO, false, 10);
        IManagedRelpConnection connection = new RelpConnectionFactory(relpConfig, socketConfig).get();
        RuntimeException thrown = Assertions
                .assertThrows(
                        RuntimeException.class, connection::forceReconnect, "should throw after reaching max attempts"
                );
        Assertions
                .assertEquals(
                        "forceReconnect() gave up after <[10]> attempts.", thrown.getMessage(),
                        "exception should match expected message"
                );
    }

    @Test
    public void testEnsureSentThrowsWhenAttemptsRunsOut() {
        int port = 35604;
        RelpConfig relpConfig = new RelpConfig(hostname, port, 100, 0, false, Duration.ZERO, false, 10);
        IManagedRelpConnection connection = new RelpConnectionFactory(relpConfig, socketConfig).get();
        byte[] bytes = "hey this is relp".getBytes(StandardCharsets.UTF_8);
        Exception thrown = Assertions.assertThrows(RuntimeException.class, () -> connection.ensureSent(bytes));
        Assertions
                .assertEquals(
                        "ensureSent() gave up after <[10]> attempts.", thrown.getMessage(),
                        "exception should match expected message"
                );
    }
}
