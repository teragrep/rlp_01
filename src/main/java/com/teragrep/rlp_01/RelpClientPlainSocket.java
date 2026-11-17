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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;

class RelpClientPlainSocket extends RelpClientSocket {

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

    @Override
    public void setKeepAlive(boolean on) {
        socketKeepAlive = on;
    }

    private int writeTimeout = 0;
    private int connectionTimeout = 0;

    private boolean socketKeepAlive = true;

    private SocketChannelable socketChannel;
    private Selectorable poll;

    RelpClientPlainSocket() {
        socketChannel = new SocketChannelableStub();
        poll = new SelectorableStub();
    }

    @Override
    void open(String hostname, int port) throws IOException, TimeoutException {
        if (!this.poll.isStub() && this.poll.selector().isOpen()) {
            // Invalidate all selection key instances in case they were open
            this.poll.selector().close();
        }

        this.poll = new SelectorableImpl(Selector.open());
        this.socketChannel = new SocketChannelableImpl(SocketChannel.open());
        // set KeepAlive
        this.socketChannel.socketChannel().socket().setKeepAlive(socketKeepAlive);
        // Make sure our poll will only block
        this.socketChannel.socketChannel().configureBlocking(false);
        // Poll only for connect
        SelectionKey key = this.socketChannel.socketChannel().register(this.poll.selector(), SelectionKey.OP_CONNECT);
        // Async connect
        this.socketChannel.socketChannel().connect(new InetSocketAddress(hostname, port));
        // Poll for connect
        boolean notConnected = true;
        while (notConnected) {
            int nReady = this.poll.selector().select(this.connectionTimeout);
            // Woke up without anything to do
            if (nReady == 0) {
                throw new TimeoutException("connection timed out");
            }
            // It would be possible to skip the whole iterator, but we want to make sure if something else than connect
            // fires then it will be discarded.
            Set<SelectionKey> polledEvents = this.poll.selector().selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isConnectable()) {
                    if (this.socketChannel.socketChannel().finishConnect()) {
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
        SelectionKey key = this.socketChannel.socketChannel().register(this.poll.selector(), SelectionKey.OP_WRITE);

        while (byteBuffer.hasRemaining()) {
            int nReady = poll.selector().select(this.writeTimeout);
            if (nReady == 0) {
                throw new TimeoutException("write timed out");
            }
            Set<SelectionKey> polledEvents = this.poll.selector().selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isWritable()) {
                    this.socketChannel.socketChannel().write(byteBuffer);
                }
                eventIter.remove();
            }
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }

    @Override
    void close() throws IOException {
        try {
            socketChannel.socketChannel().close();
        }
        catch (ClosedChannelException closedChannelException) {
            // already closed, treat as no-op
        }
        try {
            poll.selector().close();
        }
        catch (ClosedChannelException closedChannelException) {
            // already closed, treat as no-op
        }
    }

    @Override
    int read(ByteBuffer byteBuffer) throws IOException, TimeoutException {
        int readBytes = -1;

        SelectionKey key = this.socketChannel.socketChannel().register(this.poll.selector(), SelectionKey.OP_READ);
        int nReady = poll.selector().select(this.readTimeout);
        if (nReady == 0) {
            throw new TimeoutException("read timed out");
        }
        Set<SelectionKey> polledEvents = this.poll.selector().selectedKeys();
        Iterator<SelectionKey> eventIter = polledEvents.iterator();
        while (eventIter.hasNext()) {
            SelectionKey currentKey = eventIter.next();
            if (currentKey.isReadable()) {
                readBytes = socketChannel.socketChannel().read(byteBuffer);
            }
            eventIter.remove();
        }
        if (readBytes == -1) {
            throw new IOException("read failed");
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        return readBytes;
    }
}
