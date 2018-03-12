/*
 * %W% %E%
 * 
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.management.counter.perf;

import sun.management.counter.*;

/**
 * A snapshot of the perf counter for serialization.
 */
class StringCounterSnapshot extends AbstractCounter
       implements StringCounter {

    String value;

    // package private
    StringCounterSnapshot(String name, Units u, Variability v, int flags, 
                          String value) {
        super(name, u, v, flags);
        this.value = value;
    }
    
    public Object getValue() {
        return value;
    }
    
    public String stringValue() {
        return value;
    }
}
