package com.teragrep.rlp_01;

import com.sun.net.ssl.internal.ssl.Provider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SSLContextFactory {

    public static SSLContext authenticatedContext(
            String keystorePath,
            String keystorePassword,
            String protocol
    ) throws GeneralSecurityException, IOException {

        SSLContext sslContext = SSLContext.getInstance(protocol);
        KeyStore ks = KeyStore.getInstance("JKS");

        File file = new File(keystorePath);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ks.load(fileInputStream, keystorePassword.toCharArray());
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keystorePassword.toCharArray());
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext;
        }
    }
}