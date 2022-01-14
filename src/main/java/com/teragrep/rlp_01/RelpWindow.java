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

import java.util.TreeMap;

public class RelpWindow {
    // Mapping between connection's txnId and window's requestId
    private TreeMap<Integer, Long> pending;

    public RelpWindow() {
        this.pending = new TreeMap<Integer, Long>();
    }

    // for connection
    public void putPending(Integer txnId, Long requestId) {
        this.pending.put(txnId, requestId);
    }

    public boolean isPending(Integer txnId) {
        return this.pending.containsKey(txnId);
    }

    public Long getPending(Integer txnId) {
        return this.pending.get(txnId);
    }

    public void removePending(Integer txnId) { this.pending.remove(txnId); }

    public int size() {
        return this.pending.size();
    }
}
