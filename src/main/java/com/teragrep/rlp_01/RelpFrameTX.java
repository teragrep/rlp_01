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
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.StringTokenizer;

/**
 * This is the frame for transmitting RELP messages.
 * A RELP message as defined in the spec.:
 * Binary PAYLOAD (DATA in the BNF) is supported.
 * 
 */
public class RelpFrameTX extends AbstractRelpFrame implements Writeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpFrameTX.class);
    /**
     * Creates a syslog message with given (possibly binary) data.
     * 
     * @param data
     */
    public RelpFrameTX(byte[] data) {
        this(RelpCommand.SYSLOG, data);
    }

    public RelpFrameTX(String command, byte[] data) {
        super(command, data != null ? data.length : 0);
        this.data = data;
    }

    public RelpFrameTX(String command) {
        this(command, null);
    }

    /**
     * Write the whole RELP message:
     * HEADER DATA TRAILER to the byte buffer.
     */
    public void write(ByteBuffer dst) throws IOException {
        LOGGER.trace("RelpFrameTX.write> entry");
        putHeader(dst);
        putData(dst);
        dst.put((byte)'\n');
        LOGGER.trace("RelpFrameTX.write> exit");
    }

    public int length() throws UnsupportedEncodingException {
        int txn = Integer.toString(this.transactionNumber).getBytes("US-ASCII").length;
        int sp1 = 1;
        int command = this.command.getBytes("US-ASCII").length;
        int sp2 = 1;
        int length = Integer.toString(this.dataLength).getBytes("US-ASCII").length;
        int sp3 = 1;
        int data;
        if (this.data == null) {
            data = 0;
        }
        else {
            data = this.data.length;
        }
        int trailer = 1;
        return txn + sp1 + command + sp2 + length + sp3 + data + trailer;
    }

    /**
     Writes the DATA part of the RELP message into the given buffer
     with a space byte before the actual data.

     @param dst
     The buffer to write the data into.
     */
    private void putData(ByteBuffer dst) {
        LOGGER.trace("RelpFrameTX.putData> entry");
        if (this.data != null) {
                dst.put((byte) ' ');
                dst.put(this.data);
        }
        LOGGER.trace("RelpFrameTX.putData> exit");
    }

    /**
     * Writes a HEADER part of the RELP message to the byte buffer.
     *
     * @param dst
     * @throws UnsupportedEncodingException
     *  Shouldn't happen for US-ASCII..
     */
    private void putHeader(ByteBuffer dst) throws UnsupportedEncodingException {
        LOGGER.trace("RelpFrameTX.putHeader> entry");
        dst.put(Integer.toString(this.transactionNumber).getBytes("US-ASCII"));
        dst.put((byte)' ');
        dst.put(this.command.getBytes("US-ASCII"));
        dst.put((byte)' ');
        dst.put(Integer.toString(this.dataLength).getBytes("US-ASCII"));
        LOGGER.trace("RelpFrameTX.putHeader> exit");
    }

    /**
     Override for toString method. Returns the entire RELP message formatted with spaces.
     */
    @Override
    public String toString() {
        try {
            return this.transactionNumber + " " + this.command + " " + this.dataLength + " " + (this.data != null ? new String(this.data, "UTF-8") : "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
