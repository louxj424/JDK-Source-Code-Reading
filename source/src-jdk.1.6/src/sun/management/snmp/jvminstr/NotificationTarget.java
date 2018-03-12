/*
 * %Z%file      %M%
 * %Z%author    Sun Microsystems, Inc.
 * %Z%version   %I%
 * %Z%lastedit  %E%
 * 
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.management.snmp.jvminstr;

import java.net.InetAddress;

/**
 * Target notification.
 */
public interface NotificationTarget {
    public InetAddress getAddress();
    public int getPort();
    public String getCommunity();
}
