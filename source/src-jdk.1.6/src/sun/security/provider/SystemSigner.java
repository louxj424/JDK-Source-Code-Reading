/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.provider;

import java.util.*;
import java.security.*;

/**
 * SunSecurity signer. Like SystemIdentity, it has a trust bit, which
 * can be set by SunSecurity classes, and a set of accessors for other
 * classes in sun.security.*.
 *
 * @version %I%, %G%
 * @author Benjamin Renaud
 */

public class SystemSigner extends Signer {

    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = -2127743304301557711L;

    /* Is this signer trusted */
    private boolean trusted = false;

    /**
     * Construct a signer with a given name.
     */
    public SystemSigner(String name) {
	super(name);
    }

    /**
     * Construct a signer with a name and a scope.
     *
     * @param name the signer's name.
     *
     * @param scope the scope for this signer.
     */
    public SystemSigner(String name, IdentityScope scope)
     throws KeyManagementException {

	super(name, scope);
    }

    /* Set the trust status of this signer */
    void setTrusted(boolean trusted) {
	this.trusted = trusted;
    }

    /**
     * Returns true if this signer is trusted.
     */
    public boolean isTrusted() {
	return trusted;
    }

    /* friendly callback for set keys */
    void setSignerKeyPair(KeyPair pair)
    throws InvalidParameterException, KeyException {
	setKeyPair(pair);
    }

    /* friendly callback for getting private keys */
    PrivateKey getSignerPrivateKey() {
	return getPrivateKey();
    }

    void setSignerInfo(String s) {
	setInfo(s);
    }

    /**
     * Call back method into a protected method for package friends.
     */
    void addSignerCertificate(Certificate cert) throws KeyManagementException {
	addCertificate(cert);
    }

    void clearCertificates() throws KeyManagementException {
	Certificate[] certs = certificates();
	for (int i = 0; i < certs.length; i++) {
	    removeCertificate(certs[i]);
	}
    }

    public String toString() {
	String trustedString = "not trusted";
	if (trusted) {
	    trustedString = "trusted";
	}
	return super.toString() + "[" + trustedString + "]";
    }
}
