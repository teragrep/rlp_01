package com.teragrep.rlp_01.client;

import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.event.RelpEvent;
import com.teragrep.rlp_03.frame.delegate.event.RelpEventClose;

import java.util.concurrent.atomic.AtomicLong;

class RelpEventCloseCounting extends RelpEvent {
    private final AtomicLong closeCount;
    private final RelpEventClose relpEventClose;

    RelpEventCloseCounting(AtomicLong closeCount) {
        this.closeCount = closeCount;
        this.relpEventClose = new RelpEventClose();
    }

    @Override
    public void accept(FrameContext frameContext) {
        relpEventClose.accept(frameContext);
        closeCount.incrementAndGet();
    }
}
