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
package com.teragrep.rlp_01;

import java.nio.channels.Selector;

public class SelectorableImpl implements Selectorable {

    private final java.nio.channels.Selector selector;
    private final boolean isStub;

    public SelectorableImpl(java.nio.channels.Selector selector) {
        this.selector = selector;
        this.isStub = false;
    }

    @Override
    public Selector selector() {
        return selector;
    }

    @Override
    public boolean isStub() {
        return isStub;
    }
}
