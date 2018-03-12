/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sun.swing.plaf.synth;

import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.plaf.synth.*;

/**
 * SynthUI is used to fetch the SynthContext for a particular Component.
 *
 * @version %I%, %G%
 * @author Scott Violet
 */
public interface SynthUI extends SynthConstants {
    /**
     * Returns the Context for the specified component.
     *
     * @param c Component requesting SynthContext.
     * @return SynthContext describing component.
     */
    public SynthContext getContext(JComponent c);

    /**
     * Paints the border.
     */
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h);
}
