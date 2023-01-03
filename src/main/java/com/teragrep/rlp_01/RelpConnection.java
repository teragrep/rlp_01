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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Abstract the concept of RELP session: it handles the
 * handshake with the RELP server, sends RELP messages
 * and receives replies.
 * 
 */
public class RelpConnection implements RelpSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpConnection.class);


    private int rxBufferSize;
    private int txBufferSize;
    private ByteBuffer preAllocatedTXBuffer;
    private ByteBuffer preAllocatedRXBuffer;
    private static final int MAX_COMMAND_LENGTH  = 11;
    private final RelpClientSocket relpClientSocket;

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


    public int getReadTimeout() {
        return relpClientSocket.getReadTimeout();
    }

    public void setReadTimeout(int readTimeout) {
        relpClientSocket.setReadTimeout(readTimeout);
    }

    public int getWriteTimeout() {
        return relpClientSocket.getWriteTimeout();
    }

    public void setWriteTimeout(int writeTimeout) {
        relpClientSocket.setWriteTimeout(writeTimeout);
    }

	public int getConnectionTimeout() {
		return relpClientSocket.getConnectionTimeout();
	}

	public void setConnectionTimeout(int timeout) {
		relpClientSocket.setConnectionTimeout(timeout);
	}

	public int getRxBufferSize() {
	    return this.rxBufferSize;
    }

    public void setRxBufferSize(int size) {
        if (this.state != RelpConnectionState.CLOSED) {
            throw new IllegalStateException("Connection must be closed to " +
                    "change rxBufferSize");
        }
        this.preAllocatedRXBuffer = ByteBuffer.allocateDirect(size);
	    this.rxBufferSize = size;
    }

    public int getTxBufferSize() {
	    return this.txBufferSize;
    }

    public void setTxBufferSize(int size) {
        if (this.state != RelpConnectionState.CLOSED) {
            throw new IllegalStateException("Connection must be closed to " +
                    "change txBufferSize");
        }
        this.preAllocatedTXBuffer = ByteBuffer.allocateDirect(size);
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
        this.state = RelpConnectionState.CLOSED;

        this.setRxBufferSize(512);
        this.setTxBufferSize(262144);

        this.relpClientSocket = new RelpClientPlainSocket();
    }

    public RelpConnection(Supplier<SSLEngine> sslEngineSupplier) {
        this.state = RelpConnectionState.CLOSED;

        this.setRxBufferSize(512);
        this.setTxBufferSize(262144);

        this.relpClientSocket = new RelpClientTlsSocket(sslEngineSupplier);
    }

    
    /**
     * Creates a new RELP session with given server details and connects into it and
     * does the "open session" command.
     * 
     * @throws IOException
     */
    public boolean connect(String hostname, int port) throws IOException, IllegalStateException, TimeoutException {
        LOGGER.trace("relpConnection.connect> entry");
        if (state != RelpConnectionState.CLOSED) {
            throw new IllegalStateException("Session is not closed.");
        }

        //
        this.txID = new TxID();
        this.window = new RelpWindow();

        this.relpClientSocket.open(hostname, port);

        // send open session message
        RelpFrameTX relpRequest = new RelpFrameTX(RelpCommand.OPEN, OFFER);
        RelpBatch connectionOpenBatch = new RelpBatch();
        long reqId = connectionOpenBatch.putRequest(relpRequest);
        this.sendBatch(connectionOpenBatch);
        boolean openSuccess = connectionOpenBatch.verifyTransaction(reqId);
        LOGGER.trace("relpConnection.connect> exit with: " + openSuccess);
        if (openSuccess) {
            this.state = RelpConnectionState.OPEN;
        }
        return openSuccess;
    }

    public void tearDown()  {
        try {
            relpClientSocket.close();
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
        LOGGER.trace("relpConnection.disconnect> entry");
        if (state != RelpConnectionState.OPEN) {
            throw new IllegalStateException("Session is not in open state, can not close.");
        }
        RelpFrameTX relpRequest = new RelpFrameTX(RelpCommand.CLOSE);
        RelpBatch connectionCloseBatch = new RelpBatch();
        long reqId = connectionCloseBatch.putRequest(relpRequest);
        this.sendBatch(connectionCloseBatch);
        boolean closeSuccess = false;
        RelpFrameRX closeResponse = connectionCloseBatch.getResponse(reqId);
        if (closeResponse != null && closeResponse.dataLength == 0) {
            closeSuccess = true;
        }
        LOGGER.trace("relpConnection.disconnect> exit with: " + closeSuccess);
        if(closeSuccess){
            relpClientSocket.close();
            this.state = RelpConnectionState.CLOSED;
        }
        return closeSuccess;
    }

    public void commit(RelpBatch relpBatch) throws IOException, IllegalStateException, TimeoutException {
        LOGGER.trace("relpConnection.commit> entry");
        if (this.state != RelpConnectionState.OPEN) {
            throw new IllegalStateException("Session is not in open state, can not commit.");
        }
        this.state = RelpConnectionState.COMMIT;
        this.sendBatch(relpBatch);
        this.state = RelpConnectionState.OPEN;
        LOGGER.trace("relpConnection.commit> exit");
    }

    /**
     Processes all the jobs in the workQueue of the given batch by iterating
     through each requestId, retrieving the request frame associated with the id,
     setting a linearly incremented txID and sending the request to server. Finally
     calls readAcks to make sure the requests went through and received a response.

     */
    private void sendBatch(RelpBatch relpBatch)  throws IOException, TimeoutException, IllegalStateException {
        LOGGER.trace("relpConnection.sendBatch> entry with wq len " + relpBatch.getWorkQueueLength());
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
        LOGGER.trace("relpConnection.sendBatch> exit");
    }



    private void readAcks(RelpBatch relpBatch)
            throws IOException, TimeoutException, IllegalStateException {
        LOGGER.trace("relpConnection.readAcks> entry");

        RelpParser parser = null;

        int readBytes;

        boolean notComplete = this.window.size() > 0;

        while (notComplete) {
            LOGGER.trace("relpConnection.readAcks> need to read");

            readBytes = relpClientSocket.read(preAllocatedRXBuffer);

            LOGGER.trace("relpConnection.readAcks> read bytes: " + readBytes);

            // read from it
            preAllocatedRXBuffer.flip();

            // process it
            if (readBytes > 0) {
                while (preAllocatedRXBuffer.hasRemaining()) {
                    if (parser == null) {
                        parser = new RelpParser();
                    }
                    parser.parse(preAllocatedRXBuffer.get());

                    if (parser.isComplete()) {
                        LOGGER.trace("relpConnection.readAcks> read parser " +
                                "complete: " + parser.isComplete());
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
            preAllocatedRXBuffer.compact();
        }
        LOGGER.trace("relpConnection.readAcks> exit");
    }

    private void sendRelpRequestAsync(RelpFrameTX relpRequest) throws IOException, TimeoutException {
        LOGGER.trace("relpConnection.sendRelpRequestAsync> entry");
        ByteBuffer byteBuffer;
        if (relpRequest.length() > this.txBufferSize) {
            LOGGER.trace("relpConnection.sendRelpRequestAsync> allocate new " +
                    "txBuffer of size: "
                    + relpRequest.length());
            byteBuffer = ByteBuffer.allocateDirect(relpRequest.length());
        }
        else {
            LOGGER.trace("relpConnection.sendRelpRequestAsync> using " +
                    "preAllocatedTXBuffer for size: "
            + relpRequest.length());
            byteBuffer = this.preAllocatedTXBuffer;
        }
        relpRequest.write(byteBuffer);

        byteBuffer.flip();
        relpClientSocket.write(byteBuffer);
        byteBuffer.clear();
        LOGGER.trace("relpConnection.sendRelpRequestAsync> exit");
    }
}
