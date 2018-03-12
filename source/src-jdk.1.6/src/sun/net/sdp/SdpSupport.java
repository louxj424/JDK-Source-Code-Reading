/*
 * %W% %E%
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.net.sdp;

import java.io.IOException;
import java.security.AccessController;

/**
 * This class defines methods for creating SDP sockets or "converting" existing
 * file descriptors, referencing (unbound) TCP sockets, to SDP.
 */

public final class SdpSupport {
    private static final String os = AccessController
        .doPrivileged(new sun.security.action.GetPropertyAction("os.name"));
    private static final boolean isSupported = (os.equals("SunOS") || (os.equals("Linux")));

    private SdpSupport() { }

    /**
     * Creates a SDP socket, returning file descriptor referencing the socket.
     */
    public static int createSocket() throws IOException {
        if (!isSupported)
            throw new UnsupportedOperationException("SDP not supported on this platform");
        return create0();
    }

    /**
     * Converts an existing file descriptor, that references an unbound TCP socket,
     * to SDP.
     */
    public static void convertSocket(int fd) throws IOException {
        if (!isSupported)
            throw new UnsupportedOperationException("SDP not supported on this platform");
        convert0(fd);
    }

    private static native int create0() throws IOException;

    private static native void convert0(int fd) throws IOException;

    static {
        AccessController.doPrivileged(
            new sun.security.action.LoadLibraryAction("net"));
    } 
}
