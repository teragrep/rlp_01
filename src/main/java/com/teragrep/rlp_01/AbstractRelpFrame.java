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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * An abstract RELP frame class, contains only the header part.
 * 
 */
public abstract class AbstractRelpFrame {

    /**
     * TXNR
     */
    protected int transactionNumber;

    /**
     * COMMAND
     */
    protected String command;

    /**
     * DATALEN
     */
    protected int dataLength;

    /**
     * DATA
     */
    protected byte[] data;

    /**
     Constructor.

     @param command
     Type of command (e.g. "open", "syslog", etc.).
     @param dataLength
     Length of the data in the message.
     */
    protected AbstractRelpFrame(String command, int dataLength) {
        this.command = command;
        this.dataLength = dataLength;
    }

    /**
     Constructor.

     @param txID
     The transaction ID.
     @param command
     Type of command (list of possibilities in RelpCommand.java).
     @param dataLength
     Length of the data in the message.
     */
    protected AbstractRelpFrame(int txID, String command, int dataLength) {
        this.transactionNumber = txID;
        this.command = command;
        this.dataLength = dataLength;
    }

    public String getCommand() {
        return command;
    }

    public int getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(int txID) {
        this.transactionNumber = txID;
    }

    protected String readString(ByteBuffer src, int dataLength) {
        if (dataLength > 0) {
            byte[] bytes = new byte[dataLength];
            src.get(bytes);
            return new String(bytes, StandardCharsets.US_ASCII);
        } else {
            return null;
        }
    }
}
