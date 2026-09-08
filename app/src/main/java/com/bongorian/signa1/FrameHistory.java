package com.bongorian.signa1;

import java.util.*;
import java.util.function.Supplier;

/** Bounded handoff from GL submission to UI acknowledgement and shutter reservation.
 * Payloads are writable only while acquired by GL. A shutter pins the acknowledged payload
 * synchronously BEFORE posting work to GL, so queued camera callbacks cannot overwrite it. */
final class FrameHistory<T> {
    static final class Slot<T> {final T value;private long timestamp;private int readers;private boolean writing;Slot(T value){this.value=value;}}
    static final class Lease<T> {final T value;final long timestamp;private final Slot<T> slot;private final long epoch;private boolean released;Lease(Slot<T> slot,long epoch){this.slot=slot;value=slot.value;timestamp=slot.timestamp;this.epoch=epoch;}}
    private final List<Slot<T>> slots=new ArrayList<>();private long acknowledged,epoch;
    FrameHistory(int capacity,Supplier<T> factory){if(capacity<2)throw new IllegalArgumentException("Frame capacity");for(int i=0;i<capacity;i++)slots.add(new Slot<>(factory.get()));}
    synchronized Slot<T> acquire(){for(Slot<T> slot:slots)if(!slot.writing&&slot.readers==0&&(slot.timestamp==0||slot.timestamp<acknowledged)){slot.writing=true;slot.timestamp=0;return slot;}return null;}
    synchronized void publish(Slot<T> slot,long timestamp){if(!slot.writing||timestamp<=0)throw new IllegalStateException("Frame publication");slot.timestamp=timestamp;slot.writing=false;}
    synchronized void abandon(Slot<T> slot){slot.writing=false;slot.timestamp=0;}
    synchronized boolean acknowledge(long timestamp){if(timestamp<=0)return false;for(Slot<T> slot:slots)if(!slot.writing&&slot.timestamp==timestamp&&timestamp>=acknowledged){acknowledged=timestamp;return true;}return false;}
    synchronized Lease<T> reserve(){return reserve(acknowledged);}
    synchronized Lease<T> reserve(long timestamp){if(timestamp<=0)return null;for(Slot<T> slot:slots)if(!slot.writing&&slot.timestamp==timestamp){slot.readers++;return new Lease<>(slot,epoch);}return null;}
    synchronized boolean valid(Lease<T> lease){return lease!=null&&!lease.released&&lease.epoch==epoch&&lease.slot.timestamp==lease.timestamp;}
    synchronized void release(Lease<T> lease){if(lease!=null&&!lease.released){lease.released=true;lease.slot.readers--;}}
    synchronized long acknowledged(){return acknowledged;}
    synchronized void clear(){epoch++;acknowledged=0;for(Slot<T> slot:slots){slot.timestamp=0;slot.writing=false;}}
    List<T> values(){List<T> values=new ArrayList<>();for(Slot<T> slot:slots)values.add(slot.value);return Collections.unmodifiableList(values);}
}
