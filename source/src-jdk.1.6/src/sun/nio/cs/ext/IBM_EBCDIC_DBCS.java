/*
 * %W% %E%
 *
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.nio.cs.ext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;
import static sun.nio.cs.CharsetMapping.*;

/**
 * The abstract base class for subclasses which decode IBM
 * double byte ebcdic host encodings such as ibm codepage
 * 1364... etc. 
 */

class IBM_EBCDIC_DBCS
{
    abstract static class Decoder extends CharsetDecoder {

        private static final int SBCS = 0;
        private static final int DBCS = 1;
        private static final int SO = 0x0e;
        private static final int SI = 0x0f;
        private int  currentState;

        Decoder(Charset cs) {
            super(cs, 0.5f, 1.0f);
        }
    
        protected void implReset() {
            currentState = SBCS;
        }

        // Check validity of dbcs ebcdic byte pair values
        private static boolean isDoubleByte(int b1, int b2) {
            return (b1 == 0x40 && b2 == 0x40) || // DBCS-HOST SPACE
                   (0x41 <= b1 && b1 <= 0xfe && 0x41 <= b2 && b2 <= 0xfe);
        }

        protected CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();
    
            try {
                while (sp < sl) {
                    int b1 = sa[sp] & 0xff;
                    int inSize = 1;
                    if (b1 == SO) {  // Shift out
                        if (currentState != SBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = DBCS;
                    } else if (b1 == SI) {
                        if (currentState != DBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = SBCS;
                    } else {
                        char c =  UNMAPPABLE_DECODING;
                        if (currentState == SBCS) {
                            c = decodeSingle(b1);
                        } else {
                            if (sl - sp < 2)
                                return CoderResult.UNDERFLOW;
                            int b2 = sa[sp + 1] & 0xff;
                            if (!isDoubleByte(b1, b2))
                                return CoderResult.malformedForLength(2);
                            inSize++;
                            c = decodeDouble(b1, b2);
                        }
                        if (c == UNMAPPABLE_DECODING)
                            return CoderResult.unmappableForLength(inSize);
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = c;
                    }
                    sp += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }
    
        protected CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    int b1 = src.get() & 0xff;
                    int inSize = 1;
                    if (b1 == SO) {  // Shift out
                        if (currentState != SBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = DBCS;
                    } else if (b1 == SI) {
                        if (currentState != DBCS)
                            return CoderResult.malformedForLength(1);
                        else
                            currentState = SBCS;
                    } else {
                        char c = UNMAPPABLE_DECODING;
                        if (currentState == SBCS) {
                            c = decodeSingle(b1);
                        } else {
                            if (src.remaining() < 1)
                                return CoderResult.UNDERFLOW;
                            int b2 = src.get()&0xff;
                            if (!isDoubleByte(b1, b2))
                                return CoderResult.malformedForLength(2);
                            inSize++;
                            c = decodeDouble(b1, b2);
                        }
                        if (c == UNMAPPABLE_DECODING)
                            return CoderResult.unmappableForLength(inSize);
                        if (!dst.hasRemaining())
                            return CoderResult.OVERFLOW;
                        dst.put(c);
                    }
                    mark += inSize;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        abstract char decodeSingle(int b);
        abstract char decodeDouble(int b1, int b2);
    }

    abstract static class Encoder extends CharsetEncoder
    {
        protected static final int MAX_SINGLEBYTE = 0xff;
        protected int  currentState;

        static final int SBCS = 0;
        static final int DBCS = 1;
        static final byte SO = 0x0e;
        static final byte SI = 0x0f;
    
        private final Surrogate.Parser sgp = new Surrogate.Parser();
    
        Encoder(Charset cs) {
            this(cs, 4.0f, 5.0f, new byte[] {(byte)0x6f});
        }

        Encoder(Charset cs, float avg, float max, byte[] repl) {
            super(cs, avg, max, repl);
        }
    
        protected void implReset() {
            currentState = SBCS;
        }
    
        protected CoderResult implFlush(ByteBuffer out) {
            if (currentState == DBCS) {
                if (out.remaining() < 1)
                    return CoderResult.OVERFLOW;
                out.put(SI);
            }
            implReset();
            return CoderResult.UNDERFLOW;
        }
    
        protected CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();
    
            try {
                while (sp < sl) {
                    char c = sa[sp];
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Surrogate.is(c)) {
                            if (sgp.parse(c, sa, sp, sl) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
                    }
                    if (bb > MAX_SINGLEBYTE) {  // DoubleByte
                        if (currentState == SBCS) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            currentState = DBCS;
                            da[dp++] = SO;
                        }
                        if (dl - dp < 2)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)(bb >> 8);
                        da[dp++] = (byte)bb;
                    } else {                    // SingleByte
                        if (currentState == DBCS) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            currentState = SBCS;
                            da[dp++] = SI;
                        }
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte)bb;

                    }
                    sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }
    
        protected CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    char c = src.get();
                    int bb = encodeChar(c);
                    if (bb == UNMAPPABLE_ENCODING) {
                        if (Surrogate.is(c)) {
                            if (sgp.parse(c, src) < 0)
                                return sgp.error();
                            return sgp.unmappableResult();
                        }
                        return CoderResult.unmappableForLength(1);
                    }
                    if (bb > MAX_SINGLEBYTE) {  // DoubleByte
                        if (currentState == SBCS) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            currentState = DBCS;
                            dst.put(SO);
                        }
                        if (dst.remaining() < 2)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)(bb >> 8));
                        dst.put((byte)(bb));
                    } else {                  // Single-byte
                        if (currentState == DBCS) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            currentState = SBCS;
                            dst.put(SI);
                        } 
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte)bb);
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }

        public boolean canEncode(char c) {
            return encodeChar(c) != UNMAPPABLE_ENCODING;
        }

        abstract int encodeChar(char ch);
    }
}

