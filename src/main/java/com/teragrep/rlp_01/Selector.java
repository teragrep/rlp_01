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

import com.teragrep.stb_01.Stubable;

import java.io.IOException;

/**
 * A Selector that can be instantiated before it is opened, since Selector objects are instantiated using static method
 * Selector.open() Responsibilities: - Provides access to a {@link java.nio.channels.Selector} object - Reports whether
 * it is a Stub or not
 */
public interface Selector extends Stubable {

    java.nio.channels.Selector get() throws IOException;
}
