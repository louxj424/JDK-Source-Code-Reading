/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.tools.jstat;

import java.util.*;
import sun.jvmstat.monitor.*;

/**
 * Class to compare two Monitor objects by name in ascending order.
 *
 * @author Brian Doherty
 * @version %I%, %G%
 * @since 1.5
 */
class AscendingMonitorComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        String name1 = ((Monitor)o1).getName();
        String name2 = ((Monitor)o2).getName();
        return name1.compareTo(name2);
    }
}
