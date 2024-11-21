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
