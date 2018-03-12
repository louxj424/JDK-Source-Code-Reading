  /*
   * @(#)UIClientPropertyKey.java 1.1 06/08/17
   *
   * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
   * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
   */
  
  package sun.swing;
  
  /**
   * An implementation of {@code UIClientPropertyKey} that wraps a {@code String}.
   *
   * @author Shannon Hickey
   */
  public class StringUIClientPropertyKey implements UIClientPropertyKey {
  
      private final String key;
  
      public StringUIClientPropertyKey(String key) {
          this.key = key;
      }
  
      public String toString() {
          return key;
      }
  }
