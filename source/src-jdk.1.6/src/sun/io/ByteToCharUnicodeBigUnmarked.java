/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.io;
import java.io.*;


/**
 * Convert byte arrays containing Unicode characters into arrays of actual
 * Unicode characters, assuming a big-endian byte order and requiring no
 * byte-order mark.
 *
 * @version 	%I%, %E%
 * @author	Mark Reinhold
 */

public class ByteToCharUnicodeBigUnmarked extends ByteToCharUnicode {

    public ByteToCharUnicodeBigUnmarked() {
	super(BIG, false);
    }

}
