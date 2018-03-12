/*
 * %W% %E%
 *
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.provider.certpath;

import java.util.Set;
import java.util.Collection;
import java.util.Locale;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXCertPathChecker;

import sun.security.x509.AlgorithmId;

/**
 * AlgorithmChecker is a <code>PKIXCertPathChecker</code> that checks that
 * the signature algorithm of the specified certificate is not disabled.
 *
 * @author      Xuelei Fan
 */
final public class AlgorithmChecker extends PKIXCertPathChecker {

    // the disabled algorithms
    private static final String[] disabledAlgorithms = new String[] {"md2"};

    // singleton instance
    static final AlgorithmChecker INSTANCE = new AlgorithmChecker();

    /**
     * Default Constructor
     */
    private AlgorithmChecker() {
        // do nothing
    }

    /**
     * Return a AlgorithmChecker instance.
     */
    static AlgorithmChecker getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the internal state of the checker from parameters
     * specified in the constructor.
     */
    public void init(boolean forward) throws CertPathValidatorException {
        // do nothing
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set<String> getSupportedExtensions() {
        return null;
    }

    /**
     * Checks the signature algorithm of the specified certificate.
     */
    public void check(Certificate cert, Collection<String> unresolvedCritExts)
            throws CertPathValidatorException {
        check(cert);
    }

    public static void check(Certificate cert)
            throws CertPathValidatorException {
        X509Certificate xcert = (X509Certificate)cert;
        check(xcert.getSigAlgName());
    }

    static void check(AlgorithmId aid) throws CertPathValidatorException {
        check(aid.getName());
    }

    static void check(X509CRL crl) throws CertPathValidatorException {
        check(crl.getSigAlgName());
    }

    private static void check(String algName)
            throws CertPathValidatorException {

        String lowerCaseAlgName = algName.toLowerCase(Locale.ENGLISH);

        for (String disabled : disabledAlgorithms) {
            // checking the signature algorithm name
            if (lowerCaseAlgName.indexOf(disabled) != -1) {
                throw new CertPathValidatorException(
                    "algorithm check failed: " + algName + " is disabled");
            }
        }
    }

}
