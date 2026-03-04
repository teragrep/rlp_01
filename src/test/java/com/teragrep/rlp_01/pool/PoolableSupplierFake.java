package com.teragrep.rlp_01.pool;

import com.teragrep.poj_01.pool.PoolableSupplier;

import java.util.function.Consumer;
import java.util.function.Function;

public class PoolableSupplierFake<PoolRef, PoolableType> implements PoolableSupplier<PoolRef, PoolableType> {
    private final Consumer<PoolableType> consumer;
    private final Function<PoolRef, PoolableType> function;

    public PoolableSupplierFake(final Consumer<PoolableType> consumer, final Function<PoolRef, PoolableType> function) {
        this.consumer = consumer;
        this.function = function;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void accept(final PoolableType poolable) {
        consumer.accept(poolable);
    }

    @Override
    public PoolableType apply(final PoolRef pool) {
        return function.apply(pool);
    }
}
