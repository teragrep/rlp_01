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

import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * Selector is designed to be initialized via static method Selector.open(), creating an opened Selector. This subclass
 * can be used to represent a Selector that has not yet been opened.
 */
public class UnopenedSelector extends Selector {

    public UnopenedSelector() {
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public SelectorProvider provider() {
        return SelectorProvider.provider();
    }

    @Override
    public Set<SelectionKey> keys() {
        throw new ClosedSelectorException();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        throw new ClosedSelectorException();
    }

    @Override
    public int selectNow() throws IOException {
        throw new ClosedSelectorException();
    }

    @Override
    public int select(final long l) throws IOException {
        throw new ClosedSelectorException();
    }

    @Override
    public int select() throws IOException {
        throw new ClosedSelectorException();
    }

    @Override
    public Selector wakeup() {
        throw new ClosedSelectorException();
    }

    @Override
    public void close() throws IOException {
        // no-op.
    }
}
