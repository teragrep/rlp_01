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
package com.teragrep.rlp_01.pool;

import com.teragrep.poj_01.pool.PoolableSupplier;

import java.util.function.Consumer;
import java.util.function.Function;

public final class PoolableSupplierFake<PoolRef, PoolableType> implements PoolableSupplier<PoolRef, PoolableType> {
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
