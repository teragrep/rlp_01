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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
public class RelpWindowTest {
    @Test
    public void testGetPending() {
        RelpWindow window = new RelpWindow();
        window.putPending(12, 1234L);
        Assertions.assertEquals(1234, window.getPending(12), "Got wrong requestId value");
        Assertions.assertEquals(1, window.size(), "Window size is wrong");
    }

    @Test
    public void testIsPending() {
        RelpWindow window = new RelpWindow();
        window.putPending(12, 1234L);
        Assertions.assertTrue(window.isPending(12), "Transaction id is not pending");
    }

    @Test
    public void testRemovePending() {
        RelpWindow window = new RelpWindow();
        window.putPending(12, 1234L);
        window.removePending(12);
        Assertions.assertFalse(window.isPending(1234), "Transaction id is pending");
    }

    @Test
    public void testPendingSize() {
        RelpWindow window = new RelpWindow();
        Assertions.assertEquals(0, window.size(), "Unexpected amount of events in window");
        int messages = 5;
        for(int i=1; i<=messages; i++) {
            window.putPending(i, (long) i);
        }
        Assertions.assertEquals(messages, window.size(), "Unexpected amount of events in window");
    }
}
