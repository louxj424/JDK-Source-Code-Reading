/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.datatransfer;

public interface ToolkitThreadBlockedHandler {
    public void lock();
    public void unlock();
    public void enter();
    public void exit();
}
