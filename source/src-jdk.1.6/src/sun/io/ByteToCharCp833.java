/*
 * %W% %E%
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


package sun.io;

import sun.nio.cs.ext.IBM833;

/**
 * A table to convert to Cp833 to Unicode
 *
 * @author  ConverterGenerator tool
 * @version >= JDK1.1.6
 */

public class ByteToCharCp833 extends ByteToCharSingleByte {

    private final static IBM833 nioCoder = new IBM833();

    public String getCharacterEncoding() {
        return "Cp833";
    }

    public ByteToCharCp833() {
        super.byteToCharTable = nioCoder.getDecoderSingleByteMappings();
    }
}

