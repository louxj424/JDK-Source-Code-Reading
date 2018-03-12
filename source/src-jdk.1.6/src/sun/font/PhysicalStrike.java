/* %W% %G%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.font;

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;


public abstract class PhysicalStrike extends FontStrike {

    static final long INTMASK = 0xffffffffL;

    private PhysicalFont physicalFont;
    protected CharToGlyphMapper mapper;
    /* the ScalerContext is a native structure pre-filled with the
     * info needed to setup the scaler for this strike. Its immutable
     * so we set it up when the strike is created and free it when the
     * strike is disposed. There's then no need to pass the info down
     * separately to native on every call to the scaler.
     */
    protected long pScalerContext;

    /* Only one of these two arrays is non-null.
     * use the one that matches size of an address (32 or 64 bits)
     */
    protected long[] longGlyphImages;
    protected int[] intGlyphImages;

    /* Used by the TrueTypeFont subclass, which is the only client
     * of getGlyphPoint(). The field and method are here because
     * there is no TrueTypeFontStrike subclass.
     * This map is a cache of the positions of points on the outline
     * of a TrueType glyph. It is used by the OpenType layout engine
     * to perform mark positioning. Without this cache every position
     * request involves scaling and hinting the glyph outline potentially
     * over and over again.
     */
    Hashtable glyphPointMapCache;

    protected boolean getImageWithAdvance;
    protected static final int complexTX =
	AffineTransform.TYPE_FLIP |
	AffineTransform.TYPE_GENERAL_SCALE |
	AffineTransform.TYPE_GENERAL_ROTATION |
	AffineTransform.TYPE_GENERAL_TRANSFORM |
	AffineTransform.TYPE_QUADRANT_ROTATION;

    PhysicalStrike(PhysicalFont physicalFont, FontStrikeDesc desc) {
	this.physicalFont = physicalFont;
	this.desc = desc;
    }

    protected PhysicalStrike() {
    }
    /* A number of methods are delegated by the strike to the scaler
     * context which is a shared resource on a physical font.
     */

    public int getNumGlyphs() {
	return physicalFont.getNumGlyphs();
    }

    /* These 3 metrics methods below should be implemented to return
     * values in user space.
     */
    StrikeMetrics getFontMetrics() {
	if (strikeMetrics == null) {
	    strikeMetrics =
		physicalFont.getFontMetrics(pScalerContext);
	}
	return strikeMetrics;
    }

    float getCodePointAdvance(int cp) {
	return getGlyphAdvance(physicalFont.getMapper().charToGlyph(cp));
    }

   Point2D.Float getCharMetrics(char ch) {
	return getGlyphMetrics(physicalFont.getMapper().charToGlyph(ch));
    }

    int getSlot0GlyphImagePtrs(int[] glyphCodes, long[] images, int  len) {
	return 0;
    }

    /* Used by the OpenType engine for mark positioning.
     */
    Point2D.Float getGlyphPoint(int glyphCode, int ptNumber) {
        Point2D.Float gp = null;
        Integer ptKey = new Integer(glyphCode<<16|ptNumber);
	if (glyphPointMapCache == null) {
	    synchronized (this) {
		if (glyphPointMapCache == null) {
		    glyphPointMapCache = new Hashtable();
		}
	    }
	} else {
	    gp = (Point2D.Float)glyphPointMapCache.get(ptKey);
	}

	if (gp == null) {
	    gp = (physicalFont.getGlyphPoint(pScalerContext, glyphCode, ptNumber));
	    adjustPoint(gp);
	    glyphPointMapCache.put(ptKey, gp);
        }
	return gp;
    }

    protected void adjustPoint(Point2D.Float pt) {
    }
}
