/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//----------------------------------------------------------------------------
//
// Module:      JdbcOdbcTypes.java
//
// Description: Defines ODBC constants
//
// Product:     JDBCODBC (Java DataBase Connectivity using
//              Open DataBase Connectivity)
//
// Author:      Jesse Davis
//
// Date:        March, 2001
//
//----------------------------------------------------------------------------

package sun.jdbc.odbc;

import java.sql.Types;

public class JdbcOdbcTypes {

    /* This is for ODBC drivers returning the SQL_WCHAR datatype,
       ODBC WCHAR datatype is not the same as JDBC NCHAR datatype.  
     */
	public final static int WCHAR 		=  -8;

    /* This is for ODBC drivers returning the SQL_WCHAR datatype,
       ODBC WVARCHAR datatype is not the same as JDBC NVARCHAR datatype.  
     */
	public final static int WVARCHAR 		=  -9;

    /* This is for ODBC drivers returning the SQL_WCHAR datatype,
       ODBC WLONGVARCHAR datatype is not the same as JDBC NLONGVARCHAR datatype.
     */
	public final static int WLONGVARCHAR 		=  -10;



    // Prevent instantiation
    private JdbcOdbcTypes() {}
}


