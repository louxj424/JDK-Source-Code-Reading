/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.image;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.image.BufferedImage;
import sun.java2d.SurfaceData;
import sun.java2d.SurfaceManagerFactory;
import sun.java2d.loops.CompositeType;

/**
 * The abstract base class that manages the various SurfaceData objects that
 * represent an Image's contents.  Subclasses can customize how the surfaces
 * are organized, whether to cache the original contents in an accelerated
 * surface, and so on.
 */
public abstract class SurfaceManager {

    public static abstract class ImageAccessor {
	public abstract SurfaceManager getSurfaceManager(Image img);
	public abstract void setSurfaceManager(Image img, SurfaceManager mgr);
    }

    private static ImageAccessor imgaccessor;

    public static void setImageAccessor(ImageAccessor ia) {
	if (imgaccessor != null) {
	    throw new InternalError("Attempt to set ImageAccessor twice");
	}
	imgaccessor = ia;
    }

    /**
     * Returns the SurfaceManager object contained within the given Image.
     */
    public static SurfaceManager getManager(Image img) {
	SurfaceManager sMgr = imgaccessor.getSurfaceManager(img);
	if (sMgr == null) {
	    /*
	     * All images should probably get a CachingSurfaceManager
	     * by default unless otherwise specified.
	     * In practice only a BufferedImage will get here.
	     */
	    try {
		BufferedImage bi = (BufferedImage) img;
		sMgr = SurfaceManagerFactory.createCachingManager(bi);
		setManager(bi, sMgr);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Invalid Image variant");
            }
	}
	return sMgr;
    }

    public static void setManager(Image img, SurfaceManager mgr) {
	imgaccessor.setSurfaceManager(img, mgr);
    }

    /**
     * Returns the best SurfaceData object to be used as the source surface
     * in an image copy operation.  The supplied parameters should describe
     * the type of operation being performed, so that an appropriate surface
     * can be used in the operation.  For example, if the destination surface
     * is "accelerated", this method should return a surface that is most
     * compatible with the destination surface.
     */
    public abstract SurfaceData getSourceSurfaceData(SurfaceData dstData,
                                                     CompositeType comp,
                                                     Color bgColor, 
                                                     boolean scale);

    /**
     * Returns the best SurfaceData object to be used as the destination
     * surface in a rendering operation.
     */
    public abstract SurfaceData getDestSurfaceData();

    /**
     * Restores the primary surface being managed, and then returns the
     * replacement surface.  This is called when an accelerated surface has
     * been "lost", in an attempt to auto-restore its contents.
     */
    public abstract SurfaceData restoreContents();

    /**
     * Notification that any accelerated surfaces associated with this manager
     * have been "lost", which might mean that they need to be manually
     * restored or recreated.
     * 
     * The default implementation does nothing, but platform-specific 
     * variants which have accelerated surfaces should perform any necessary
     * actions.
     */
    public void acceleratedSurfaceLost() {}

    /**
     * Returns an ImageCapabilities object which can be
     * inquired as to the specific capabilities of this
     * Image.  This default implementation returns an unaccelerated
     * ImageCapabilities object.  It is expected that subclasses which
     * accelerate their images will override this method.
     *
     * @see java.awt.Image#getCapabilities
     */
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        return new ImageCapabilities(false);
    }

    /**
     * Releases system resources in use by ancillary SurfaceData objects,
     * such as surfaces cached in accelerated memory.  For example, a
     * CachingSurfaceManager should release all of its cached surfaces,
     * but the base system memory surface will not be affected.
     *
     * The default implementation does nothing, but platform-
     * specific variants should free native surfaces, such as texture objects
     * being cached in VRAM.
     */
    public void flush() {
    }

    /**
     * Called when image's acceleration priority is changed.
     */
    public void setAccelerationPriority(float priority) {}
}
