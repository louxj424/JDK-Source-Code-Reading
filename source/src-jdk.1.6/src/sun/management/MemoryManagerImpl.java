/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.management;

import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;

/**
 * Implementation class for a memory manager.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getMemoryManagerMXBeans() returns a list
 * of instances of this class.
 */
class MemoryManagerImpl implements MemoryManagerMXBean {

    private final String  name;
    private final boolean isValid;
    private MemoryPoolMXBean[] pools;

    MemoryManagerImpl(String name) {
        this.name = name;
        this.isValid = true;
        this.pools = null;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return isValid;
    }

    public String[] getMemoryPoolNames() {
        MemoryPoolMXBean[] ps = getMemoryPools();

        String[] names = new String[ps.length];
        for (int i = 0; i < ps.length; i++) {
            names[i] = ps[i].getName();
        }
        return names;
    }

    synchronized MemoryPoolMXBean[] getMemoryPools() {
        if (pools == null) {
            pools = getMemoryPools0();
        }
        return pools;
    }
    private native MemoryPoolMXBean[] getMemoryPools0();

}

