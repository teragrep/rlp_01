package com.teragrep.rlp_01.client;

import com.teragrep.rlp_01.pool.Stubable;

import javax.net.ssl.SSLContext;
import java.util.function.Supplier;

/**
 * Wrapper for SSLContext because such do not have stubness property
 */
public interface SSLContextSupplier extends Supplier<SSLContext>, Stubable {
    SSLContext get();
}
