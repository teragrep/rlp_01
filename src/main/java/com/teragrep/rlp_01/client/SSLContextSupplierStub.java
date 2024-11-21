package com.teragrep.rlp_01.client;

import javax.net.ssl.SSLContext;

public class SSLContextSupplierStub implements SSLContextSupplier {

    @Override
    public SSLContext get() {
        throw new UnsupportedOperationException("stub does not support this.");
    }

    @Override
    public boolean isStub() {
        return true;
    }
}
