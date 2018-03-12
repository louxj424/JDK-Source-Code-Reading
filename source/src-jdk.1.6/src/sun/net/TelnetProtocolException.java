/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net;

import java.io.*;

/**
 * An unexpected result was received by the client when talking to the
 * telnet server.
 * 
 * @version     %I%,%G%
 * @author      Jonathan Payne 
 */

public class TelnetProtocolException extends IOException {
    public TelnetProtocolException(String s) {
	super(s);
    }
}
