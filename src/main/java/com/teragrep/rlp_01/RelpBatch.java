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

import java.util.*;

/**
 * A class that is used to send RELP messages to the server.
 * Note, this class is not thread-safe. External synchronization
 * is required if objects of this class are shared by different
 * threads.
 * 
 */
public class RelpBatch {

    private final requestID reqID;

    private final TreeMap<Long, RelpFrameTX> requests;
    private final TreeMap<Long, RelpFrameRX> responses;

    // Not processed queue, for asynchronous use.
    private final TreeSet<Long> workQueue;

    public RelpBatch() {
        this.reqID = new requestID();
        this.requests = new TreeMap<Long, RelpFrameTX>();
        this.responses = new TreeMap<Long, RelpFrameRX>();
        this.workQueue = new TreeSet<>();
    }

    /**
     Adds a new syslog message to this sending window by converting
     it into a RelpFrameTX.
     Note: this method is not thread-safe, so concurrent threads
     calling this method must externally synchronized access to here.

     @param syslogMessage
     The syslog msg.
     */
    public long insert( byte[] syslogMessage ) {
        RelpFrameTX relpRequest = new RelpFrameTX( syslogMessage );
        return putRequest( relpRequest );
    }

    /**
     Adds a new RELP message frame to this sending window.

     @param request
     The request message.
     @return id
     The requestId of the newly created request.
     */
    public long putRequest( RelpFrameTX request ) {
        long id = this.reqID.getNextID();
        this.requests.put( id, request );
        this.workQueue.add( id );
        return id;
    }

    public RelpFrameTX getRequest(Long id) {
        return this.requests.getOrDefault(id, null);
    }

    public void removeRequest(Long id) {
        this.requests.remove( id );
        this.workQueue.remove( id );
    }

    public RelpFrameRX getResponse(Long id) {
        return this.responses.getOrDefault(id, null);
    }

    public void putResponse(Long id, RelpFrameRX response) {
        if( this.requests.containsKey( id ) ) {
            this.responses.put( id, response );
        }
    }

    public boolean verifyTransaction(Long id) {
        return this.requests.containsKey(id) &&
                this.getResponse(id) != null &&
                this.responses.get(id).getResponseCode() == 200;
    }

    public boolean verifyTransactionAll() {
        Set<Long> reqIds = this.requests.keySet();
        for (Long reqId : reqIds) {
            if (!this.verifyTransaction(reqId)) {
                return false;
            }
        }
        return true;
    }

    public void retryAllFailed() {
        Set<Long> reqIds = this.requests.keySet();
        for (Long reqId : reqIds) {
            if (!this.verifyTransaction(reqId)) {
                this.retryRequest(reqId);
            }
        }
    }

    public void removeTransaction(Long id) {
        this.responses.remove(id);
        this.requests.remove(id);
    }

    // work queue
    public void retryRequest(Long id) {
        if( this.requests.containsKey( id ) ) {
            this.workQueue.add( id );
        }
    }

    public int getWorkQueueLength() {
        return this.workQueue.size();
    }

    public Long popWorkQueue() {
        return this.workQueue.pollFirst();
    }
}
 
