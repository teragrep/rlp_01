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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RelpFrameTXTest {
    private final String message = "This is a message with ünïcödë";
    @Test
    public void testConstructorByteArray() {
        RelpFrameTX frame = new RelpFrameTX(message.getBytes());
        Assertions.assertEquals(
                String.format(
                        "0 syslog %s %s",
                        message.getBytes().length,
                        message
                ),
                frame.toString(),
                "Frame toString() differs"
        );
        Assertions.assertEquals(RelpCommand.SYSLOG, frame.getCommand(), "Default command differs");
    }

    @Test
    public void testCommandOpen() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.OPEN);
        Assertions.assertEquals("0 open 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.OPEN, frame.getCommand(), "Open command differs");
    }

    @Test
    public void testCommandResponse() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.RESPONSE);
        Assertions.assertEquals("0 rsp 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.RESPONSE, frame.getCommand(), "Response command differs");
    }

    @Test
    public void testCommandSyslog() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.SYSLOG);
        Assertions.assertEquals("0 syslog 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.SYSLOG, frame.getCommand(), "Syslog command differs");
    }

    @Test
    public void testCommandSyslogWithMessage() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.SYSLOG, message.getBytes());
        Assertions.assertEquals(
                String.format(
                        "0 syslog %s %s",
                        message.getBytes().length,
                        message
                ),
                frame.toString(),
                "frame toString() differs"
        );
        Assertions.assertEquals(RelpCommand.SYSLOG, frame.getCommand(), "Syslog command differs");
    }

    @Test
    public void testCommandAbort() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.ABORT);
        Assertions.assertEquals("0 abort 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.ABORT, frame.getCommand(), "Abort command differs");
    }

    @Test
    public void testCommandClose() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.CLOSE);
        Assertions.assertEquals("0 close 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.CLOSE, frame.getCommand(), "Close command differs");
    }

    @Test
    public void testCommandServerClose() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.SERVER_CLOSE);
        Assertions.assertEquals("0 serverclose 0 ", frame.toString(),"frame toString() differs");
        Assertions.assertEquals(RelpCommand.SERVER_CLOSE, frame.getCommand(), "ServerClose command differs");
    }

    @Test
    public void testSetTransactionId() {
        RelpFrameTX frame = new RelpFrameTX(message.getBytes());
        Assertions.assertEquals(0, frame.getTransactionNumber(), "Initial TransactionNumber differs");
        frame.setTransactionNumber(3);
        Assertions.assertEquals(3, frame.getTransactionNumber(), "Changed TransactionNumber differs");
    }

    @Test
    public void testExplicitCommand() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.OPEN, message.getBytes());
        Assertions.assertEquals(RelpCommand.OPEN, frame.getCommand(), "Explicit command differs");
    }

    @Test
    public void testWrite() {
        RelpFrameTX frame = new RelpFrameTX(RelpCommand.SYSLOG, message.getBytes());
        ByteBuffer buffer = ByteBuffer.allocateDirect(frame.length());
        Assertions.assertDoesNotThrow(() -> frame.write(buffer));
        buffer.flip();
        Assertions.assertEquals(
                String.format(
                        "0 syslog %s %s\n",
                        message.getBytes().length,
                        message
                ),
                StandardCharsets.UTF_8.decode(buffer).toString(),
                "Write results differs"
        );
    }

    @Test
    public void testLength() {
        RelpFrameTX frame = new RelpFrameTX(message.getBytes());
        Assertions.assertEquals(
                String.format(
                        "0 syslog %s %s\n",
                        message.getBytes().length,
                        message
                ).getBytes().length,
                frame.length(),
                "Frame length() differs"
        );
    }

    @Test
    public void testToString() {
        RelpFrameTX frame = new RelpFrameTX(message.getBytes());
        frame.setTransactionNumber(14);
        Assertions.assertEquals(
                String.format(
                        "14 syslog %s %s",
                        message.getBytes().length,
                        message
                ),
                frame.toString(),
                "frame toString() differs"
        );
    }
}
