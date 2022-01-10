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
    private String frameTxnIdString;
    private int frameTxnId;
    private String frameCommandString;
    private String frameLengthString;
    private int frameLength;
    private int frameLengthLeft;
    private ByteBuffer frameData;
    private final boolean debugEnabled;

    private static final int MAX_COMMAND_LENGTH  = 11;

    public RelpParser() {
        this.state = relpParserState.TXN;
        this.frameTxnIdString = "";
        this.frameTxnId = -1;
        this.frameCommandString= "";
        this.frameLengthString= "";
        this.frameLength = -1;
        if( System.getenv( "RELP_DEBUG" ) != null ) {
            debugEnabled = true;
        }
        else {
            debugEnabled = false;
        }
    }

    public RelpParser( boolean debug )
    {
        this.state = relpParserState.TXN;
        this.frameTxnIdString = "";
        this.frameTxnId = -1;
        this.frameCommandString = "";
        this.frameLengthString = "";
        this.frameLength = -1;
        debugEnabled = debug;
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    public int getTxnId() {
        return this.frameTxnId;
    }

    public String getCommandString() {
        return this.frameCommandString;
    }

    public int getLength() {
        return this.frameLength;
    }

    public ByteBuffer getData() {
        return this.frameData;
    }

    public relpParserState getState() { return this.state; }

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
                    frameTxnId = Integer.parseInt(frameTxnIdString);
                    state = relpParserState.COMMAND;
                    if( debugEnabled ) {
                        System.out.println( "relpParser> txnId: " + frameTxnId );
                    }
                }
                else {
                    frameTxnIdString += new String(new byte[] {b});
                }
                break;
            case COMMAND:
                if (b == ' '){
                    state = relpParserState.LENGTH;
                    if( debugEnabled ) {
                        System.out.println( "relpParser> command: " + frameCommandString );
                    }
                    // Spec constraints.
                    if( frameCommandString.length() > MAX_COMMAND_LENGTH &&
                            !frameCommandString.equals(RelpCommand.OPEN) &&
                            !frameCommandString.equals(RelpCommand.CLOSE) &&
                            !frameCommandString.equals(RelpCommand.ABORT) &&
                            !frameCommandString.equals(RelpCommand.SERVER_CLOSE) &&
                            !frameCommandString.equals(RelpCommand.SYSLOG) &&
                            !frameCommandString.equals(RelpCommand.RESPONSE)) {
                        throw new IllegalStateException( "Invalid COMMAND." );
                    }
                }
                else {
                    frameCommandString += new String(new byte[] {b});
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
                    frameLength = Integer.parseInt(frameLengthString);
                    // Length of data can't be longer than Long.MAX_VALUE to avoid parsing
                    // spurious messages.
                    if( frameLength > 128000 ) {
                        throw new IllegalStateException( "Invalid DATALEN value." );
                    }
                    frameLengthLeft = frameLength;
                    // allocate buffer
                    frameData = ByteBuffer.allocateDirect(frameLength);

                    state = relpParserState.DATA;
                    // Length bytes done, move onto next state.
                    if (frameLength == 0 ) {
                        state = relpParserState.NL;
                    } else {
                        state = relpParserState.DATA;
                    }
                    if( debugEnabled ) {
                        System.out.println( "relpParser> length: " + frameLengthString );
                    }
                    if (b == '\n') {
                        if (frameLength == 0) {
                            this.isComplete = true;
                        }
                        if( debugEnabled ) {
                            System.out.println( "relpParser> newline after LENGTH: " + new String( new byte[] {b} ) );
                        }
                    }
                }
                else {
                    frameLengthString += new String(new byte[] {b});
                }
                break;
            case DATA:
                if(this.isComplete) this.state = relpParserState.NL;
                if (frameLengthLeft > 0) {
                    frameData.put(b);
                    frameLengthLeft--;
                    if( debugEnabled ) {
                        System.out.println( "relpParser> data b: " + new String( new byte[] {b} ) + " left: " + frameLengthLeft );
                    }
                }
                if (frameLengthLeft == 0) {
                    // make ready for consumer
                    frameData.flip();
                    state = relpParserState.NL;
                    if( debugEnabled ) {
                        System.out.println( "relpParser> data buffer: " + frameData.toString() );
                    }
                }
                break;
            case NL:
                if (b == '\n'){
                    this.isComplete = true;
                    if( debugEnabled ) {
                        System.out.println( "relpParser> newline: " + new String( new byte[] {b} ) );
                    }
                }
                else {
                    throw new IllegalStateException("relp frame parsing failure");
                }
                break;
            default:
                break;
        }
    }
}
