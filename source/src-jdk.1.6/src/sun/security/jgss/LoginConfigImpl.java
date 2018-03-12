/*
 * %W% %E%
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.jgss;

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.ietf.jgss.Oid;

/**
 * A Configuration implementation especially designed for JGSS.
 *
 * @author weijun.wang
 * @since 1.6
 */
public class LoginConfigImpl extends Configuration {
    
    Configuration config;
    private final int caller;
    private final String mechName;
    private static final sun.security.util.Debug debug =
	sun.security.util.Debug.getInstance("gssloginconfig", "\t[GSS LoginConfigImpl]");

    /**
     * A new instance of LoginConfigImpl must be created for each login request
     * since it's only used by a single (caller, mech) pair
     * @param caller defined in GSSUtil as CALLER_XXX final fields
     * @param oid defined in GSSUtil as XXX_MECH_OID final fields
     */
    public LoginConfigImpl(int caller, Oid mech) {
        
        this.caller = caller;
        
        if (mech.equals(GSSUtil.GSS_KRB5_MECH_OID)) {
            mechName = "krb5";
        } else {
            throw new IllegalArgumentException(mech.toString() + " not supported");
        }
        config = java.security.AccessController.doPrivileged
                (new java.security.PrivilegedAction <Configuration> () {
            public Configuration run() {
                return Configuration.getConfiguration();
            }
        });
    }
    
    /**
     * @param name Almost useless, since the (caller, mech) is already passed
     *             into constructor. The only use will be detecting OTHER which
     *             is called in LoginContext
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {

        AppConfigurationEntry[] entries = null;
        
        // This is the second call from LoginContext, which we will just ignore
        if ("OTHER".equalsIgnoreCase(name)) {
            return null;
        }
        
        String[] alts = null;
        
        // Compatibility:
        // For the 4 old callers, old entry names will be used if the new
        // entry name is not provided.
        
        if ("krb5".equals(mechName)) {
            switch (caller) {
            case GSSUtil.CALLER_INITIATE:
                alts = new String[] {
                    "com.sun.security.jgss.krb5.initiate",
                    "com.sun.security.jgss.initiate",
                };
                break;
            case GSSUtil.CALLER_ACCEPT:
                alts = new String[] {
                    "com.sun.security.jgss.krb5.accept",
                    "com.sun.security.jgss.accept",
                };
                break;
            case GSSUtil.CALLER_SSL_CLIENT:
                alts = new String[] {
                    "com.sun.security.jgss.krb5.initiate",
                    "com.sun.net.ssl.client",
                };
                break;
            case GSSUtil.CALLER_SSL_SERVER:
                alts = new String[] {
                    "com.sun.security.jgss.krb5.accept",
                    "com.sun.net.ssl.server",
                };
                break;
            case GSSUtil.CALLER_HTTP_NEGOTIATE:
                alts = new String[] {
                    "com.sun.security.jgss.krb5.initiate",
                };
                break;
            case GSSUtil.CALLER_UNKNOWN:
                // should never use
                throw new AssertionError("caller cannot be unknown");
            default:
                throw new AssertionError("caller not defined");
            }
        } else {
            throw new IllegalArgumentException(mechName + " not supported");
            // No other mech at the moment, maybe --
            /*
            switch (caller) {
            case GSSUtil.CALLER_INITIATE:
            case GSSUtil.CALLER_SSL_CLIENT:
            case GSSUtil.CALLER_HTTP_NEGOTIATE:
                alts = new String[] {
                    "com.sun.security.jgss." + mechName + ".initiate",
                };
                break;
            case GSSUtil.CALLER_ACCEPT:
            case GSSUtil.CALLER_SSL_SERVER:
                alts = new String[] {
                    "com.sun.security.jgss." + mechName + ".accept",
                };
                break;
            case GSSUtil.CALLER_UNKNOWN:
                // should never use
                throw new AssertionError("caller cannot be unknown");
            default:
                throw new AssertionError("caller not defined");
            }
             */            
        }
        for (String alt: alts) {
            if (debug != null) {
                debug.println("Trying " + alt);
            }
            entries = config.getAppConfigurationEntry(alt);
            if (entries != null) {
                break;
            }
        }
        
        return entries;
    }
    
    /**
     * We plan to define a default value for a caller-mech pair, so that when
     * user hasn't specify a login config file or hasn't provide an entry.
     */
    /*private AppConfigurationEntry[] getDefaultConfigurationEntry(int caller) {
        HashMap <String, String> options = new HashMap <String, String> (2);
        
        if (mechName == null || mechName.equals("krb5")) {
            if (caller == GSSUtil.CALLER_HTTP_NEGOTIATE) {
                options.put("useTicketCache", "true");
                // XXX: change the false to true later
                options.put("doNotPrompt", "false");
            } else if (isServerSide(caller)) {
                options.put("useKeyTab", "true");
                options.put("storeKey", "true");
            } else {
                options.put("useTicketCache", "true");
                options.put("doNotPrompt", "true");
            }
            return new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)
            };
        }
        return null;
    }
    
    private static boolean isServerSide (int caller) {
        return (caller % 2 == 0);
    }*/    
}
