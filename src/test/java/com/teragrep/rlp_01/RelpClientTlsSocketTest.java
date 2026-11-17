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
package com.teragrep.rlp_01;

import com.teragrep.rlp_01.client.SSLContextSupplierKeystore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

class RelpClientTlsSocketTest {

    String keystorePath = "noSuchFile";
    String keystorePassword = "changeit";
    String protocol = "TLSv1.3";
    Supplier<SSLEngine> sslEngineSupplier = (() -> new SSLContextSupplierKeystore(
            keystorePath,
            keystorePassword,
            protocol
    ).get().createSSLEngine());

    @Test
    public void testCloseUnopenedConnection() {
        final RelpClientTlsSocket socket = new RelpClientTlsSocket(sslEngineSupplier);
        Assertions.assertDoesNotThrow(socket::close);
    }

    @Test
    public void testWriteToUnopenedConnection() {
        final RelpClientTlsSocket socket = new RelpClientTlsSocket(sslEngineSupplier);
        final String message = "SomePayload";
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(message.length());
        byteBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        Assertions.assertThrows(ClosedChannelException.class, () -> {
            socket.write(byteBuffer);
        });
    }

    @Test
    public void testReadFromUnopenedConnection() {
        final RelpClientTlsSocket socket = new RelpClientTlsSocket(sslEngineSupplier);
        final String message = "SomePayload";
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(message.length());
        byteBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        Assertions.assertThrows(ClosedChannelException.class, () -> {
            socket.read(byteBuffer);
        });
    }

}
