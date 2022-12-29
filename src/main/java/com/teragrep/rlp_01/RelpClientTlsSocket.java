/*
   Java Reliable Event Logging Protocol Library RLP-01
   Copyright (C) 2021, 2022  Suomen Kanuuna Oy

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import tlschannel.ClientTlsChannel;
import tlschannel.NeedsReadException;
import tlschannel.NeedsWriteException;
import tlschannel.TlsChannel;

import javax.net.ssl.SSLContext;

public class RelpClientTlsSocket extends RelpClientSocket {
    private int readTimeout = 0;

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public int getWriteTimeout() {
        return writeTimeout;
    }

    @Override
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private int writeTimeout = 0;
    private int connectionTimeout = 0;

    private SocketChannel socketChannel;
    private Selector selector;


    // TODO remove, use engine as parameter
    SSLContext sslContext;

    private TlsChannel tlsChannel = null;

    RelpClientTlsSocket() {
        try {

            // FIXME parametrize
            sslContext = SSLContextFactory.authenticatedContext(
                    "keystore.jks",
                    "changeit",
                    "TLSv1.3"
            );
        }
        catch (GeneralSecurityException | IOException exception) {
            sslContext = null;
            // FIXME
        }
    }

    @Override
    void open (String hostname, int port) throws IOException, TimeoutException {
        if (this.selector != null && this.selector.isOpen()) {
            // Invalidate all selection key instances in case they were open
            this.selector.close();
        }

        this.selector = Selector.open();

        this.socketChannel = SocketChannel.open();
        // Make sure our poll will only block
        this.socketChannel.configureBlocking(false);
        // Poll only for connect
        SelectionKey key = this.socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
        // Async connect
        this.socketChannel.connect(new InetSocketAddress(hostname, port));

        // TODO parametrize using sslEngine
        ClientTlsChannel.Builder builder = ClientTlsChannel.newBuilder(
                socketChannel,
                sslContext
        );

        tlsChannel = builder.build();

        // Poll for connect
        boolean notConnected = true;
        while (notConnected) {
            int nReady = this.selector.select(this.connectionTimeout);
            // Woke up without anything to do
            if (nReady == 0) {
                throw new TimeoutException("connection timed out");
            }
            // It would be possible to skip the whole iterator, but we want to make sure if something else than connect
            // fires then it will be discarded.
            Set<SelectionKey> polledEvents = this.selector.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isConnectable()) {
                    if (this.socketChannel.finishConnect()) {
                        // Connection established
                        notConnected = false;
                        if (System.getenv("RELP_DEBUG") != null) {
                            System.out.println("relpConnection> established");
                            try {
                                Thread.sleep(1 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                eventIter.remove();
            }
        }
        // No need to be longer interested in connect.
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
    }

    @Override
    void write(ByteBuffer byteBuffer) throws IOException, TimeoutException {
        SelectionKey key = this.socketChannel.register(this.selector, SelectionKey.OP_WRITE);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendRelpRequestAsync> need to write: " + byteBuffer.hasRemaining());
        }
        while (byteBuffer.hasRemaining()) {
            int nReady = selector.select(this.writeTimeout);
            if (nReady == 0) {
                throw new TimeoutException("write timed out");
            }
            Set<SelectionKey> polledEvents = this.selector.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                // tlsChannel needs to know about both
                if (currentKey.isWritable() || currentKey.isReadable()) {
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpConnection.sendRelpRequestAsync> became writable");
                    }
                    try {
                        this.tlsChannel.write(byteBuffer);
                    } catch (NeedsReadException e) {
                        key.interestOps(SelectionKey.OP_READ); // overwrites previous value
                    } catch (NeedsWriteException e) {
                        key.interestOps(SelectionKey.OP_WRITE); // overwrites previous value
                    }
                }
                eventIter.remove();
            }
            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.sendRelpRequestAsync> still need to write: "
                        + byteBuffer.hasRemaining());
            }
        }
    }

    @Override
    void close() throws IOException {
        socketChannel.close();
    }

    @Override
    int read(ByteBuffer byteBuffer) throws IOException, TimeoutException {
        int readBytes = -1;

        SelectionKey key = this.socketChannel.register(this.selector, SelectionKey.OP_READ);
        int nReady = selector.select(this.readTimeout);
        if (nReady == 0) {
            throw new TimeoutException("read timed out");
        }
        Set<SelectionKey> polledEvents = this.selector.selectedKeys();
        Iterator<SelectionKey> eventIter = polledEvents.iterator();
        while (eventIter.hasNext()) {
            SelectionKey currentKey = eventIter.next();
            // tlsChannel needs to know about both
            if (currentKey.isReadable() || currentKey.isWritable()) {
                if (System.getenv("RELP_DEBUG") != null) {
                    System.out.println("relpConnection.readAcks> became readable");
                }
                try {
                    readBytes = tlsChannel.read(byteBuffer);
                } catch (NeedsReadException e) {
                    key.interestOps(SelectionKey.OP_READ); // overwrites previous value
                } catch (NeedsWriteException e) {
                    key.interestOps(SelectionKey.OP_WRITE); // overwrites previous value
                }
            }
            eventIter.remove();
        }
        return readBytes;
    }
}
