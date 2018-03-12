/*
 * %W% %E%
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.nio.ch;

import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * Provides access to implementation private constructors and methods.
 */

public final class Secrets {
    private Secrets() { }

    public static SocketChannel newSocketChannel(int fdVal) {
        return new SocketChannelImpl(SelectorProvider.provider(), fdVal);
    }

    public static ServerSocketChannel newServerSocketChannel(int fdVal) {
        return new ServerSocketChannelImpl(SelectorProvider.provider(), fdVal);
    }
}

