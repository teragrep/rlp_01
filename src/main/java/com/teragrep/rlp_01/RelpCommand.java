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
 Simple class to contain all the known command types.
 */
// TODO change to ENUM
public class RelpCommand {
    public final static String OPEN            = "open";
    public final static String CLOSE           = "close";
    public final static String ABORT           = "abort";
    public final static String SERVER_CLOSE    = "serverclose";
    public final static String SYSLOG          = "syslog";
    public final static String RESPONSE        = "rsp";
}
