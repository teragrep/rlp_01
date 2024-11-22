/*
   Java Reliable Event Logging Protocol Library RLP-01
   Copyright (C) 2021-2024  Suomen Kanuuna Oy

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
package com.teragrep.rlp_01.client;

import java.time.Duration;

public class RelpConfig {
    public final String relpTarget;
    public final int relpPort;
    public final int relpReconnectInterval;
    public final int rebindRequestAmount;
    public final boolean rebindEnabled;
    public final Duration maxIdle;
    public final boolean maxIdleEnabled;

    public RelpConfig(String relpTarget, int relpPort, int relpReconnectInterval, int rebindRequestAmount, boolean rebindEnabled, Duration maxIdle, boolean maxIdleEnabled) {
        this.relpTarget = relpTarget;
        this.relpPort = relpPort;
        this.relpReconnectInterval = relpReconnectInterval;
        this.rebindRequestAmount = rebindRequestAmount;
        this.rebindEnabled = rebindEnabled;
        this.maxIdle = maxIdle;
        this.maxIdleEnabled = maxIdleEnabled;
    }
}
