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
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Set;

/**
 * SocketChannel is designed to be initialized via static method SocketChannel.open(), creating an opened SocketChannel.
 * This subclass can be used to represent a SocketChannel that has not yet been opened.
 */
public class UnopenedTLSChannel extends SocketChannel {

    public UnopenedTLSChannel() {
        this(SelectorProvider.provider());
    }

    public UnopenedTLSChannel(final SelectorProvider provider) {
        super(provider);
        try {
            // Calling close() immediately in this constructor sets the internal state of the SocketChannel so that it behaves as if it has been closed.
            super.close();
        }
        catch (IOException e) {
            // close() should never throw in this implementation as long as implCloseSelectableChannel() doesn't throw, but must be caught to satisfy compiler.
            throw new RuntimeException("Failed initialize an UnopenedSocketChannel!", e);
        }
    }

    @Override
    public SocketChannel bind(final SocketAddress socketAddress) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public <T> SocketChannel setOption(final SocketOption<T> socketOption, final T t) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public <T> T getOption(final SocketOption<T> socketOption) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return new HashSet<>();
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public Socket socket() {
        throw new RuntimeException("UnopenedSocketChannel does not have a Socket!");
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnectionPending() {
        return false;
    }

    @Override
    public boolean connect(final SocketAddress socketAddress) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public boolean finishConnect() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public int read(final ByteBuffer byteBuffer) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long read(final ByteBuffer[] byteBuffers, final int i, final int i1) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public int write(final ByteBuffer byteBuffer) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long write(final ByteBuffer[] byteBuffers, final int i, final int i1) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        // no-op. This is called via AbstractInterruptibleChannel.close() -> AbstractSelectableChannel.implCloseSelectableChannel()
    }

    @Override
    protected void implConfigureBlocking(final boolean b) throws IOException {
        throw new ClosedChannelException();
    }
}
