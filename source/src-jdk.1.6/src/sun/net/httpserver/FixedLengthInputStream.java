/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net.httpserver;

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.spi.*;

/**
 * a class which allows the caller to read up to a defined
 * number of bytes off an underlying stream
 * close() does not close the underlying stream
 */

class FixedLengthInputStream extends LeftOverInputStream {
    private int remaining;

    FixedLengthInputStream (ExchangeImpl t, InputStream src, int len) {
	super (t, src);
	this.remaining = len;
    }

    protected int readImpl (byte[]b, int off, int len) throws IOException {
	eof = (remaining == 0);
	if (eof) {
	    return -1;
	}
	if (len > remaining) {
	    len = remaining;
	}
	int n = in.read(b, off, len);
	if (n > -1) {
	    remaining -= n;
	}
	return n;
    }

    public int available () throws IOException {
	if (eof) {
	    return 0;
	}
	int n = in.available();
	return n < remaining? n: remaining;
    }

    public boolean markSupported () {return false;}

    public void mark (int l) {
    }	

    public void reset () throws IOException {
	throw new IOException ("mark/reset not supported");
    }	
}
