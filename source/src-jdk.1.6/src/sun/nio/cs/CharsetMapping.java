/*
 * %W% %E%
 *
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.nio.cs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.security.*;

public class CharsetMapping {
    public final static char UNMAPPABLE_DECODING = '\uFFFD';
    public final static int  UNMAPPABLE_ENCODING = 0xFFFD;

    char[] b2cSB;                //singlebyte b->c
    char[] b2cDB1;               //dobulebyte b->c /db1
    char[] b2cDB2;               //dobulebyte b->c /db2

    int    b2Min, b2Max;         //min/max(start/end) value of 2nd byte
    int    b1MinDB1, b1MaxDB1;   //min/Max(start/end) value of 1st byte/db1
    int    b1MinDB2, b1MaxDB2;   //min/Max(start/end) value of 1st byte/db2
    int    dbSegSize;

    char[] c2b;
    char[] c2bIndex;

    // Supplementary
    char[] b2cSupp;
    char[] c2bSupp;

    // Composite
    Entry[] b2cComp;
    Entry[] c2bComp;

    public char decodeSingle(int b) {
	return b2cSB[b];
    }

    public char decodeDouble(int b1, int b2) {
	if (b2 >= b2Min && b2 < b2Max) {	    
	    b2 -= b2Min;
	    if (b1 >= b1MinDB1 && b1 <= b1MaxDB1) {
	        b1 -= b1MinDB1;
	        return b2cDB1[b1 * dbSegSize + b2];
	    } 
	    if (b1 >= b1MinDB2 && b1 <= b1MaxDB2) {
	        b1 -= b1MinDB2;
	        return b2cDB2[b1 * dbSegSize + b2];
	    }
	}
	return UNMAPPABLE_DECODING;
    }

    // for jis0213 all supplementary characters are in 0x2xxxx range,
    // so only the xxxx part is now stored, should actually store the
    // codepoint value instead.
    public char[] decodeSurrogate(int db, char[] cc) {
	int end = b2cSupp.length / 2;
	int i = Arrays.binarySearch(b2cSupp, 0, end, (char)db);
	if (i >= 0) {
	    Character.toChars(b2cSupp[end + i] + 0x20000, cc, 0);
	    return cc;
	}
        return null;
    }

    public char[] decodeComposite(Entry comp, char[] cc) {
	int i = findBytes(b2cComp, comp);
	if (i >= 0) {
	    cc[0] = (char)b2cComp[i].cp;
	    cc[1] = (char)b2cComp[i].cp2;
	    return cc;
	}
	return null;
    }

    public int encodeChar(char ch) {
        int index = c2bIndex[ch >> 8];
	if (index == 0xffff)
	    return UNMAPPABLE_ENCODING;       
	return c2b[index + (ch & 0xff)];
    }

    public int encodeSurrogate(char hi, char lo) {
        int cp = Character.toCodePoint(hi, lo);
	if (cp < 0x20000 || cp >= 0x30000)
            return UNMAPPABLE_ENCODING;
	int end = c2bSupp.length / 2;
	int i = Arrays.binarySearch(c2bSupp, 0, end, (char)cp);
	if (i >= 0)
	    return c2bSupp[end + i];
	return UNMAPPABLE_ENCODING;
    }

    public boolean isCompositeBase(Entry comp) {
        if (comp.cp <= 0x31f7 && comp.cp >= 0xe6) {
            return (findCP(c2bComp, comp) >= 0);
	}
	return false;
    }

    public int encodeComposite(Entry comp) {
	int i = findComp(c2bComp, comp);
	if (i >= 0)
	    return c2bComp[i].bs;
	return UNMAPPABLE_ENCODING;
    }

    // init the CharsetMapping object from the .dat binary file
    public static CharsetMapping get(final Class clz, final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<CharsetMapping>() {
	    public CharsetMapping run() {
	        return new CharsetMapping().load(clz.getResourceAsStream(name));
	    }
        });
    }

    /*****************************************************************************/
    public static class Entry {
        public int bs;   //byte sequence reps
        public int cp;   //Unicode codepoint
        public int cp2;  //CC of composite

        public Entry () {}
        public Entry (int bytes, int cp, int cp2) {
            this.bs = bytes;
	    this.cp = cp;
	    this.cp2 = cp2;
	}
    }
    static Comparator<Entry> comparatorBytes =
	new Comparator<Entry>() {
	    public int compare(Entry m1, Entry m2) {
	        return m1.bs - m2.bs;
	    }
	    public boolean equals(Object obj) {
	        return this == obj;
	    }
    };

    static Comparator<Entry> comparatorCP =
	new Comparator<Entry>() {
	    public int compare(Entry m1, Entry m2) {
	        return m1.cp - m2.cp;
	    }
	    public boolean equals(Object obj) {
	        return this == obj;
	    }
    };

    static Comparator<Entry> comparatorComp =
	new Comparator<Entry>() {
	    public int compare(Entry m1, Entry m2) {
	         int v = m1.cp - m2.cp;
	         if (v == 0)
		   v = m1.cp2 - m2.cp2;
		 return v;
	    }
	    public boolean equals(Object obj) {
	        return this == obj;
	    }
    };

    static int findBytes(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorBytes);
    }

    static int findCP(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorCP);
    }

    static int findComp(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorComp);
    }

    public static class Parser {
        static final Pattern basic = Pattern.compile("(?:0x)?(\\p{XDigit}++)\\s++(?:0x)?(\\p{XDigit}++)?\\s*+.*");
        static final int gBS = 1;
        static final int gCP = 2;
        static final int gCP2 = 3;

        BufferedReader reader;
        boolean closed;
        Matcher matcher;
        int gbs, gcp, gcp2;
      
        public Parser (InputStream in, Pattern p, int gbs, int gcp, int gcp2)
            throws IOException
        {
            this.reader = new BufferedReader(new InputStreamReader(in));
	    this.closed = false;
	    this.matcher = p.matcher("");
	    this.gbs = gbs;
	    this.gcp = gcp;
	    this.gcp2 = gcp2;
	}

        public Parser (InputStream in, Pattern p) throws IOException {
            this(in, p, gBS, gCP, gCP2);
	}

        public Parser (InputStream in) throws IOException {
            this(in, basic, gBS, gCP, gCP2);
	}

        protected boolean isDirective(String line) {
	    return line.startsWith("#");
	}

        protected Entry parse(Matcher matcher, Entry mapping) {
	    mapping.bs = Integer.parseInt(matcher.group(gbs), 16);
            mapping.cp = Integer.parseInt(matcher.group(gcp), 16);
            if (gcp2 <= matcher.groupCount() &&
		matcher.group(gcp2) != null)
                mapping.cp2 = Integer.parseInt(matcher.group(gcp2), 16);
	    else
	        mapping.cp2 = 0;
	    return mapping;
	}

        public Entry next() throws Exception {
	    return next(new Entry());
	}

        // returns null and closes the input stream if the eof has beenreached.
        public Entry next(Entry mapping) throws Exception {
	    if (closed)
                return null;
            String line;
	    while ((line = reader.readLine()) != null) {
	        if (isDirective(line))
		    continue;
		matcher.reset(line);
	        if (!matcher.lookingAt()) {
		    //System.out.println("Missed: " + line);
		    continue;
		}
		return parse(matcher, mapping);
            }
	    reader.close();
	    closed = true;
            return null;
	}
    }

    /*****************************************************************************/
    // tags of different charset mapping tables
    private final static int MAP_SINGLEBYTE      = 0x1; // 0..256  : c
    private final static int MAP_DOUBLEBYTE1     = 0x2; // min..max: c
    private final static int MAP_DOUBLEBYTE2     = 0x3; // min..max: c [DB2]
    private final static int MAP_SUPPLEMENT      = 0x5; //           db,c 
    private final static int MAP_SUPPLEMENT_C2B  = 0x6; //           c,db
    private final static int MAP_COMPOSITE       = 0x7; //           db,base,cc
    private final static int MAP_INDEXC2B        = 0x8; // index table of c->bb

    private static final void writeShort(OutputStream out, int data)
        throws IOException
    {
        out.write((data >>> 8) & 0xFF);
        out.write((data      ) & 0xFF);
    }

    private static final void writeShortArray(OutputStream out,
					      int type,
					      int[] array,
					      int off,
					      int size)   // exclusive
        throws IOException
    {
        writeShort(out, type);
	writeShort(out, size);
	for (int i = off; i < size; i++) {
	    writeShort(out, array[off+i]);
	}
    }

    public static final void writeSIZE(OutputStream out, int data)
        throws IOException
    {
        out.write((data >>> 24) & 0xFF);
        out.write((data >>> 16) & 0xFF);
        out.write((data >>>  8) & 0xFF);
        out.write((data       ) & 0xFF);
    }

    public static void writeINDEXC2B(OutputStream out, int[] indexC2B)
        throws IOException
    {
        writeShort(out, MAP_INDEXC2B);
	writeShort(out, indexC2B.length);
	int off = 0;
	for (int i = 0; i < indexC2B.length; i++) {
	    if (indexC2B[i] != 0) {
	        writeShort(out, off);
		off += 256;
	    } else {
	        writeShort(out, -1);
	    }
	}
    }

    public static void writeSINGLEBYTE(OutputStream out, int[] sb)
        throws IOException
    {
        writeShortArray(out, MAP_SINGLEBYTE, sb, 0, 256);
    }

    private static void writeDOUBLEBYTE(OutputStream out,
					int type,
					int[] db,
					int b1Min, int b1Max,
					int b2Min, int b2Max)
        throws IOException
    {
        writeShort(out, type);
	writeShort(out, b1Min);
	writeShort(out, b1Max);
	writeShort(out, b2Min);
	writeShort(out, b2Max);
	writeShort(out, (b1Max - b1Min + 1) * (b2Max - b2Min + 1));

	for (int b1 = b1Min; b1 <= b1Max; b1++) {
            for (int b2 = b2Min; b2 <= b2Max; b2++) {
	        writeShort(out, db[b1 * 256 + b2]);
	    }
	}
    }
    public static void writeDOUBLEBYTE1(OutputStream out,
					int[] db,
					int b1Min, int b1Max,
					int b2Min, int b2Max)
        throws IOException
    {
        writeDOUBLEBYTE(out, MAP_DOUBLEBYTE1, db, b1Min, b1Max, b2Min, b2Max);
    }

    public static void writeDOUBLEBYTE2(OutputStream out,
					int[] db,
					int b1Min, int b1Max,
					int b2Min, int b2Max)
        throws IOException
    {
        writeDOUBLEBYTE(out, MAP_DOUBLEBYTE2, db, b1Min, b1Max, b2Min, b2Max);
    }

    // the c2b table is output as well
    public static void writeSUPPLEMENT(OutputStream out, Entry[] supp, int size)
        throws IOException
    {
        writeShort(out, MAP_SUPPLEMENT);
        writeShort(out, size * 2);
        // db at first half, cc at the low half
	for (int i = 0; i < size; i++) {
	    writeShort(out, supp[i].bs);
	}
	for (int i = 0; i < size; i++) {
	    writeShort(out, supp[i].cp);
	}

        //c2b
        writeShort(out, MAP_SUPPLEMENT_C2B);
	writeShort(out, size*2);
	Arrays.sort(supp, 0, size, comparatorCP);
	for (int i = 0; i < size; i++) {
	    writeShort(out, supp[i].cp);
	}
	for (int i = 0; i < size; i++) {
	    writeShort(out, supp[i].bs);
	}
    }

    public static void writeCOMPOSITE(OutputStream out, Entry[] comp, int size)
        throws IOException
    {
        writeShort(out, MAP_COMPOSITE);
	writeShort(out, size*3);
	// comp is sorted already
	for (int i = 0; i < size; i++) {
	    writeShort(out, (char)comp[i].bs);
	    writeShort(out, (char)comp[i].cp);
	    writeShort(out, (char)comp[i].cp2);
	}
    }

    private static final boolean readNBytes(InputStream in, byte[] bb, int N)
        throws IOException
    {
        int off = 0;
	while (N > 0) {
            int n = in.read(bb, off, N);
	    if (n == -1)
	        return false;
	    N = N - n;
	    off += n;
	}
	return true;
    }

    int off = 0;
    byte[] bb;
    private char[] readCharArray() {
        // first 2 bytes are the number of "chars" stored in this table
        int size  = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        char [] cc = new char[size];
	for (int i = 0; i < size; i++) {
	    cc[i] = (char)(((bb[off++]&0xff)<<8) | (bb[off++]&0xff));
	}
	return cc;
    }

    void readSINGLEBYTE() {
        char[] map = readCharArray();
	for (int i = 0; i < map.length; i++) {
	    char c = map[i];
	    if (c != UNMAPPABLE_DECODING) {
	        c2b[c2bIndex[c >> 8] + (c&0xff)] = (char)i;
	    }
	}
	b2cSB = map;
    }

    void readINDEXC2B() {
        char[] map = readCharArray();
	for (int i = map.length - 1; i >= 0; i--) {
	    if (c2b == null && map[i] != -1) {
	        c2b = new char[map[i] + 256];
		Arrays.fill(c2b, (char)UNMAPPABLE_ENCODING);
		break;
	    }
	}
	c2bIndex = map;
    }

    char[] readDB(int b1Min, int b2Min, int segSize) {
        char[] map = readCharArray();
	for (int i = 0; i < map.length; i++) {
	    char c = map[i];
	    if (c != UNMAPPABLE_DECODING) {
	        int b1 = i / segSize;
	        int b2 = i % segSize;
	        int b = (b1 + b1Min)* 256 + (b2 + b2Min);
	        //System.out.printf("    DB %x\t%x%n", b, c & 0xffff);
	        c2b[c2bIndex[c >> 8] + (c&0xff)] = (char)(b);
	    }
	}
	return map;
    }

    void readDOUBLEBYTE1() {
	b1MinDB1 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b1MaxDB1 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b2Min =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b2Max =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        dbSegSize = b2Max - b2Min + 1;
        b2cDB1 = readDB(b1MinDB1, b2Min, dbSegSize);
    }

    void readDOUBLEBYTE2() {
	b1MinDB2 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b1MaxDB2 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b2Min =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
	b2Max =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        dbSegSize = b2Max - b2Min + 1;
        b2cDB2 = readDB(b1MinDB2, b2Min, dbSegSize);
    }

    void readCOMPOSITE() {
        char[] map = readCharArray();
        int mLen = map.length/3;
	b2cComp = new Entry[mLen];
	c2bComp = new Entry[mLen];
	for (int i = 0, j= 0; i < mLen; i++) {
	    Entry m = new Entry();
	    m.bs = map[j++];
	    m.cp = map[j++];
	    m.cp2 = map[j++];
	    b2cComp[i] = m;
	    c2bComp[i] = m;
	}
	Arrays.sort(c2bComp, 0, c2bComp.length, comparatorComp);
    }

    CharsetMapping load(InputStream in) {
        try {
            // The first 4 bytes are the size of the total data followed in
            // this .dat file.
	    int len = ((in.read()&0xff) << 24) | ((in.read()&0xff) << 16) |
	              ((in.read()&0xff) << 8) | (in.read()&0xff);
	    bb = new byte[len];
            off = 0;
	    //System.out.printf("In : Total=%d%n", len);
	    // Read in all bytes
	    if (!readNBytes(in, bb, len))
	        throw new RuntimeException("Corrupted data file");
	    in.close();
	
	    while (off < len) {
	        int type = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
		switch(type) {
		case MAP_INDEXC2B:
		    readINDEXC2B();
		    break;
		case MAP_SINGLEBYTE:
		    readSINGLEBYTE();
		    break;
		case MAP_DOUBLEBYTE1:
		    readDOUBLEBYTE1();
		    break;
		case MAP_DOUBLEBYTE2:
		    readDOUBLEBYTE2();
	            break;
		case MAP_SUPPLEMENT:
		    b2cSupp = readCharArray();
	            break;
		case MAP_SUPPLEMENT_C2B:
		    c2bSupp = readCharArray();
	            break;
		case MAP_COMPOSITE:
		    readCOMPOSITE();
	            break;
		default:
		    throw new RuntimeException("Corrupted data file");
		}
	    }
	    bb = null;
	    return this;
	} catch (IOException x) {
	    x.printStackTrace();
	    return null;
	}
    }
}


