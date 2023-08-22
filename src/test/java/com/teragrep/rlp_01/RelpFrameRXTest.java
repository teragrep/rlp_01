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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RelpFrameRXTest {
	
	@Test
	public void testCode200() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(200, frame.getResponseCode(), "Got wrong response code");
	}
	
	@Test
	public void testCode500() {
		String message = "500 NOK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(500, frame.getResponseCode(), "Got wrong response code");
	}
	
	@Test
	public void testCode123() {
		String message = "123 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(123, frame.getResponseCode(), "Got wrong response code");
	}
	
	@Test
	public void testCodeEmpty() {
		String message = "";
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, 0, createBuffer(message));
			frame.getResponseCode();
		});
	}
	
	@Test
	public void testCodeNoCode() {
		String message = " ";
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
			frame.getResponseCode();
		});
	}
	
	@Test
	public void testCode2000() {
		String message = "2000 OK";
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
			frame.getResponseCode();
		});
	}

	@Test
	public void testGetData() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(message, new String(frame.getData()), "Message is not as expected");
	}

	@Test
	public void testGetCommandSyslog() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.SYSLOG, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testGetCommandClose() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.CLOSE, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.CLOSE, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testGetCommandServerClose() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SERVER_CLOSE, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.SERVER_CLOSE, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testGetCommandOpen() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.OPEN, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.OPEN, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testGetCommandResponse() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.RESPONSE, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.RESPONSE, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testGetCommandAbort() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.ABORT, message.length(), createBuffer(message));
		Assertions.assertEquals(RelpCommand.ABORT, frame.getCommand(), "Command was not as expected");
	}

	@Test
	public void testSetTransactionId() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(2, frame.getTransactionNumber(), "Initial TransactionNumber is not as expected");
		frame.setTransactionNumber(3);
		Assertions.assertEquals(3, frame.getTransactionNumber(), "Changed TransactionNumber is not as expected");
	}

	@Test
	public void testToString() {
		String message = "200 OK";
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, message.length(), createBuffer(message));
		Assertions.assertEquals(
				String.format(
						"2 syslog %s %s\n",
						message.getBytes().length,
						message
				),
				frame.toString(),
				"ToString() is not as expected"
		);
	}

	private ByteBuffer createBuffer(String message) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(message.length());
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		return buffer;
	}
}
