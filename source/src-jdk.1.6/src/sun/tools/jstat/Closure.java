/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.tools.jstat;

import java.util.List;
import sun.jvmstat.monitor.MonitorException;

/**
 * An interface for visitor object on a binary tree.
 *
 * @author Brian Doherty
 * @version %I%, %G%
 * @since 1.5
 */
interface Closure {
    void visit(Object o, boolean hasNext) throws MonitorException;
}
