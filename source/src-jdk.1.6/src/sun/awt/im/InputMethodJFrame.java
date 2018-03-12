/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.im;

import javax.swing.JFrame;
import javax.swing.JRootPane;

/**
 * Implements a Swing based input method window that provides the minimal
 * functionality as specified in
 * {@link java.awt.im.spi.InputMethodContext#createInputMethodJFrame}.
 *
 * @version %I% %G%
 */
public class InputMethodJFrame
        extends JFrame
        implements InputMethodWindow {

    InputContext inputContext = null;

    /**
     * Constructs a Swing based input method window.
     */
    public InputMethodJFrame(String title, InputContext context) {
        super(title);
        //InputMethodJFrame never has LookAndFeel decoration
        if(JFrame.isDefaultLookAndFeelDecorated())
        {
           this.setUndecorated(true);
           this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        }
        if (context != null) {
            this.inputContext = context;
        }
	setFocusableWindowState(false);
    }
    
    public void setInputContext(InputContext inputContext) {
        this.inputContext = inputContext;
    }

    public java.awt.im.InputContext getInputContext() {
        if (inputContext != null) {
            return inputContext;
        } else {
            return super.getInputContext();
        }
    }
}
