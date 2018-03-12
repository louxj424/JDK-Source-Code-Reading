/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * Generic PKCS Parsing exception. 
 * 
 * @version %I%	%G%
 * @author Benjamin Renaud 
 */

package sun.security.pkcs;

import java.io.IOException;

public class ParsingException extends IOException {

    private static final long serialVersionUID = -6316569918966181883L;

    public ParsingException() {
	super();
    }

    public ParsingException(String s) {
	super(s);
    }
}
