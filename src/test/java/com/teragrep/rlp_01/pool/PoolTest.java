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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

public class PoolTest {

    @Test
    public void testUnboundPool() {
        AtomicLong report = new AtomicLong();

        Pool<TestPoolable> pool = new UnboundPool<>(() -> new TestPoolableImpl(report), new TestPoolableStub());

        final int testCycles = 1_000_000;
        CountDownLatch countDownLatch = new CountDownLatch(testCycles);

        for (int i = 0; i < testCycles; i++) {
            ForkJoinPool.commonPool().submit(() -> {
                TestPoolable testPoolable = pool.get();
                testPoolable.increment();
                pool.offer(testPoolable);
                countDownLatch.countDown();
            });
        }

        Assertions.assertAll(countDownLatch::await);

        pool.close();

        Assertions.assertEquals(testCycles, report.get());
    }

    private interface TestPoolable extends Poolable {
        void increment();
    }

    private static class TestPoolableImpl implements TestPoolable {

        private final AtomicLong report;
        private final List<Integer> counterList;

        TestPoolableImpl(AtomicLong report) {
            this.report = report;
            this.counterList = new ArrayList<>(1);
            int counter = 0;
            this.counterList.add(counter);
        }

        @Override
        public void increment() {
            // unsynchronized list access here to test concurrent modification
            int counter = counterList.remove(0);
            counter = counter + 1;
            counterList.add(counter);
        }

        @Override
        public boolean isStub() {
            return false;
        }

        @Override
        public void close() throws IOException {
            int counter = counterList.get(0);
            report.addAndGet(counter);
        }
    }

    private static class TestPoolableStub implements TestPoolable {

        @Override
        public boolean isStub() {
            return true;
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("stub does not support this");
        }

        @Override
        public void increment() {
            throw new UnsupportedOperationException("stub does not support this");
        }
    }
}
