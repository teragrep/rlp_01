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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;

class RelpClientPlainSocket extends RelpClientSocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpClientPlainSocket.class);

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

    private boolean socketKeepAlive = false;

    private SocketChannel socketChannel;
    private Selector poll;


    RelpClientPlainSocket() {

    }

    @Override
    void open (String hostname, int port) throws IOException, TimeoutException {
        if (this.poll != null && this.poll.isOpen()) {
            // Invalidate all selection key instances in case they were open
            this.poll.close();
        }

        this.poll = Selector.open();

        this.socketChannel = SocketChannel.open();
        // set KeepAlive
        this.socketChannel.socket().setKeepAlive(socketKeepAlive);
        // Make sure our poll will only block
        this.socketChannel.configureBlocking(false);
        // Poll only for connect
        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_CONNECT);
        // Async connect
        this.socketChannel.connect(new InetSocketAddress(hostname, port));
        // Poll for connect
        boolean notConnected = true;
        while (notConnected) {
            int nReady = this.poll.select(this.connectionTimeout);
            // Woke up without anything to do
            if (nReady == 0) {
                throw new TimeoutException("connection timed out");
            }
            // It would be possible to skip the whole iterator, but we want to make sure if something else than connect
            // fires then it will be discarded.
            Set<SelectionKey> polledEvents = this.poll.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isConnectable()) {
                    if (this.socketChannel.finishConnect()) {
                        // Connection established
                        notConnected = false;
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("relpConnection> established");
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
        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_WRITE);
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("relpConnection.sendRelpRequestAsync> need to write <{}> ", byteBuffer.hasRemaining());
        }

        while (byteBuffer.hasRemaining()) {
            int nReady = poll.select(this.writeTimeout);
            if (nReady == 0) {
                throw new TimeoutException("write timed out");
            }
            Set<SelectionKey> polledEvents = this.poll.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isWritable()) {
                    LOGGER.trace("relpConnection.sendRelpRequestAsync> became writable");
                    this.socketChannel.write(byteBuffer);
                }
                eventIter.remove();
            }
            if(LOGGER.isTraceEnabled()) {
                LOGGER.trace("relpConnection.sendRelpRequestAsync> still need to write <{}>", byteBuffer.hasRemaining());
            }
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }

    @Override
    void close() throws IOException {
        socketChannel.close();
        poll.close();
    }

    @Override
    int read(ByteBuffer byteBuffer) throws IOException, TimeoutException {
        int readBytes = -1;

        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_READ);
        int nReady = poll.select(this.readTimeout);
        if (nReady == 0) {
            throw new TimeoutException("read timed out");
        }
        Set<SelectionKey> polledEvents = this.poll.selectedKeys();
        Iterator<SelectionKey> eventIter = polledEvents.iterator();
        while (eventIter.hasNext()) {
            SelectionKey currentKey = eventIter.next();
            if (currentKey.isReadable()) {
                LOGGER.trace("relpConnection.readAcks> became readable");
                readBytes = socketChannel.read(byteBuffer);
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
