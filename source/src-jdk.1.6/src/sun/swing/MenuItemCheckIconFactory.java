/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.swing;

import javax.swing.Icon;
import javax.swing.JMenuItem;

/**
 *
 * @version %I% %G%
 * @author Igor Kushnirskiy
 */

public interface MenuItemCheckIconFactory {
    Icon getIcon(JMenuItem component);
    boolean isCompatible(Object icon, String prefix);
}
