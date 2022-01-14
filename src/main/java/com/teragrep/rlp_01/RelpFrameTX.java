/*
 * Java Reliable Event Logging Protocol Library RLP-01
 * Copyright (C) 2021  Suomen Kanuuna Oy
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 *  
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *  
 * If you modify this Program, or any covered work, by linking or combining it 
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *  
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *  
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *  
 * Names of the licensors and authors may not be used for publicity purposes.
 *  
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *  
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *  
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */

package com.teragrep.rlp_01;

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
    /**
     * Creates a syslog message with given (possibly binary) data.
     * 
     * @param data
     */
    RelpFrameTX(byte[] data) {
        this(RelpCommand.SYSLOG, data);
    }

    RelpFrameTX(String command, byte[] data) {
        super(command, data != null ? data.length : 0);
        this.data = data;
    }

    RelpFrameTX(String command) {
        this(command, null);
    }

    /**
     * Write the whole RELP message:
     * HEADER DATA TRAILER to the byte buffer.
     */
    public void write(ByteBuffer dst) throws IOException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.write> entry");
        }
        putHeader(dst);        
        putData(dst);
        dst.put((byte)'\n');
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.write> exit");
        }
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
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.putData> entry");
        }
        if (this.data != null) {
                dst.put((byte) ' ');
                dst.put(this.data);
        }
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.putData> exit");
        }
    }

    /**
     * Writes a HEADER part of the RELP message to the byte buffer.
     *
     * @param dst
     * @throws UnsupportedEncodingException
     *  Shouldn't happen for US-ASCII..
     */
    private void putHeader(ByteBuffer dst) throws UnsupportedEncodingException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.putHeader> entry");
        }
        dst.put(Integer.toString(this.transactionNumber).getBytes("US-ASCII"));
        dst.put((byte)' ');
        dst.put(this.command.getBytes("US-ASCII"));
        dst.put((byte)' ');
        dst.put(Integer.toString(this.dataLength).getBytes("US-ASCII"));
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("RelpFrameTX.putHeader> exit");
        }
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
