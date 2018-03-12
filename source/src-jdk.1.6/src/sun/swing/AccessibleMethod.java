/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.swing;

import java.security.*;
import java.lang.reflect.*;

/**
 * A utility for accessing and invoking methods, via reflection,
 * that would otherwise be unaccessible.
 *
 * @version %I% %G%
 * @author Shannon Hickey
 */
public class AccessibleMethod {

    private final Method method;

    /**
     * Construct an instance for the given params.
     *
     * @param klass the class to which the method belongs
     * @param methodName the name of the method
     * @param paramTypes the paramater type array
     * @throws NullPointerException if <code>klass</code>
     *         or <code>name</code> is <code>null</code>
     * @throws NoSuchMethodException if the method can't be found
     */
    public AccessibleMethod(Class klass,
                            String methodName,
                            Class ... paramTypes) throws NoSuchMethodException {
        try {
            method = AccessController.doPrivileged(
                new AccessMethodAction(klass, methodName, paramTypes));
        } catch (PrivilegedActionException e) {
            throw (NoSuchMethodException)e.getCause();
        }
    }

    /**
     * Invoke the method that this object represents.
     * Has the same behavior and throws the same exceptions as
     * <code>java.lang.reflect.Method.invoke</code> with one
     * exception: This method does not throw
     * <code>IllegalAccessException</code> since the target
     * method has already been made accessible.
     *
     * @param obj the object the underlying method is invoked from
     * @param args the arguments used for the method call
     * @return the result of dispatching the method represented by
     *         this object on <code>obj</code> with parameters
     *         <code>args</code>
     * @see java.lang.reflect.Method#invoke
     */
    public Object invoke(Object obj, Object ... args)
            throws IllegalArgumentException, InvocationTargetException {

        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            // should never happen since we've made it accessible
            throw new AssertionError("accessible method inaccessible");
        }
    }

    /**
     * Invoke the method that this object represents, with the
     * expectation that the method being called throws no
     * checked exceptions.
     * <p>
     * Simply calls <code>this.invoke(obj, args)</code>
     * but catches any <code>InvocationTargetException</code>
     * and returns the cause wrapped in a runtime exception.
     *
     * @param obj the object the underlying method is invoked from
     * @param args the arguments used for the method call
     * @return the result of dispatching the method represented by
     *         this object on <code>obj</code> with parameters
     *         <code>args</code>
     * @see #invoke
     */
    public Object invokeNoChecked(Object obj, Object ... args) {
        try {
            return invoke(obj, args);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException)ex.getCause();
            } else {
                throw new RuntimeException(ex.getCause());
            }
        }
    }

    /** The action used to fetch the method and make it accessible */
    private static class AccessMethodAction implements PrivilegedExceptionAction<Method> {
        private final Class klass;
        private final String methodName;
        private final Class[] paramTypes;

        public AccessMethodAction(Class klass,
                                  String methodName,
                                  Class ... paramTypes) {

            this.klass = klass;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
        }

        public Method run() throws NoSuchMethodException {
            Method method = klass.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        }
    }
}
