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

import java.nio.ByteBuffer;

public class RelpParser {

    // perhaps antlr4 would be better for this and not some hand made parser
    private relpParserState state;
    private boolean isComplete;

    // storage
    private String responseTxnIdString;
    private int responseTxnId;
    private String responseCommandString;
    private String responseLengthString;
    private int responseLength;
    private int responseLengthLeft;
    private ByteBuffer responseData;

    public RelpParser() {
        this.state = relpParserState.TXN;
        this.responseTxnIdString = "";
        this.responseTxnId = -1;
        this.responseCommandString= "";
        this.responseLengthString= "";
        this.responseLength = -1;
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    public int getTxnId() {
        return this.responseTxnId;
    }

    public String getCommandString() {
        return this.responseCommandString;
    }

    public int getLength() {
        return this.responseLength;
    }

    public ByteBuffer getData() {
        return this.responseData;
    }

    private enum relpParserState {
        TXN,
        COMMAND,
        LENGTH,
        DATA,
        NL
    }

    // TODO add SPEC constraints to parsing and throw exceptions if they are breached

    public void parse(byte b) {
        switch (this.state) {
            case TXN:
                if (b == ' '){
                    responseTxnId = Integer.parseInt(responseTxnIdString);
                    state = relpParserState.COMMAND;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> txnId: " + responseTxnId);
                    }
                }
                else {
                    responseTxnIdString += new String(new byte[] {b});
                }
                break;
            case COMMAND:
                if (b == ' '){
                    state = relpParserState.LENGTH;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> command: " + responseCommandString);
                    }
                }
                else {
                    responseCommandString += new String(new byte[] {b});
                }
                break;
            case LENGTH:
                /*
                 '\n' is especially for our dear librelp which should follow:
                 HEADER = TXNR SP COMMAND SP DATALEN SP;
                 but sometimes librelp follows:
                 HEADER = TXNR SP COMMAND SP DATALEN LF; and LF is for relpParserState.NL
                 */
                if (b == ' ' || b == '\n'){
                    responseLength = Integer.parseInt(responseLengthString);
                    responseLengthLeft = responseLength;
                    // allocate buffer
                    responseData = ByteBuffer.allocateDirect(responseLength);

                    state = relpParserState.DATA;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> length: " + responseLengthString);
                    }
                    if (b == '\n') {
                        if (responseLength == 0) {
                            this.isComplete = true;
                        }
                        if (System.getenv("RELP_DEBUG") != null) {
                            System.out.println("relpParser> newline after LENGTH: " + new String(new byte[] {b}));
                        }
                    }
                }
                else {
                    responseLengthString += new String(new byte[] {b});
                }
                break;
            case DATA:
                if (responseLengthLeft > 0) {
                    responseData.put(b);
                    responseLengthLeft--;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> data b: " + new String(new byte[] {b}) + " left: " + responseLengthLeft);
                    }
                }
                if (responseLengthLeft == 0) {
                    // make ready for consumer
                    responseData.flip();
                    state = relpParserState.NL;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> data: " + responseData.toString());
                    }
                }
                break;
            case NL:
                if (b == '\n'){
                    this.isComplete = true;
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpParser> newline: " + new String(new byte[] {b}));
                    }
                }
                else {
                    throw new IllegalStateException("relp response parsing failure");
                }
                break;
            default:
                break;
        }
    }
}
