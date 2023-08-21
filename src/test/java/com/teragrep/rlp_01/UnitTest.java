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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnitTest {
	
	@Test
	public void testCode200() {
		String message = "200 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 200);
	}
	
	@Test
	public void testCode500() {
		String message = "500 NOK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 500);
	}
	
	@Test
	public void testCode123() {
		String message = "123 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 123);
	}
	
	@Test
	public void testCodeEmpty() {
		String message = "";
		int len = 0;
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
			frame.getResponseCode();
		});
	}
	
	@Test
	public void testCodeNoCode() {
		String message = " ";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
			frame.getResponseCode();
		});
	}
	
	@Test
	public void testCode2000() {
		String message = "2000 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes(StandardCharsets.UTF_8));
		buffer.flip();
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
			frame.getResponseCode();
		});
	}
}
