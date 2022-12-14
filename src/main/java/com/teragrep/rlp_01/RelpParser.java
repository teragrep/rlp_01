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

/**
 A hand-made parser to process RELP messages.
 */
public class RelpParser {

    // perhaps antlr4 would be better for this and not some hand made parser
    private relpParserState state;
    private boolean isComplete;

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

    /**
     Parse the message byte-by-byte and enter each byte as a string to the proper
     storage based on the state of the parser.

     @param b
     Byte to be parsed.
     */
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
                    // Length of data can't be longer than Integer.MAX_VALUE to avoid parsing
                    // spurious messages.
                    if( frameLength > Integer.MAX_VALUE ) {
                        throw new IllegalStateException( "Invalid DATALEN value." );
                    }
                    frameLengthLeft = frameLength;
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
                // Parser will only read the given length of data. If the message
                // gives data bigger than the frameLength, bad luck for them.
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
                    // RELP message always ends with a newline byte.
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
