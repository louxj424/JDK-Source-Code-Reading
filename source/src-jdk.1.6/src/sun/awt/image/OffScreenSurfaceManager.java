/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.image;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.peer.ComponentPeer;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;

/**
 * This SurfaceManager variant does not have a system-memory-based
 * backup surface. Its default surface is accelerated, so all 
 * rendering to the image will go to that surface.
 * 
 * Since there's no backup surface, this manager can only be
 * used when a surface loss is either impossible, or is somehow
 * managed otherwise.
 */
public abstract class OffScreenSurfaceManager
    extends CachingSurfaceManager
{
    /**
     * Reference to an intermediate BufImgSD for use in copyDefaultToAccelerated.
     */
    private WeakReference<SurfaceData> bisdRef;

    public OffScreenSurfaceManager(Component c, BufferedImage bi) {
	super(bi);

	if (!accelerationEnabled) {
	    return;
	}

	GraphicsConfiguration gc;
	ComponentPeer peer = null;
	if (c != null) {
	    peer = c.getPeer();
	}

	if (peer != null) {
	    gc = peer.getGraphicsConfiguration();
	} else {
	    GraphicsEnvironment env =
		GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gd = env.getDefaultScreenDevice();
	    gc = gd.getDefaultConfiguration();
	}

	initAcceleratedSurface(gc, bImg.getWidth(), bImg.getHeight());
	if (sdAccel != null) {
	    // our default surface is the accelerated surface
	    sdDefault = sdAccel;
	}
    }

    /**
     * Need to override this method as we don't need to check for
     * the number of copies done from this image
     */
    @Override
    public SurfaceData getSourceSurfaceData(SurfaceData destSD,
					    CompositeType comp,
					    Color bgColor,
					    boolean scale)
    {
	if (accelerationEnabled &&
	    destSD != sdAccel &&
	    isDestSurfaceAccelerated(destSD))
        {
	    // First, we validate the surface if necessary and then
	    // return the appropriate surfaceData object.
	    validate(destSD.getDeviceConfiguration());
	    if (sdAccel != null) {
		return sdAccel;
	    }
	}
	return sdDefault;
    }

    /**
     * This method updates the non-default accelerated surfaces
     * with the contents of the default one using intermediate
     * buffered image surface, because it's often not possible
     * to copy directly between accelerated surfaces 
     * associated with different GraphicsConfigurations or
     * GraphicsDevices.
     */
    @Override
    protected synchronized void copyDefaultToAccelerated() {
	if (accelSurfaces != null) {
	    boolean backupStillNeeded = false;
	    Collection<SurfaceData> elems = accelSurfaces.values(); 
	    SurfaceData bisd = 
		bisdRef == null ? null : bisdRef.get();
	    for (SurfaceData sdata : elems) {
		if (sdDefault != null && sdDefault != sdata) {
		    try {
			if (bisd == null) {
			    BufferedImage bi = createTempImage();
			    bisd = BufImgSurfaceData.createData(bi);
			    bisdRef = new WeakReference(bisd);
			}

			SurfaceType srcType = sdDefault.getSurfaceType();
			SurfaceType biType = bisd.getSurfaceType();
			SurfaceType dstType = sdata.getSurfaceType();

			Blit blit = Blit.getFromCache(srcType,
						      CompositeType.SrcNoEa,
						      biType);
			blit.Blit(sdDefault, bisd,
				  AlphaComposite.Src, null,
				  0, 0, 0, 0,
				  bImg.getWidth(), bImg.getHeight());

			blit = Blit.getFromCache(biType,
						 CompositeType.SrcNoEa,
						 dstType);
			blit.Blit(bisd, sdata,
				  AlphaComposite.Src, null,
				  0, 0, 0, 0,
				  bImg.getWidth(), bImg.getHeight());
		    } catch (Exception e) {
			// Catch the exception so as to not propagate it.
			// We will just continue to use the default SurfaceData.
			if (sdata != null) {
			    sdata.setSurfaceLost(true);
			}
			backupStillNeeded = true;
		    }
		}
	    }
	    // clear backup needed flag only if all surfaces were updated 
	    // successfully
	    if (!backupStillNeeded) {
		sdDefault.setNeedsBackup(false);
	    }
	}
    }

    @Override
    protected boolean isOperationSupported(SurfaceData dstData,
					   CompositeType comp, 
					   Color bgColor, boolean scale)
    {
	// we don't have much choice, we only have accelerated surface.
	return true;
    }

    /**
     * Flush all but the default surface. Add the latter to the list of 
     * accelerated surfaces.
     */
    @Override
    public synchronized void flush() {
	sdAccel = null;
	if (accelSurfaces != null) {
	    HashMap oldAccelSurfaces = accelSurfaces;
	    accelSurfaces = new HashMap<Object, SurfaceData>(2);
	    accelSurfaces.put(sdDefault.getDeviceConfiguration(), 
			      sdDefault);

	    Collection<SurfaceData> elems = oldAccelSurfaces.values(); 
	    for (SurfaceData sdata : elems) {
		if (sdata != sdDefault) {
		    sdata.flush();
		}
	    }
	}
    }

    /**
     * Invalidate all accelerated surfaces.  The only difference from
     * the superclass implementation is that this method does not invalidate
     * the default surface.  This is important because sdDefault==sdAccel
     * in the RemoteOffScreenImage case, so we do not want to invalidate
     * the default surface, e.g. when called after a display change.
     * @see #flush
     */
    @Override
    public synchronized void invalidateAcceleratedSurfaces() {
        sdAccel = null;
        if (accelSurfaces != null) {
            HashMap oldAccelSurfaces = accelSurfaces;
            accelSurfaces = new HashMap<Object, SurfaceData>(2);
            /*
             * REMIND: The default surface was originally part of
             * accelSurfaces, so we need to put it back in there.  Note
             * however that we use the default surface's original
             * GraphicsConfig, which may no longer be valid after a
             * display change.  This means we may hold the reference to
             * that GraphicsConfig longer than we would like to, and we
             * may cache the default surface into another accelerated
             * surface after each successive display change event.  These
             * consequences should cause no harm, but we may want to revisit
             * this eventually.
             */
            accelSurfaces.put(sdDefault.getDeviceConfiguration(), 
                              sdDefault);

            Collection<SurfaceData> elems = oldAccelSurfaces.values(); 
            for (SurfaceData sdata : elems) {
                if (sdata != sdDefault) {
                    sdata.invalidate();
                }
            }
        }
    }

    protected BufferedImage createTempImage() {
	ColorModel cm = bImg.getColorModel();
	WritableRaster wr = 
	    cm.createCompatibleWritableRaster(bImg.getWidth(), 
					      bImg.getHeight());
	return new BufferedImage(cm, wr,
				 cm.isAlphaPremultiplied(), null);
    }
}
