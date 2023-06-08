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
 * The RELP response contains the response header,
 * which is the same as in RELP requests. Then there is DATA part.
 * 
 */
public class RelpFrameRX extends AbstractRelpFrame {
    /**
     * PAYLOAD
     */

    public RelpFrameRX(int txID, String command, int dataLength, ByteBuffer src) {
        super(txID, command, dataLength);
        this.data = new byte[src.remaining()];
        src.get(this.data);
    }

    public byte[] getData() {
        return data;
    }

    /**
     An override for the toString() method. Builds a string (including spaces and
     newline trailer at the end) from the RELP response frame.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append( this.transactionNumber );
        stringBuilder.append( ' ' );
        stringBuilder.append( this.command );
        stringBuilder.append( ' ' );
        stringBuilder.append( this.dataLength );
        if( this.data != null ) {
            stringBuilder.append( ' ' );
            stringBuilder.append( new String(this.data) );
        }
        stringBuilder.append( '\n' );
        return stringBuilder.toString();
    }

    /**
     RELP response is structured as: RESPONSE-CODE SP [HUMANMSG] [LF CMDDATA]
     Therefore, response code is extracted by taking a substring up to the first
     space character and parsing it as an integer.

     @return response code of the RELP response. 200 is OK, all the rest are errors (currently).
     */
    public int getResponseCode()
    {
        /*
        TODO this is SYSLOG command specific, move somewhere else
        */
        int position = 0;
        byte[] code = new byte[3];

        for (byte datum : data) {
            if (position == 3 && datum == ' ') {
                // three numbers and a space, means it's a code
                return Integer.parseInt(new String(code));
            }
            else if (position >= 3) {
                throw new IllegalArgumentException("response code too long");
            }

            if (datum >= 48 && datum <= 57) { // 0-9 in ascii dec
                code[position] = datum;
            }
            else {
                throw new IllegalArgumentException("response code not a number");
            }

            position++;
        }
        throw new IllegalArgumentException("response code not present");
    }
}
