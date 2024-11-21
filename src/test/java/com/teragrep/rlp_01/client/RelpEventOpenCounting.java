package com.teragrep.rlp_01.client;

import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.event.RelpEvent;
import com.teragrep.rlp_03.frame.delegate.event.RelpEventOpen;

import java.util.concurrent.atomic.AtomicLong;

class RelpEventOpenCounting extends RelpEvent {
    private final AtomicLong openCount;
    private final RelpEventOpen eventOpen;

    RelpEventOpenCounting(AtomicLong openCount) {
        this.openCount = openCount;
        this.eventOpen = new RelpEventOpen();
    }

    @Override
    public void accept(FrameContext frameContext) {
        eventOpen.accept(frameContext);
        openCount.incrementAndGet();
    }
}
