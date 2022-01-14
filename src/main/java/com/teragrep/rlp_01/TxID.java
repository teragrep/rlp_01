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

/**
 * Generates RELP transaction identifiers. Increases monotonically and wraps
 * around at 999 999 999.
 * 
 */
public class TxID {

    private int transactionIdentifier;
    public int MAX_ID = 999999999;

    TxID() {
        this.transactionIdentifier = 1;
    }

    int getNextTransactionIdentifier() {
        if( transactionIdentifier == MAX_ID ) {
            transactionIdentifier = 1;
        }
        return transactionIdentifier++;
    }
}
