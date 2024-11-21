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

import com.teragrep.rlp_01.SSLContextFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Wrapper for existing SSLContextFactory for providing stubness
 */
public class SSLContextSupplierKeystore implements SSLContextSupplier {
    private final String keystorePath;
    private final String keystorePassword;
    private final String protocol;

    public SSLContextSupplierKeystore(String keystorePath, String keystorePassword, String protocol) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.protocol = protocol;
    }

    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public SSLContext get() {
        try {
            // TODO refactor static method authenticatedContext to non-static, this is just for version compatibility
            return SSLContextFactory.authenticatedContext(keystorePath, keystorePassword, protocol);
        }
        catch (GeneralSecurityException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
