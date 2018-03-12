/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.color;

import java.awt.color.ProfileDataException;

/**
 * An interface to allow the ProfileDeferralMgr to activate a
 * deferred profile.
 */
public interface ProfileActivator {

    /**
     * Activate a previously deferred ICC_Profile object.
     */
    public void activate() throws ProfileDataException;

}
