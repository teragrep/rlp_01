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
 A hand-made parser to process RELP messages.
 */
public class RelpParser {

    private static final int MAX_COMMAND_LENGTH  = 11;

    // perhaps antlr4 would be better for this and not some hand made parser
    private RelpParserState state = RelpParserState.TXN;
    private boolean isComplete = false;
    private final ByteBuffer txnIdBuffer = ByteBuffer.allocateDirect(String.valueOf(TxID.MAX_ID).length());
    private int frameTxnId = -1;
    private final ByteBuffer commandBuffer = ByteBuffer.allocateDirect(MAX_COMMAND_LENGTH);
    private String frameCommand = "";
    private final ByteBuffer lengthBuffer = ByteBuffer.allocateDirect(String.valueOf(Integer.MAX_VALUE).length());
    private int frameLength = -1;
    private int frameLengthLeft;
    private ByteBuffer frameData;


    public RelpParser() {

    }

    @Deprecated
    public RelpParser( boolean debug ) {

    }

    private String byteBufferToAsciiString(ByteBuffer byteBuffer) {
        return StandardCharsets.US_ASCII.decode(byteBuffer).toString();
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    public int getTxnId() {
        return this.frameTxnId;
    }

    public String getCommandString() {
        return frameCommand;
    }

    public int getLength() {
        return this.frameLength;
    }

    public ByteBuffer getData() {
        return this.frameData;
    }

    @Override
    public String toString() {
        return "RelpParser{" +
                "state=" + state +
                '}';
    }

    private enum RelpParserState {
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
    public void parse(byte b) {
        if(this.isComplete) {
            throw new IllegalStateException("parser was not reset after completing");
        }
        switch (this.state) {
            case TXN:
                if (b == ' '){
                    txnIdBuffer.flip();
                    frameTxnId = Integer.parseInt(byteBufferToAsciiString(txnIdBuffer));
                    if (frameTxnId < 0) {
                        throw new IllegalArgumentException("TXNR must be >= 0");
                    }
                    state = RelpParserState.COMMAND;
                }
                else {
                    txnIdBuffer.put(b);
                }
                break;
            case COMMAND:
                if (b == ' '){
                    commandBuffer.flip();
                    // Spec constraints.
                    frameCommand = byteBufferToAsciiString(commandBuffer);
                    if(
                            !frameCommand.equals(RelpCommand.OPEN) &&
                            !frameCommand.equals(RelpCommand.CLOSE) &&
                            !frameCommand.equals(RelpCommand.ABORT) &&
                            !frameCommand.equals(RelpCommand.SERVER_CLOSE) &&
                            !frameCommand.equals(RelpCommand.SYSLOG) &&
                            !frameCommand.equals(RelpCommand.RESPONSE)) {
                        throw new IllegalStateException( "Invalid COMMAND." );
                    }
                    state = RelpParserState.LENGTH;
                }
                else {
                    commandBuffer.put(b);
                }
                break;
            case LENGTH:
                /*
                 '\n' is especially for our dear librelp which should follow:
                 HEADER = TXNR SP COMMAND SP DATALEN SP;
                 but sometimes librelp follows:
                 HEADER = TXNR SP COMMAND SP DATALEN LF; and LF is for relpParserState.NL
                 */
                if (b == ' ' || b == '\n') {
                    lengthBuffer.flip();
                    frameLength = Integer.parseInt(byteBufferToAsciiString(lengthBuffer));

                    if (frameLength < 0) {
                        throw new IllegalArgumentException("DATALEN must be >= 0");
                    }

                    frameLengthLeft = frameLength;
                    frameData = ByteBuffer.allocateDirect(frameLength);

                    // Length bytes done, move onto next state.
                    if (frameLength == 0 ) {
                        state = RelpParserState.NL;
                    } else {
                        state = RelpParserState.DATA;
                    }
                    if (b == '\n') {
                        if (frameLength == 0) {
                            this.isComplete = true;
                        }
                    }
                }
                else {
                    lengthBuffer.put(b);
                }
                break;
            case DATA:
                // Parser will only read the given length of data. If the message
                // gives data bigger than the frameLength, bad luck for them.
                if (frameLengthLeft > 0) {
                    frameData.put(b);
                    frameLengthLeft--;
                }
                if (frameLengthLeft == 0) {
                    // make ready for consumer
                    frameData.flip();
                    state = RelpParserState.NL;
                }
                break;
            case NL:
                if (b == '\n'){
                    // RELP message always ends with a newline byte.
                    this.isComplete = true;
                }
                else {
                    throw new IllegalStateException("relp frame parsing failure");
                }
                break;
            default:
                break;
        }
    }

    public void reset() {
        state = RelpParserState.TXN;
        isComplete = false;
        txnIdBuffer.clear();
        frameTxnId = -1;
        commandBuffer.clear();
        frameCommand = "";
        lengthBuffer.clear();
        frameLength = -1;
        frameLengthLeft = 0;
        frameData = null;
    }
}
