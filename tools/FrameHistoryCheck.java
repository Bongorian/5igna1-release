package com.bongorian.signa1;

/** Independent schedule fixtures for the exact displayed-frame capture contract. */
public final class FrameHistoryCheck {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        FrameHistory<int[]> history=new FrameHistory<>(3,()->new int[1]);
        FrameHistory.Slot<int[]> first=history.acquire();first.value[0]=10;history.publish(first,100);
        check(history.reserve()==null,"submission is not display acknowledgement");
        check(history.acknowledge(100),"first displayed frame");
        FrameHistory.Lease<int[]> shutter=history.reserve(100);
        FrameHistory.Slot<int[]> second=history.acquire();second.value[0]=20;history.publish(second,200);history.acknowledge(200);
        FrameHistory.Slot<int[]> third=history.acquire();third.value[0]=30;history.publish(third,300);
        check(history.acquire()==null,"bounded backpressure while UI and shutter retain images");
        check(shutter.value[0]==10&&shutter.timestamp==100,"queued camera frames cannot replace shutter image");
        check(!history.acknowledge(99),"stale UI acknowledgement rejected");
        check(history.valid(shutter),"old displayed frame is still pinned");history.release(shutter);
        check(history.acquire()==first,"released older buffer is reusable");history.abandon(first);
        FrameHistory.Lease<int[]> last=history.reserve(200);history.clear();check(!history.valid(last)&&history.reserve()==null,"camera lifecycle invalidates captures");history.release(last);
        check(history.acquire()!=null,"camera restart recovers buffers");
        System.out.println("PASS UI-acknowledged frames, synchronous shutter pin, bounded backpressure and lifecycle invalidation");
    }
}
