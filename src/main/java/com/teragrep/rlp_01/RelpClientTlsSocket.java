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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import tlschannel.ClientTlsChannel;
import tlschannel.NeedsReadException;
import tlschannel.NeedsWriteException;

import javax.net.ssl.SSLEngine;

public class RelpClientTlsSocket extends RelpClientSocket {

    private AtomicInteger readTimeout = new AtomicInteger(0);

    @Override
    public int getReadTimeout() {
        return readTimeout.get();
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout.set(readTimeout);
    }

    @Override
    public int getWriteTimeout() {
        return writeTimeout.get();
    }

    @Override
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout.set(writeTimeout);
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout.get();
    }

    @Override
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout.set(connectionTimeout);
    }

    @Override
    public void setKeepAlive(boolean on) {
        socketKeepAlive.set(on);
    }

    private final AtomicInteger writeTimeout = new AtomicInteger(0);
    private final AtomicInteger connectionTimeout = new AtomicInteger(0);

    private final AtomicBoolean socketKeepAlive = new AtomicBoolean(true);

    private final AtomicReference<SocketChannelable> socketChannel;
    private final AtomicReference<Selectorable> selector;

    private final AtomicReference<TLSChannelable> tlsChannel;

    private final Supplier<SSLEngine> sslEngineSupplier;

    RelpClientTlsSocket(Supplier<SSLEngine> sslEngineSupplier) {
        this.sslEngineSupplier = sslEngineSupplier;
        this.socketChannel = new AtomicReference<>(new SocketChannelableStub());
        this.selector = new AtomicReference<>(new SelectorableStub());
        this.tlsChannel = new AtomicReference<>(new TLSChannelableStub());
    }

    @Override
    void open(String hostname, int port) throws IOException, TimeoutException {
        if (!this.selector.get().isStub() && this.selector.get().selector().isOpen()) {
            // Invalidate all selection key instances in case they were open
            this.selector.get().selector().close();
        }

        this.selector.set(new SelectorableImpl(java.nio.channels.Selector.open()));

        this.socketChannel.set(new SocketChannelableImpl(java.nio.channels.SocketChannel.open()));
        // set KeepAlive
        this.socketChannel.get().socketChannel().socket().setKeepAlive(socketKeepAlive.get());
        // Make sure our poll will only block
        this.socketChannel.get().socketChannel().configureBlocking(false);
        // Poll only for connect
        SelectionKey key = this.socketChannel
                .get()
                .socketChannel()
                .register(this.selector.get().selector(), SelectionKey.OP_CONNECT);
        // Async connect
        this.socketChannel.get().socketChannel().connect(new InetSocketAddress(hostname, port));

        SSLEngine sslEngine = sslEngineSupplier.get();
        // force client mode
        sslEngine.setUseClientMode(true);

        ClientTlsChannel.Builder builder = ClientTlsChannel.newBuilder(socketChannel.get().socketChannel(), sslEngine);

        tlsChannel.set(new TLSChannelableImpl(builder.build()));

        // Poll for connect
        boolean notConnected = true;
        while (notConnected) {
            int nReady = this.selector.get().selector().select(this.connectionTimeout.get());
            // Woke up without anything to do
            if (nReady == 0) {
                throw new TimeoutException("connection timed out");
            }
            // It would be possible to skip the whole iterator, but we want to make sure if something else than connect
            // fires then it will be discarded.
            Set<SelectionKey> polledEvents = this.selector.get().selector().selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isConnectable()) {
                    if (this.socketChannel.get().socketChannel().finishConnect()) {
                        // Connection established
                        notConnected = false;
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
        SelectionKey key = this.socketChannel
                .get()
                .socketChannel()
                .register(this.selector.get().selector(), SelectionKey.OP_WRITE);
        while (byteBuffer.hasRemaining()) {
            int nReady = selector.get().selector().select(this.writeTimeout.get());
            if (nReady == 0) {
                throw new TimeoutException("write timed out");
            }
            Set<SelectionKey> polledEvents = this.selector.get().selector().selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                // tlsChannel needs to know about both
                if (currentKey.isWritable() || currentKey.isReadable()) {
                    try {
                        this.tlsChannel.get().tlsChannel().write(byteBuffer);
                    }
                    catch (NeedsReadException e) {
                        key.interestOps(SelectionKey.OP_READ); // overwrites previous value
                    }
                    catch (NeedsWriteException e) {
                        key.interestOps(SelectionKey.OP_WRITE); // overwrites previous value
                    }
                }
                eventIter.remove();
            }
        }
    }

    @Override
    void close() throws IOException {
        if (!tlsChannel.get().isStub()) {
            tlsChannel.get().tlsChannel().close();
        }
        if (!selector.get().isStub()) {
            selector.get().selector().close();
        }
    }

    @Override
    int read(ByteBuffer byteBuffer) throws IOException, TimeoutException {
        int readBytes;
        try {
            // try reading data if it is available from the tlsChannel
            readBytes = tlsChannel.get().tlsChannel().read(byteBuffer);
        }
        catch (NeedsReadException | NeedsWriteException e) {
            // there is no more data, zero read will need selector to work on it
            readBytes = 0;
        }

        if (readBytes == -1) {
            throw new IOException("Read failed, end-of-stream reached");
        }

        if (readBytes == 0) {
            SelectionKey key = this.socketChannel
                    .get()
                    .socketChannel()
                    .register(this.selector.get().selector(), SelectionKey.OP_READ);
            int nReady = selector.get().selector().select(this.readTimeout.get());
            if (nReady == 0) {
                throw new TimeoutException("Read timed out");
            }
            Set<SelectionKey> polledEvents = this.selector.get().selector().selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                // tlsChannel needs to know about both
                if (currentKey.isReadable() || currentKey.isWritable()) {
                    try {
                        readBytes = tlsChannel.get().tlsChannel().read(byteBuffer);
                        if (readBytes == -1) {
                            throw new IOException("Read failed, end-of-stream reached");
                        }
                    }
                    catch (NeedsReadException e) {
                        key.interestOps(SelectionKey.OP_READ); // overwrites previous value
                    }
                    catch (NeedsWriteException e) {
                        key.interestOps(SelectionKey.OP_WRITE); // overwrites previous value
                    }
                }
                eventIter.remove();
            }
        }
        return readBytes;
    }
}
