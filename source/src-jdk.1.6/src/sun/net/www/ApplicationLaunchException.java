/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net.www;

/**
 * An exception thrown by the MimeLauncher when it is unable to launch
 * an external content viewer.
 *
 * @version     %I%, %G%
 * @author      Sunita Mani
 */

public class ApplicationLaunchException extends Exception {
    public ApplicationLaunchException(String reason) {
	super(reason);
    }
}
