/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.misc.resources;

/**
 * <p> This class represents the <code>ResourceBundle</code>
 * for sun.misc.
 * 
 * @author Michael Colburn
 * @version %I%, %G%
 */

public class Messages_fr extends java.util.ListResourceBundle {

    /**
     * Returns the contents of this <code>ResourceBundle</code>.     
     * <p>   
     * @return the contents of this <code>ResourceBundle</code>.
     */
    public Object[][] getContents() {
	return contents;
    }
        
    private static final Object[][] contents = {
	{ "optpkg.versionerror", "ERREUR\u00a0: Format de version utilis\u00e9 pour le fichier JAR {0} non valide. Consultez la documentation pour voir le format de version pris en charge." },
	{ "optpkg.attributeerror", "ERREUR\u00a0: L''attribut manifeste JAR {0} n\u00e9cessaire n''est pas d\u00e9fini pour le fichier {1}. " },
	{ "optpkg.attributeserror", "ERREUR\u00a0: Certains attributs manifeste JAR {0} n\u00e9cessaires ne sont pas d\u00e9finis pour le fichier {1}. " }
    };
    
}
