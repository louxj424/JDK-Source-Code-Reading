/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net.httpserver;

/**
 * A Http error
 */
class HttpError extends RuntimeException {
    public HttpError (String msg) {
	super (msg);
    }
}
