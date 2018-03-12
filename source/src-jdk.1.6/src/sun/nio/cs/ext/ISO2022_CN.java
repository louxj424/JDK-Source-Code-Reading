/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * %W%	%E%
 */

package sun.nio.cs.ext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CharacterCodingException;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.US_ASCII;

public class ISO2022_CN
    extends Charset
    implements HistoricallyNamedCharset
{
    private static final byte ISO_ESC = 0x1b;
    private static final byte ISO_SI = 0x0f;
    private static final byte ISO_SO = 0x0e;
    private static final byte ISO_SS2_7 = 0x4e;
    private static final byte ISO_SS3_7 = 0x4f;
    private static final byte MSB = (byte)0x80;
    private static final char REPLACE_CHAR = '\uFFFD';

    private static final byte SODesigGB = 0;
    private static final byte SODesigCNS = 1;
    private static final byte SS2Desig = 2;  //CNS-1
    private static final byte SS3Desig = 3;  //CNS-2

    private static CharsetDecoder gb2312Decoder = null;
    private static CharsetDecoder cnsDecoder = null;

    public ISO2022_CN() {
	super("ISO-2022-CN", ExtendedCharsets.aliasesFor("ISO-2022-CN"));
    }

    public String historicalName() {
	return "ISO2022CN";
    }

    public boolean contains(Charset cs) {
	return ((cs instanceof EUC_CN)     // GB2312-80 repertoire 
		|| (cs instanceof US_ASCII)
		|| (cs instanceof EUC_TW)  // CNS11643 repertoire
		|| (cs instanceof ISO2022_CN));
    }

    public CharsetDecoder newDecoder() {
	gb2312Decoder = new EUC_CN().newDecoder();
	cnsDecoder = new EUC_TW().newDecoder();
	return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
	throw new UnsupportedOperationException();
    }

    public boolean canEncode() {
	return false;
    }

    static class Decoder extends CharsetDecoder {

	private boolean shiftOut;
	private byte currentSODesig;
	private ByteBuffer bBuf;

	Decoder(Charset cs) {
	    super(cs, 1.0f, 1.0f);
	    shiftOut = false;
	    currentSODesig = SODesigGB;
	    bBuf = ByteBuffer.allocate(4);
	}

	protected void implReset() {
	    shiftOut= false;
	    currentSODesig = SODesigGB;
        }

	private char cnsDecode(byte byte1, byte byte2,
			       byte byte3, byte byte4) { 
	    byte3 |= MSB;
	    byte4 |= MSB;
            bBuf.clear();
	    try {
		bBuf.put(byte1);
		bBuf.put(byte2);
		bBuf.put(byte3);
		bBuf.put(byte4);
		bBuf.flip();
		return cnsDecoder.decode(bBuf).get();
	    } catch (CharacterCodingException e) {}
            return REPLACE_CHAR;
        }

	private char SODecode(byte byte1, byte byte2, byte SOD) {
	    byte1 |= MSB;
	    byte2 |= MSB;
            bBuf.clear();
            try {
	        bBuf.put(byte1);
	        bBuf.put(byte2);
	        bBuf.flip();
                if (SOD == SODesigGB)
		    return gb2312Decoder.decode(bBuf).get();
                else if (SOD == SODesigCNS)
		    return cnsDecoder.decode(bBuf).get();
            } catch (CharacterCodingException e) {}
	    return REPLACE_CHAR;
	}

	private CoderResult decodeBufferLoop(ByteBuffer src,
					     CharBuffer dst)
	{
	    int mark = src.position();
	    byte b1 = 0, b2 = 0, b3 = 0, b4 = 0;
	    int inputSize = 0;

	    try {
		while (src.hasRemaining()) {
		    b1 = src.get();
		    inputSize = 1;

		    while (b1 == ISO_ESC ||
		           b1 == ISO_SO ||
		           b1 == ISO_SI) {
			if (b1 == ISO_ESC) {  // ESC  
			    currentSODesig = SODesigGB;

			    if (src.remaining() < 1)
				return CoderResult.UNDERFLOW;

			    b2 = src.get();
			    inputSize++;

			    if ((b2 & (byte)0x80) != 0)
				return CoderResult.malformedForLength(inputSize);

			    if (b2 == (byte)0x24) { 
				if (src.remaining() < 1)
				    return CoderResult.UNDERFLOW;

				b3 = src.get();
				inputSize++;

			        if ((b3 & (byte)0x80) != 0)
				    return CoderResult.malformedForLength(inputSize);
				if (b3 == 'A'){		     // "$A"
				    currentSODesig = SODesigGB;
				} else if (b3 == ')') {
				    if (src.remaining() < 1)
					return CoderResult.UNDERFLOW;
				    b4 = src.get();
				    inputSize++;
				    if (b4 == 'A'){          // "$)A"
					currentSODesig = SODesigGB;
				    } else if (b4 == 'G'){   // "$)G"
				        currentSODesig = SODesigCNS;
				    } else {
					return CoderResult.malformedForLength(inputSize);
				    }
				} else if (b3 == '*') {
				    if (src.remaining() < 1)
					return CoderResult.UNDERFLOW;
				    b4 = src.get();
				    inputSize++;
				    if (b4 != 'H') {	     // "$*H"
				        //SS2Desig -> CNS-P1
					return CoderResult.malformedForLength(inputSize);
                                    }
				} else if (b3 == '+') {
				    if (src.remaining() < 1)
					return CoderResult.UNDERFLOW;
				    b4 = src.get();
				    inputSize++;
				    if (b4 != 'I'){	     // "$+I"
				        //SS3Desig -> CNS-P2.
					return CoderResult.malformedForLength(inputSize);
				    }
				} else {
					return CoderResult.malformedForLength(inputSize);
				}
			    } else if (b2 == ISO_SS2_7 || b2 == ISO_SS3_7) {
				if (src.remaining() < 2)
				    return CoderResult.UNDERFLOW;
				b3 = src.get();
				b4 = src.get();
				inputSize += 2;
				b2 = (b2 == ISO_SS2_7) ? (byte)0xa2:
							 (byte)0xa3;
				if (dst.remaining() < 1)
				    return CoderResult.OVERFLOW;
                                //SS2->CNS-P1, SS3->CNS-P2
				dst.put(cnsDecode((byte)0x8e,
                                                  (byte)b2,
						  (byte)b3,
                                                  (byte)b4));
			    } else {
				return CoderResult.malformedForLength(inputSize);
			    }
		        } else if (b1 == ISO_SO) {
			    shiftOut = true;
			} else if (b1 == ISO_SI) { // shift back in
			    shiftOut = false;
			}
			mark += inputSize;
			if (src.remaining() < 1)
			    return CoderResult.UNDERFLOW;
			b1 = src.get();
			inputSize = 1;
		    }

		    if (dst.remaining() < 1)
			return CoderResult.OVERFLOW;

		    if (!shiftOut) {
			dst.put((char)b1);
			mark += inputSize;
		    } else { 
			if (src.remaining() < 1)
			    return CoderResult.UNDERFLOW;
			b2 = src.get();
			inputSize++;
			dst.put(SODecode((byte)b1, (byte)b2, currentSODesig));
			mark += inputSize;
		    }
		}
		return CoderResult.UNDERFLOW;
	    } finally {
		src.position(mark);
	    }
	}

	private CoderResult decodeArrayLoop(ByteBuffer src,
					    CharBuffer dst)
	{

	    int inputSize = 0;
	    byte b1 = 0, b2 = 0, b3 = 0, b4 = 0;

	    byte[] sa = src.array();
	    int sp = src.arrayOffset() + src.position();
	    int sl = src.arrayOffset() + src.limit();
	    assert (sp <= sl);
	    sp = (sp <= sl ? sp : sl);

	    char[] da = dst.array();
	    int dp = dst.arrayOffset() + dst.position();
	    int dl = dst.arrayOffset() + dst.limit();
	    assert (dp <= dl);
	    dp = (dp <= dl ? dp : dl);
	    try {
		while (sp < sl) {
		    b1 = sa[sp];
		    inputSize = 1;

		    while (b1 == ISO_ESC || b1 == ISO_SO || b1 == ISO_SI) {
			if (b1 == ISO_ESC) {  // ESC  
			    currentSODesig = SODesigGB;

			    if (sp + 2 > sl)
				return CoderResult.UNDERFLOW;

			    b2 = sa[sp + 1];
			    inputSize++;

			    if ((b2 & (byte)0x80) != 0)
				return CoderResult.malformedForLength(inputSize);
			    if (b2 == (byte)0x24) { 
				if (sp + 3 > sl)
				    return CoderResult.UNDERFLOW;

				b3 = sa[sp + 2];
				inputSize++;

			        if ((b3 & (byte)0x80) != 0)
				    return CoderResult.malformedForLength(inputSize);
				if (b3 == 'A'){		     // "$A"
				    /* <ESC>$A is not a legal designator sequence for 
                                       ISO2022_CN, it is listed as an escape sequence 
                                       for GB2312 in ISO2022-JP-2. Keep it here just for
                                       the sake of "compatibility".
              			     */
				    currentSODesig = SODesigGB;
				} else if (b3 == ')') {
				    if (sp + 4 > sl)
					return CoderResult.UNDERFLOW;
				    b4 = sa[sp + 3];
				    inputSize++;

				    if (b4 == 'A'){          // "$)A"
					currentSODesig = SODesigGB;
				    } else if (b4 == 'G'){   // "$)G"
				        currentSODesig = SODesigCNS;
				    } else {
					return CoderResult.malformedForLength(inputSize);
				    }
				} else if (b3 == '*') {
				    if (sp + 4 > sl)
					return CoderResult.UNDERFLOW;
				    b4 = sa[sp + 3];
				    inputSize++;
				    if (b4 != 'H'){	     // "$*H"
					return CoderResult.malformedForLength(inputSize);
				    }
				} else if (b3 == '+') {
				    if (sp + 4 > sl) 
					return CoderResult.UNDERFLOW;
				    b4 = sa[sp + 3];
				    inputSize++;
				    if (b4 != 'I'){	     // "$+I"
					return CoderResult.malformedForLength(inputSize);
				    }
				} else {
					return CoderResult.malformedForLength(inputSize);
				}
			    } else if (b2 == ISO_SS2_7 || b2 == ISO_SS3_7) {
				if (sp + 4 > sl) {
				    return CoderResult.UNDERFLOW;
				}
				b3 = sa[sp + 2];
				b4 = sa[sp + 3];
				b2 = (b2 == ISO_SS2_7) ? (byte)0xa2:
							 (byte)0xa3;
				if (dl - dp < 1)  {
				    return CoderResult.OVERFLOW;
				}
                                inputSize += 2;
				da[dp++] = cnsDecode((byte)0x8e,
                                                  (byte)b2,
						  (byte)b3,
                                                  (byte)b4);
			    } else {
				return CoderResult.malformedForLength(inputSize);
			    }
		        } else if (b1 == ISO_SO) {
			    shiftOut = true;
			} else if (b1 == ISO_SI) { // shift back in
			    shiftOut = false;
			}
			sp += inputSize;
			if (sp + 1 > sl)
			    return CoderResult.UNDERFLOW;
			b1 = sa[sp];
			inputSize = 1;
		    }

		    if (dl - dp < 1) {
			return CoderResult.OVERFLOW;
		    }

		    if (!shiftOut) {
			da[dp++] = (char)(b1);
		    } else { 
		        if (sp + 2 > sl)
			    return CoderResult.UNDERFLOW;
			b2 = sa[sp + 1];
			inputSize++;
			da[dp++] = SODecode((byte)b1, (byte)b2, currentSODesig);
		    }
		    sp += inputSize;
		}
		return CoderResult.UNDERFLOW;
	    } finally {
		src.position(sp - src.arrayOffset());
		dst.position(dp - dst.arrayOffset());
	    }
	}

	protected CoderResult decodeLoop(ByteBuffer src,
					 CharBuffer dst)
	{
	    if (src.hasArray() && dst.hasArray())
		return decodeArrayLoop(src, dst);
	    else
		return decodeBufferLoop(src, dst);
	}
    }
}
