/*
 * %W% %E%
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.io;

import sun.nio.cs.ext.IBM833;

/**
 * Tables and data to convert Unicode to Cp833
 *
 * @author  ConverterGenerator tool
 * @version >= JDK1.1.6
 */

public class CharToByteCp833 extends CharToByteSingleByte {

    private final static IBM833 nioCoder = new IBM833();

    public String getCharacterEncoding() {
        return "Cp833";
    }

    public CharToByteCp833() {
        super.mask1 = 0xFF00;
        super.mask2 = 0x00FF;
        super.shift = 8;
        super.index1 = nioCoder.getEncoderIndex1();
        super.index2 = nioCoder.getEncoderIndex2();
    }
}

