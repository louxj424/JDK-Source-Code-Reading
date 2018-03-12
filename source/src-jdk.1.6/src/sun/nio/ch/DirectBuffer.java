/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.nio.ch;

import sun.misc.Cleaner;


public interface DirectBuffer {

    public long address();

    public Object viewedBuffer();

    public Cleaner cleaner();

}
