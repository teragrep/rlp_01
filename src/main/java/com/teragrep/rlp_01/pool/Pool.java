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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class Pool<T extends Poolable> implements AutoCloseable, Supplier<T> {

    private final Supplier<T> supplier;

    private final ConcurrentLinkedQueue<T> queue;

    private final T stub;

    private final Lock lock = new ReentrantLock();

    private final AtomicBoolean close;

    public Pool(final Supplier<T> supplier, T stub) {
        this.supplier = supplier;
        this.queue = new ConcurrentLinkedQueue<>();
        this.stub = stub;
        this.close = new AtomicBoolean();
    }

    public T get() {
        T object;
        if (close.get()) {
            object = stub;
        }
        else {
            // get or create
            object = queue.poll();
            if (object == null) {
                object = supplier.get();
            }
        }

        return object;
    }

    public void offer(T object) {
        if (!object.isStub()) {
            queue.add(object);
        }

        if (close.get()) {
            while (queue.peek() != null) {
                if (lock.tryLock()) {
                    while (true) {
                        T pooled = queue.poll();
                        if (pooled == null) {
                            break;
                        }
                        else {
                            try {
                                pooled.close();
                            }
                            catch (Exception exception) {
                                System.err.println("Exception <" + exception.getMessage() + "> while closing poolable <"+ pooled +">");
                            }
                        }
                    }
                    lock.unlock();
                }
                else {
                    break;
                }
            }
        }
    }

    public void close() {
        close.set(true);

        // close all that are in the pool right now
        offer(stub);
    }
}
