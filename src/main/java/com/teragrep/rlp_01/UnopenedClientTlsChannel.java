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

import tlschannel.TlsChannel;
import tlschannel.TrackingAllocator;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

public class UnopenedClientTlsChannel implements TlsChannel {

    public UnopenedClientTlsChannel() {

    }

    @Override
    public ByteChannel getUnderlying() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public SSLEngine getSslEngine() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public Consumer<SSLSession> getSessionInitCallback() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public TrackingAllocator getPlainBufferAllocator() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public TrackingAllocator getEncryptedBufferAllocator() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public boolean getRunTasks() {
        throw new IllegalStateException("UnopenedClientTlsChannel does not implement this!");
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public void renegotiate() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public void handshake() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long write(final ByteBuffer[] srcs) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public long read(final ByteBuffer[] dsts) throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    @Override
    public boolean shutdown() throws IOException {
        throw new ClosedChannelException();
    }

    @Override
    public boolean shutdownReceived() {
        return false;
    }

    @Override
    public boolean shutdownSent() {
        return false;
    }
}
