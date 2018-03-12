/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.jgss.wrapper;

import java.util.HashMap;
import java.security.Provider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.ietf.jgss.Oid;
import sun.security.action.PutAllAction;

/**
 * Defines the Sun NativeGSS provider for plugging in the
 * native GSS mechanisms to Java GSS.
 *
 * List of supported mechanisms depends on the local
 * machine configuration.
 *
 * @author Yu-Ching Valerie Peng
 * @version %I%, %G%
 */

public final class SunNativeProvider extends Provider {

    private static final long serialVersionUID = -238911724858694204L;

    private static final String NAME = "SunNativeGSS";
    private static final String INFO = "Sun Native GSS provider";
    private static final String MF_CLASS = 
	"sun.security.jgss.wrapper.NativeGSSFactory";
    private static final String LIB_PROP = "sun.security.jgss.lib";
    private static final String DEBUG_PROP = "sun.security.nativegss.debug";
    private static HashMap MECH_MAP;
    static final Provider INSTANCE = new SunNativeProvider();
    static boolean DEBUG; 
    static void debug(String message) {
	if (DEBUG) {
	    if (message == null) {
		throw new NullPointerException();
	    }
	    System.out.println(NAME + ": " + message);
	}
    }

    static {
	MECH_MAP = 
	    AccessController.doPrivileged(new PrivilegedAction<HashMap>() {
		    public HashMap run() {
			DEBUG = Boolean.parseBoolean
			    (System.getProperty(DEBUG_PROP));
                        try {
                            System.loadLibrary("j2gss");
                        } catch (Error err) {
                            debug("No j2gss library found!");
                            if (DEBUG) err.printStackTrace();
                            return null;
                        }
			String gssLib = System.getProperty(LIB_PROP);
			if (gssLib == null || gssLib.trim().equals("")) {
			    String osname = System.getProperty("os.name");
			    if (osname.startsWith("SunOS")) {
				gssLib = "libgss.so";
			    } else if (osname.startsWith("Linux")) {
				gssLib = "libgssapi.so";
			    }
			}
			if (GSSLibStub.init(gssLib)) {
			    debug("Loaded GSS library: " + gssLib);
			    Oid[] mechs = GSSLibStub.indicateMechs();
			    HashMap map = new HashMap(); 
			    for (int i = 0; i < mechs.length; i++) {
				debug("Native MF for " + mechs[i]);
				map.put("GssApiMechanism." + mechs[i], 
				        MF_CLASS);
			    }
			    return map;
			}
			return null;
		    }
		});
    }

    public SunNativeProvider() {
	/* We are the Sun NativeGSS provider */
	super(NAME, 1.0, INFO);

	if (MECH_MAP != null) {
	    AccessController.doPrivileged(new PutAllAction(this, MECH_MAP));
	}
    }
}
