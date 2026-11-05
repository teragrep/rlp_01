/*
* Teragrep Reliable Event Logging Protocol (RELP) Library for Java
* Copyright (C) 2021-2026 Suomen Kanuuna Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.teragrep.rlp_01.client;

import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.client.exceptions.RelpException;
import com.teragrep.stb_01.Stubable;

import java.io.Closeable;

public interface IManagedRelpConnection extends Stubable, Closeable {

    void reconnect() throws RelpException;

    long connect() throws RelpException;

    void forceReconnect() throws RelpException;

    long ensureSent(byte[] bytes) throws RelpException;

    long ensureSent(RelpBatch relpBatch) throws RelpException;
}
