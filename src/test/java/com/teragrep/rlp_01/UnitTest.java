package com.teragrep.rlp_01;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UnitTest {
	
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();
	
	@Test
	public void testCode200() throws UnsupportedEncodingException {
		String message = "200 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 200);
	}
	
	@Test
	public void testCode500() throws UnsupportedEncodingException {
		String message = "500 NOK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 500);
	}
	
	@Test
	public void testCode123() throws UnsupportedEncodingException {
		String message = "123 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		assertEquals(frame.getResponseCode(), 123);
	}
	
	@Test
	public void testCodeEmpty() throws UnsupportedEncodingException {
		String message = "";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("response code not present");
		frame.getResponseCode();		
	}
	
	@Test
	public void testCodeNoCode() throws UnsupportedEncodingException {
		String message = " ";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("response code not a number");
		frame.getResponseCode();		
	}
	
	@Test
	public void testCode2000() throws UnsupportedEncodingException {
		String message = "2000 OK";
		int len = message.length();
		ByteBuffer buffer = ByteBuffer.allocateDirect(len);
		buffer.put(message.getBytes("UTF-8"));
		buffer.flip();
		RelpFrameRX frame = new RelpFrameRX(2, RelpCommand.SYSLOG, len, buffer);
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("response code too long");
		frame.getResponseCode();		
	}
}
