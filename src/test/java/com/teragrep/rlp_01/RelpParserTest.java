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
import java.util.HashMap;

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
        Assertions.assertEquals(2, parser.getTxnId(), "parser getTxnId() differs");
        Assertions.assertEquals("rsp", parser.getCommandString(), "parser getCommandString() differs");
        Assertions.assertEquals(6, parser.getLength(), "parser getLength() differs");
        Assertions.assertEquals("200 OK", StandardCharsets.UTF_8.decode(parser.getData()).toString(), "parser.getData() differs");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
    }

    @Test
    public void testNegativeTxnr() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createParser("-2 rsp 6 200 OK\n"));
    }

    @Test
    public void testNegativeLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createParser("2 rsp -6 200 OK\n"));
    }

    @Test
    public void testEndsWithoutNL() {
        RelpParser parser = createParser("2 rsp 6 200 OK");
        Assertions.assertFalse(parser.isComplete(), "parser isComplete() differs");
    }

    @Test
    public void testCommandInvalid() {
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("2 horse 6 200 OK\n"));
    }

    @Test
    public void testCommandOpen() {
        RelpParser parser = createParser("0 open 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("open", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testCommandClose() {
        RelpParser parser = createParser("0 close 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("close", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testCommandAbort() {
        RelpParser parser = createParser("0 abort 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("abort", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testCommandServerClose() {
        RelpParser parser = createParser("0 serverclose 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("serverclose", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testCommandSyslog() {
        RelpParser parser = createParser("0 syslog 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("syslog", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testCommandResponse() {
        RelpParser parser = createParser("0 rsp 0\n");
        Assertions.assertTrue(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals("rsp", parser.getCommandString(), "parser getCommandString() differs");
    }

    @Test
    public void testReset() {
        RelpParser parser = createParser("0 rsp 6 200 OK\n");
        parser.reset();
        Assertions.assertFalse(parser.isComplete(), "parser isComplete() differs");
        Assertions.assertEquals(-1, parser.getTxnId(), "parser getTxnId() differs");
        Assertions.assertEquals("", parser.getCommandString(), "parser getCommandString() differs");
        Assertions.assertEquals(-1, parser.getLength(), "parser getLength() differs");
        Assertions.assertNull(parser.getData(), "parser getData() differs");
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
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("0 rsp 3 six\n\n\n\n\n\n"));
    }

    @Test
    public void testDataAfterLastNewline() {
        Assertions.assertThrows(IllegalStateException.class, () -> createParser("0 rsp 3 six\n\nBonjour"));
    }

    @Test
    public void testMultipleMessagesInRow() {
        String message = "0 rsp 3 six\n1 rsp 4 four\n2 rsp 5 five!\n";
        HashMap<Integer, String> results = new HashMap<>();
        RelpParser parser = new RelpParser();
        for(byte b : message.getBytes()) {
            parser.parse(b);
            if(parser.isComplete()) {
                results.put(parser.getTxnId(), StandardCharsets.UTF_8.decode(parser.getData()).toString());
                parser.reset();
            }
        }
        Assertions.assertEquals("six", results.get(0), "parser getData() differs");
        Assertions.assertEquals("four", results.get(1), "parser getData() differs");
        Assertions.assertEquals("five!", results.get(2), "parser getData() differs");
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
