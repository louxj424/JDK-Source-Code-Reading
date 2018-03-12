/*
 * @(#)DataSource.java	1.0 02/04/05
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.jdbc.odbc.ee;

import sun.jdbc.odbc.JdbcOdbcDriver;
import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Local DataSource implementation for JDBC-ODBC bridge.
 * <p>
 * An application program can use by binding this as a <code> Datasource </code>.
 * <PRE>
 * .............................. 
 * InitialContext ctx = new InitialContext();
 * javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup("jdbc/odbcDB");
 * java.sql.Connection con1 = ds.getConnection();
 * java.sql.Connection con2 = ds.getConnection(user,password);
 * .............................. 
 * </PRE>
 * In the above code, jdbc/odbcDB , will be bound in the JNDI after setting necessary 
 * properties.
 * </p>
 * <p>
 * To bind in JNDI, an application need to do some thing like this.
 * <PRE> 
 * ...............
 * sun.jdbc.odbc.ee.DataSource ds = new sun.jdbc.odbc.ee.DataSource("jndiname");
 * ds.setUser("user");
 * ds.setPassword("password");
 * ds.set.......
 * ...............
 * InitialContext ctx = new InitialContext();
 * ctx.bind("jndiname",ds);
 * ...............
 * </PRE>
 * </p> 
 *
 * @version	1.0, 05/04/02
 * @author	Binod P.G
 */ 
public class DataSource extends CommonDataSource{

    /** Connection attributes set for the datasource object **/
    private ConnectionAttributes attrib = null;

    /**
     * Returns the Connection depending on the properties set.
     *
     * @return Connection object.
     */
    public Connection getConnection() throws SQLException{
    	attrib = super.getAttributes();
    	JdbcOdbcDriver driver = new JdbcOdbcDriver();
    	driver.setTimeOut(super.getLoginTimeout());
    	driver.setWriter(super.getLogWriter());    	
 	return driver.connect(attrib.getUrl(),attrib.getProperties());
    }

    /**
     * Returns the Connection depending on the properties set and userid and password.
     *
     * @param	user	userid
     * @param	password	password
     * @return Connection object.
     * @throws SQLException	If there is an error while connecting.
     */
    public Connection getConnection(String user, String password) throws SQLException{
    	attrib = super.getAttributes();    
    	JdbcOdbcDriver driver = new JdbcOdbcDriver();
    	driver.setTimeOut(super.getLoginTimeout());
    	driver.setWriter(super.getLogWriter());    	    	
    	Properties info = attrib.getProperties();
    	info.put("user", user);
    	info.put("password", password);
 	return driver.connect(attrib.getUrl(),info);    	
    }
    
    /**
     * Returns a reference object with all the information required to reconstruct the
     * datasource. This is used by JNDI.
     *
     * @return Reference object.
     */
    public Reference getReference() throws NamingException {
    	Reference ref = new Reference (this.getClass().getName(),
    					"sun.jdbc.odbc.ee.ObjectFactory", null);
    	ref.add(new StringRefAddr("databaseName", super.getDatabaseName()));
    	ref.add(new StringRefAddr("dataSourceName", super.getDataSourceName()));    	
    	ref.add(new StringRefAddr("user", super.getUser()));
    	ref.add(new StringRefAddr("password", super.getPassword())); 
    	ref.add(new StringRefAddr("charSet", super.getCharSet()));     	
    	ref.add(new StringRefAddr("loginTimeout", ""+super.getLoginTimeout()));    	    	    	    	    	
    	return ref;
    }
   
    /**
     * Returns an object that implements the given interface to allow access to non-standard methods.
     * The result may be either the object found to implement the interface or a proxy for that object.
     * If the receiver implements the interface then that is the object. If the receiver is a wrapper
     * and the wrapped object implements the interface then that is the object. Otherwise the object is
     *  the result of calling <code>unwrap</code> recursively on the wrapped object. If the receiver is not a
     * wrapper and does not implement the interface, then throws <code>SQLException</code>.
     *
     * @param interfaces A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws java.sql.SQLException If no object found that implements the interface 
     * @since 1.6
     */
    public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
        return null;
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param interfaces a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws java.sql.SQLException  if an error occurs while determining whether this is a wrapper
     * for an object with the given interface.
     * @since 1.6
     */
    public boolean isWrapperFor(Class<?> interfaces) throws SQLException {
        return false;
    }

   /**
    * Private serial version unique ID to ensure serialization
    * compatibility.
    */
    static final long serialVersionUID = -7768089779584724575L;
}
