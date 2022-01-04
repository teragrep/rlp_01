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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

/**
 * A class that is used to send RELP messages to the server.
 * Note, this class is not thread-safe. External synchronization
 * is required if objects of this class are shared by different
 * threads.
 * 
 */
public class RelpBatch {

    // requestId for user of the window
    private requestID reqID;

    private TreeMap<Long, RelpFrameTX> requests;
    private TreeMap<Long, RelpFrameRX> responses;

    // not processed queue, for asynchronous use
    private LinkedList<Long> workQueue;

    public RelpBatch() {
        this.reqID = new requestID();
        this.requests = new TreeMap<Long, RelpFrameTX>();
        this.responses = new TreeMap<Long, RelpFrameRX>();
        this.workQueue = new LinkedList<Long>();
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
        if (this.requests.containsKey(id)) {
            return this.requests.get(id);
        }
        else {
            return null;
        }
    }

    public void removeRequest(Long id) {
        this.requests.remove( id );
        this.workQueue.remove( id );
    }

    public RelpFrameRX getResponse(Long id) {
        if (this.responses.containsKey(id)) {
            return this.responses.get(id);
        }
        else {
            return null;
        }
    }

    public void putResponse(Long id, RelpFrameRX response) {
        if( this.requests.containsKey( id ) ) {
            this.responses.put( id, response );
        }
    }

    public boolean verifyTransaction(Long id) {
        if( this.requests.containsKey( id ) &&
                this.getResponse( id ) != null &&
                this.responses.get( id ).getResponseCode() == 200 ) {
            return true;
        } else {
            return false;
        }
    }

    public boolean verifyTransactionAll() {
        Set<Long> reqIds = this.requests.keySet();
        Iterator<Long> reqIt = reqIds.iterator();
        while(reqIt.hasNext()) {
            if (this.verifyTransaction(reqIt.next()) == false) {
                return false;
            }
        }
        return true;
    }

    public void retryAllFailed() {
        Set<Long> reqIds = this.requests.keySet();
        Iterator<Long> reqIt = reqIds.iterator();
        while(reqIt.hasNext()) {
            Long reqId = reqIt.next();
            if (this.verifyTransaction(reqId) == false) {
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
        return this.workQueue.pop();
    }

    public boolean isPending(Long id) {
        return this.workQueue.contains(id) && this.requests.containsKey(id);
    }
}
 
