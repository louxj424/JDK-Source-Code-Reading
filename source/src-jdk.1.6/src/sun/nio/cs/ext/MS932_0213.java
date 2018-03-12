/*
 * %W% %E%
 *
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;

public class MS932_0213 extends Charset {
    public MS932_0213() {
	super("x-MS932_0213", ExtendedCharsets.aliasesFor("MS932_0213"));
    }

    public boolean contains(Charset cs) {
	return ((cs.name().equals("US-ASCII"))
		|| (cs instanceof MS932)
		|| (cs instanceof MS932_0213));
    }

    public CharsetDecoder newDecoder() {
	return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
	return new Encoder(this);
    }

    protected static class Decoder extends SJIS_0213.Decoder {
        MS932DB.Decoder decMS932;
        protected Decoder(Charset cs) {
            super(cs);
	    decMS932 = new MS932DB.Decoder(cs);
        }

        protected char decodeDouble(int b1, int b2) {
	    char c = decMS932.decodeDouble(b1, b2);
	    if (c == DoubleByteDecoder.REPLACE_CHAR)
                return super.decodeDouble(b1, b2);
	    return c;
	}
    }

    protected static class Encoder extends SJIS_0213.Encoder {
        MS932DB.Encoder encMS932;
        protected Encoder(Charset cs) {
            super(cs);
	    encMS932 = new MS932DB.Encoder(cs);
        }

        protected int encodeChar(char ch) {
	    int db = encMS932.encodeDouble(ch);
	    if (db == 0)
	        return super.encodeChar(ch);
	    return db;
	}
    }
}
