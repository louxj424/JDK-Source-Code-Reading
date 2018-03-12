/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.tools.native2ascii.resources;

import java.util.ListResourceBundle;

public class MsgNative2ascii_zh_CN extends ListResourceBundle {

    public Object[][] getContents() {
        return new Object[][] {
        {"err.bad.arg", "-encoding \u9700\u8981\u53c2\u6570"},
        {"err.cannot.read",  "\u65e0\u6cd5\u8bfb\u53d6 {0}\u3002"},
        {"err.cannot.write", "\u65e0\u6cd5\u5199\u5165 {0}\u3002"},
        {"usage", "\u7528\u6cd5\uff1anative2ascii" +
         " [-reverse] [-encoding \u7f16\u7801] [\u8f93\u5165\u6587\u4ef6 [\u8f93\u51fa\u6587\u4ef6]]"},
        };
    }
}
