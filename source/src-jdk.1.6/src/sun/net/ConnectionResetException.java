/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net;

import java.net.SocketException;

/**
 * Thrown to indicate a connection reset.
 *
 * @since   1.4
 */
public
class ConnectionResetException extends SocketException {

    public ConnectionResetException(String msg) {
        super(msg);
    }

    public ConnectionResetException() {
    }
}

