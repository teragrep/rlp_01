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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class RelpParserTest {
    public RelpParser createParser(String message) {
        RelpParser parser = new RelpParser();
        for(byte b : message.getBytes()) {
            parser.parse(b);
        }
        return parser;
    }

    @Test
    public void testHappyPath() {
        RelpParser parser = createParser("2 rsp 6 200 OK\n");
        Assertions.assertEquals(2, parser.getTxnId(), "Wrong transaction id received");
        Assertions.assertEquals("rsp", parser.getCommandString(), "Wrong command received");
        Assertions.assertEquals(6, parser.getLength(), "Wrong length received");
        Assertions.assertEquals("200 OK", StandardCharsets.UTF_8.decode(parser.getData()).toString(), "Data does not match");
        Assertions.assertTrue(parser.isComplete(), "Parser is not finished");
    }

    @Test
    public void testNegativeTxnr() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createParser("-2 rsp -6 200 OK\n"));
    }

    @Test
    public void testNegativeLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createParser("2 rsp -6 200 OK\n"));
    }

    @Test
    public void testEndsWithoutNL() {
        RelpParser parser = createParser("2 rsp 6 200 OK");
        Assertions.assertFalse(parser.isComplete(), "Parser is complete");
    }

    @Test
    public void testCommandInvalid() {
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("2 horse 6 200 OK\n"));
    }

    @Test
    public void testCommandOpen() {
        RelpParser parser = createParser("0 open 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("open", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testCommandClose() {
        RelpParser parser = createParser("0 close 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("close", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testCommandAbort() {
        RelpParser parser = createParser("0 abort 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("abort", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testCommandServerClose() {
        RelpParser parser = createParser("0 serverclose 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("serverclose", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testCommandSyslog() {
        RelpParser parser = createParser("0 syslog 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("syslog", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testCommandResponse() {
        RelpParser parser = createParser("0 rsp 0\n");
        Assertions.assertTrue(parser.isComplete(), "Parser is not complete");
        Assertions.assertEquals("rsp", parser.getCommandString(), "Parser got wrong command");
    }

    @Test
    public void testReset() {
        RelpParser parser = createParser("0 rsp 6 200 OK\n");
        parser.reset();
        Assertions.assertFalse(parser.isComplete(), "Parser should be in incomplete state");
        Assertions.assertEquals(-1, parser.getTxnId(), "TxnId was incorrectly reset");
        Assertions.assertEquals("", parser.getCommandString(), "Command was incorrectly reset");
        Assertions.assertEquals(-1, parser.getLength(), "Length was incorrectly reset");
        Assertions.assertNull( parser.getData(), "Data was incorrectly reset");
    }

    @Test
    public void testNonNumericTxnId() {
        Assertions.assertThrows(NumberFormatException.class, () -> createParser("Seven rsp 6 200 OK\n"));
    }

    @Test
    public void testNonNumericLength() {
        Assertions.assertThrows(NumberFormatException.class, () -> createParser("0 rsp six 200 OK\n"));
    }

    @Test
    public void testTooLongMessage() {
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("0 rsp 6 My Message Is Too Long"));
    }

    @Test
    public void testMultipleLastNewlines() {
        RelpParser parser = createParser("0 rsp 3 six\n\n\n\n\n\n");
        Assertions.assertEquals("six", StandardCharsets.UTF_8.decode(parser.getData()).toString(), "Parser got too much to data");
        Assertions.assertTrue(parser.isComplete(), "Parser should have completed");
    }

    @Test
    public void testDataAfterLastNewline() {
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("0 rsp 3 six\n\nBonjour"));
    }

    @Disabled(value="Triggers BufferOverflow and is not gracefully handled")
    @Test
    public void testVeryLongTxnId() {
        createParser("99999999999999999999 rsp 6 200 OK\n");
    }

    @Disabled(value="Triggers BufferOverflow and is not gracefully handled")
    @Test
    public void testVeryLongCommand() {
        createParser("0 ThisShouldBeVeryLongCommandThatBreaksThings 6 200 OK\n");
    }

    @Disabled(value="Triggers BufferOverflow and is not gracefully handled")
    @Test
    public void testVeryLongContentLength() {
        createParser("0 rsp 99999999999999999999 200 OK\n");
    }
}
