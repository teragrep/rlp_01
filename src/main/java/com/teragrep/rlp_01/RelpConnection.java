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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Abstract the concept of RELP session: it handles the
 * handshake with the RELP server, sends RELP messages
 * and receives replies.
 * 
 */
public class RelpConnection implements RelpSender {

    private int rxBufferSize;
    private int txBufferSize;
    private ByteBuffer preAllocatedTXBuffer;
    private static final int MAX_COMMAND_LENGTH  = 11;

    private final static byte[] OFFER;
    
    static {
        try {
            OFFER = ("\nrelp_version=0\nrelp_software=RLP-01\ncommands=" + RelpCommand.SYSLOG + "\n").getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private enum RelpConnectionState {
        CLOSED,
        OPEN,
        COMMIT
    }

    
    private String hostname;
    private int port;
    private SocketChannel socketChannel;
    private Selector poll;

    private int readTimeout = 0;
    private int writeTimeout = 0;
    private int connectionTimeout = 0;

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return this.writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int timeout) {
		this.connectionTimeout = timeout;
	}

	public int getRxBufferSize() {
	    return this.rxBufferSize;
    }

    public void setRxBufferSize(int size) {
	    this.rxBufferSize = size;
    }

    public int getTxBufferSize() {
	    return this.txBufferSize;
    }

    public void setTxBufferSize(int size) {
	    this.txBufferSize = size;
    }


	private RelpConnectionState state;
    
    /**
     * The TXNR generator object.
     */
    private TxID txID;

    // window for the connection
    private RelpWindow window;

    public RelpConnection() {
        this.rxBufferSize = 512;
        this.txBufferSize = 262144;

        this.state = RelpConnectionState.CLOSED;
        this.preAllocatedTXBuffer = ByteBuffer.allocateDirect(this.txBufferSize);
    }

    
    /**
     * Creates a new RELP session with given server details and connects into it and
     * does the "open session" command.
     * 
     * @throws IOException
     */
    public boolean connect(String hostname, int port) throws IOException, IllegalStateException, TimeoutException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.connect> entry");
        }
        if (state != RelpConnectionState.CLOSED) {
            throw new IllegalStateException("Session is not closed.");
        }

        //
        this.txID = new TxID();
        this.window = new RelpWindow();

        this.hostname = hostname;
        this.port = port;
        this.createSocketChannel();

        // send open session message
        RelpFrameTX relpRequest = new RelpFrameTX(RelpCommand.OPEN, OFFER);
        RelpBatch connectionOpenBatch = new RelpBatch();
        long reqId = connectionOpenBatch.putRequest(relpRequest);
        this.sendBatch(connectionOpenBatch);
        boolean openSuccess = connectionOpenBatch.verifyTransaction(reqId);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.connect> exit with: " + openSuccess);
        }
        if (openSuccess) {
            this.state = RelpConnectionState.OPEN;
        }
        return openSuccess;
    }

    public void tearDown()  {
        try {
            this.socketChannel.close();
        }
        catch (IOException e) {
            ; // don't care
        }
        this.state = RelpConnectionState.CLOSED;
    }

    /**
     Sends a "close session" command to disconnect from the session by creating a "close session"
     request. (Similar to connect())

     */
    public boolean disconnect() throws IOException, IllegalStateException, TimeoutException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.disconnect> entry");
        }
        if (state != RelpConnectionState.OPEN) {
            throw new IllegalStateException("Session is not in open state, can not close.");
        }
        RelpFrameTX relpRequest = new RelpFrameTX(RelpCommand.CLOSE);
        RelpBatch connectionCloseBatch = new RelpBatch();
        long reqId = connectionCloseBatch.putRequest(relpRequest);
        this.sendBatch(connectionCloseBatch);
        boolean closeSuccess = false;
        RelpFrameRX closeResponse = connectionCloseBatch.getResponse(reqId);
        if (closeResponse.dataLength == 0) {
            closeSuccess = true;
        }
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.disconnect> exit with: " + closeSuccess);
        }
        if(closeSuccess){
            this.socketChannel.close();
            this.state = RelpConnectionState.CLOSED;
        }
        return closeSuccess;
    }

    public void commit(RelpBatch relpBatch) throws IOException, IllegalStateException, TimeoutException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.commit> entry");
        }
        if (this.state != RelpConnectionState.OPEN) {
            throw new IllegalStateException("Session is not in open state, can not commit.");
        }
        this.state = RelpConnectionState.COMMIT;
        this.sendBatch(relpBatch);
        this.state = RelpConnectionState.OPEN;
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.commit> exit");
        }
    }

    /**
     Processes all the jobs in the workQueue of the given batch by iterating
     through each requestId, retrieving the request frame associated with the id,
     setting a linearly incremented txID and sending the request to server. Finally
     calls readAcks to make sure the requests went through and received a response.

     */
    private void sendBatch(RelpBatch relpBatch)  throws IOException, TimeoutException, IllegalStateException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendBatch> entry with wq len " + relpBatch.getWorkQueueLength());
        }
        // send a batch of requests..
        RelpFrameTX relpRequest;

        while (relpBatch.getWorkQueueLength() > 0) {
            long reqId = relpBatch.popWorkQueue();
            relpRequest = relpBatch.getRequest(reqId);

            int txnId = this.txID.getNextTransactionIdentifier();
            relpRequest.setTransactionNumber(txnId);

            this.window.putPending(txnId, reqId);

            sendRelpRequestAsync(relpRequest);
        }
        readAcks(relpBatch);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendBatch> exit");
        }
    }



    private void readAcks(RelpBatch relpBatch)
            throws IOException, TimeoutException, IllegalStateException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.readAcks> entry");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(this.rxBufferSize);

        RelpParser parser = null;

        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_READ);
        boolean notComplete;
        if (this.window.size() > 0) {
            notComplete = true;
        }
        else {
            notComplete = false;
        }

        int readBytes = -1;

        while (notComplete) {
            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.readAcks> need to read");
            }

            int nReady = poll.select(this.readTimeout);
            if (nReady == 0) {
                throw new TimeoutException("read timed out");
            }
            Set<SelectionKey> polledEvents = this.poll.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isReadable()) {
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpConnection.readAcks> became readable");
                    }
                    readBytes = socketChannel.read(byteBuffer);
                }
                eventIter.remove();
            }
            if (readBytes == -1) {
                throw new IOException("read failed");
            }

            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.readAcks> read bytes: " + readBytes);
            }

            // read from it
            byteBuffer.flip();

            // process it
            if (readBytes > 0) {
                while (byteBuffer.hasRemaining()) {
                    if (parser == null) {
                        parser = new RelpParser();
                    }
                    parser.parse(byteBuffer.get());

                    if (parser.isComplete()) {
                        if (System.getenv("RELP_DEBUG") != null) {
                            System.out.println("relpConnection.readAcks> read parser complete: " + parser.isComplete());
                        }
                        // one response read successfully
                        int txnId = parser.getTxnId();
                        if (window.isPending(txnId)) {
                            Long requestId = window.getPending(txnId);
                            RelpFrameRX response = new RelpFrameRX(
                                    parser.getTxnId(),
                                    parser.getCommandString(),
                                    parser.getLength(),
                                    parser.getData()
                            );
                            relpBatch.putResponse(requestId, response);
                            window.removePending(txnId);
                        }
                        // this one is complete, ready for next
                        parser = null;
                        if (window.size() == 0) {
                            notComplete = false;
                            break;
                        }
                    }
                }
            }
            // everything should be read by now
            byteBuffer.compact();
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.readAcks> exit");
        }
    }

    private void sendRelpRequestAsync(RelpFrameTX relpRequest) throws IOException, TimeoutException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendRelpRequestAsync> entry");
        }
        ByteBuffer byteBuffer;
        if (relpRequest.length() > this.txBufferSize) {
            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.sendRelpRequestAsync> allocate new txBuffer of size: "
                        + relpRequest.length());
            }
            byteBuffer = ByteBuffer.allocateDirect(relpRequest.length());
        }
        else {
            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.sendRelpRequestAsync> using preAllocatedTXBuffer for size: "
                + relpRequest.length());
            }
            byteBuffer = this.preAllocatedTXBuffer;
        }
        relpRequest.write(byteBuffer);

        byteBuffer.flip();
        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_WRITE);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendRelpRequestAsync> need to write: " + byteBuffer.hasRemaining());
        }
        while (byteBuffer.hasRemaining()) {
            int nReady = poll.select(this.writeTimeout);
            if (nReady == 0) {
                throw new TimeoutException("write timed out");
            }
            Set<SelectionKey> polledEvents = this.poll.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isWritable()) {
                    if (System.getenv("RELP_DEBUG") != null) {
                        System.out.println("relpConnection.sendRelpRequestAsync> became writable");
                    }
                    this.socketChannel.write(byteBuffer);
                }
                eventIter.remove();
            }
            if (System.getenv("RELP_DEBUG") != null) {
                System.out.println("relpConnection.sendRelpRequestAsync> still need to write: "
                        + byteBuffer.hasRemaining());
            }
        }
        byteBuffer.clear();
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.sendRelpRequestAsync> exit");
        }
    }
    
    /**
     * Creates a new SocketChannel to the RELP server.
     * 
     * @throws IOException
     */
    private void createSocketChannel() throws IOException, TimeoutException {
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.createSocketChannel> entry");
        }
        if (this.poll != null && this.poll.isOpen()) {
            // Invalidate all selection key instances in case they were open
            this.poll.close();
        }

        this.poll = Selector.open();

        this.socketChannel = SocketChannel.open();
        // Make sure our poll will only block
        this.socketChannel.configureBlocking(false);
        // Poll only for connect
        SelectionKey key = this.socketChannel.register(this.poll, SelectionKey.OP_CONNECT);
        // Async connect
        this.socketChannel.connect(new InetSocketAddress(this.hostname, this.port));
        // Poll for connect
        boolean notConnected = true;
        while (notConnected) {
            int nReady = this.poll.select(this.connectionTimeout);
            // Woke up without anything to do
            if (nReady == 0) {
                throw new TimeoutException("connection timed out");
            }
            // It would be possible to skip the whole iterator, but we want to make sure if something else than connect
            // fires then it will be discarded.
            Set<SelectionKey> polledEvents = this.poll.selectedKeys();
            Iterator<SelectionKey> eventIter = polledEvents.iterator();
            while (eventIter.hasNext()) {
                SelectionKey currentKey = eventIter.next();
                if (currentKey.isConnectable()) {
                    if (this.socketChannel.finishConnect()) {
                        // Connection established
                        notConnected = false;
                        if (System.getenv("RELP_DEBUG") != null) {
                            System.out.println("relpConnection> established");
                            try {
                                Thread.sleep(1 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                eventIter.remove();
            }
        }
        // No need to be longer interested in connect.
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
        if (System.getenv("RELP_DEBUG") != null) {
            System.out.println("relpConnection.createSocketChannel> exit");
        }
    }
}
