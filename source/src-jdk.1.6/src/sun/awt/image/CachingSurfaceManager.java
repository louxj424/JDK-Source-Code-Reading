/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.image;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.security.AccessController;
import java.util.Collection;
import java.util.HashMap;
import sun.awt.DisplayChangedListener;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.BlitBg;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.security.action.GetPropertyAction;

/**
 * This SurfaceManager variant attempts to cache the default (software)
 * surface of a BufferedImage into one or more accelerated surfaces.  All
 * rendering to the image will use the software surface as the destination.
 * When copying from the image, however, the CachingSurfaceManager may elect
 * to use one of the surfaces residing in accelerated memory (e.g. in VRAM
 * on some platforms).  These accelerated copies can be much faster than
 * copies that rely on software loops.
 *
 * Any situation that causes accelerated surfaces to be lost will cause no
 * disruption; the software surface will be the default until an accelerated
 * surface can be automatically restored.
 */
public abstract class CachingSurfaceManager
    extends SurfaceManager
    implements RasterListener, DisplayChangedListener
{
    /**
     * A reference to the BufferedImage whose contents are being managed.
     */
    protected BufferedImage bImg;

    /**
     * The default (software) surface containing the contents of the
     * BufferedImage.
     */
    protected SurfaceData sdDefault;

    /**
     * The current accelerated surface, referenced in certain operations
     * such as copyDefaultToAccelerated() and validate().  This is a reference
     * to one of the objects in the accelSurfaces Hashtable.
     */
    protected SurfaceData sdAccel;

    /**
     * A collection of all accelerated surfaces.  Each surface is associated
     * with some key, typically the GraphicsDevice or GraphicsConfiguration
     * under which the surface was created.
     */
    protected HashMap<Object, SurfaceData> accelSurfaces;

    /**
     * This flag is used to track whether anyone has requested
     * direct access to the pixels, via one of the many get*() methods
     * of BufferedImage.  If so, then we punt on trying to use the
     * cached SurfaceData object because we cannot ensure that its
     * data matches that of the default (software) surface.
     */
    protected boolean localAccelerationEnabled = false;

    /**
     * If true, acceleration is globally enabled for all
     * CachingSurfaceManagers, which means they should attempt to cache a
     * copy of their default software surface in accelerated memory.
     * Platform-specific variants can disable this flag if accelerated
     * surfaces cannot be created (e.g. when DirectDraw is disabled).
     */
    protected static boolean accelerationEnabled = true;

    /**
     * Regulates whether we disable local acceleration on rasterStolen
     * events.  Normally, when the user gets a handle to the Raster of
     * a hidden-acceleration image, we punt on trying to accelerate it
     * because we cannot guarantee that our cached version will match
     * the software version.  Use of this flag can force us to
     * proceed with acceleration anyway.
     * This workaround was introduced for a particular customer 
     * situation where we could not provide a way for the 
     * user to set the bits in an image without disabling acceleration
     * for that image, so they had to have 2 copies of that data.  This
     * flag was used to allow that application to run without these
     * duplicate images to preserve memory usage.
     * Use of this flag means applications have to let us know when
     * the image data has changed.  Since there is no API for that
     * currently, they must use the workaround of forcing some write
     * to the image (such as a transparent image operation); this 
     * will trigger a dirty flag for the image and we will then 
     * copy the new data down to the cached version.
     * A real fix in the future would be to implement new API that
     * allows applications to lock and unlock an image for direct
     * pixel manipulation.
     */
    protected static boolean allowRasterSteal = false;

    /**
     * Value that determines how many copies we will do from the system
     * memory version of the image before we attempt to cache and
     * accelerate future copies.
     */
    protected static int accelerationThreshold = 1;

    static {
        String manimg = (String)AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.managedimages"));
        if (manimg != null && manimg.equals("false")) {
            accelerationEnabled = false;
            System.out.println("Disabling managed images");
        }

	String num = (String)AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.accthreshold"));
	if (num != null) {
	    try {
		int parsed = Integer.parseInt(num);
		if (parsed >= 0) {
		    accelerationThreshold = parsed;
		    System.out.println("New Acceleration Threshold: " +
                                       accelerationThreshold);
		}
	    } catch (NumberFormatException e) {
		System.err.println("Error setting new threshold:" + e);
	    }
	}

        String ras = (String)AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.allowrastersteal"));
	if (ras != null && ras.equals("true")) {
	    allowRasterSteal = true;
	    System.out.println("Raster steal allowed");
	}
    }

    public CachingSurfaceManager(BufferedImage bImg) {
        this.bImg = bImg;
        this.sdDefault = BufImgSurfaceData.createData(bImg);

        if (accelerationEnabled && bImg.getAccelerationPriority() > 0) {
            WritableRaster raster = bImg.getRaster();
            if (raster instanceof SunWritableRaster) {
                localAccelerationEnabled = true;
                ((SunWritableRaster)raster).setRasterListener(this);
                if (localAccelerationEnabled) {
                    // only create the hashtable if local acceleration is
                    // still enabled after the call to setRasterListener()
                    // The initial capacity of 2 should be enough for most cases.
		    accelSurfaces = new HashMap<Object, SurfaceData>(2);
                }
            }
        }

        GraphicsEnvironment ge = 
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        // We could have a HeadlessGE at this point, so double-check before
        // assuming anything.
        // Also, no point in listening to display change events if
        // the image is never going to be accelerated.
        if (accelerationEnabled && localAccelerationEnabled && 
            ge instanceof SunGraphicsEnvironment) 
        {
            ((SunGraphicsEnvironment)ge).addDisplayChangedListener(this);
        }
    }

    public SurfaceData getSourceSurfaceData(SurfaceData dstData,
                                            CompositeType comp,
                                            Color bgColor, 
                                            boolean scale)
    {
	// Only return the accelerated version of sdDefault if all of
	// the following are true:
        //   - acceleration for this specific image is enabled ("global"
        //     acceleration is assumed to be enabled if "local" acceleration
        //     is enabled)
	//   - we're not in the middle of a copyDefaultToAccelerated
	//     operation (destSD != sdAccel)
	//   - the destination SD is accelerated, so copies from
	//     sdAccel will be faster than copies from system memory
	//   - current operation (composite type + bg color + scaling op)
	//     is suitable for the accelerated surface which represents the
	//     image. See isOperationSupported() for the definition of 
	//     'suitable'
	//   - we have already done this copy before since the last 
	//     time the image was modified.
	if (localAccelerationEnabled                            &&
	    (dstData != sdAccel)                                &&
	    isDestSurfaceAccelerated(dstData)                   &&
	    isOperationSupported(dstData, comp, bgColor, scale) &&
	    (sdDefault.increaseNumCopies() > accelerationThreshold))
	{
	    // first, we validate the accelerated SurfaceData if necessary
            // and then return the appropriate SurfaceData object
	    validate(dstData.getDeviceConfiguration());
	    if (sdAccel != null && !sdAccel.isSurfaceLost()) {
		return sdAccel;
	    }
	}

	// fallback case; return the system-memory version
	return sdDefault;
    }

    public SurfaceData getDestSurfaceData() {
        return sdDefault;
    }

    /**
     * Abstract method returning a boolean value indicating whether the
     * destination surface passed in is accelerated.  This is implemented
     * in platform-specific classes to return values based on whether 
     * copies to the specific type of destination surface can be
     * accelerated.
     */
    protected abstract boolean isDestSurfaceAccelerated(SurfaceData destSD);

    /**
     * Returns true if there is an accelerated SurfaceData for the
     * given GraphicsConfiguration
     */
    protected boolean isValidAccelSurface(GraphicsConfiguration gc) {
	return (getAccelSurface(gc) != null);
    }

    /**
     * Returns accelerated SurfaceData compatible with the given
     * GraphicsConfiguration.
     */
    protected SurfaceData getAccelSurface(GraphicsConfiguration gc) {
	return accelSurfaces != null ? accelSurfaces.get(gc) : null;
    }

    /**
     * This method checks if the operation described by the given parameters
     * is "compatible" with the current accelerated surface.  Returns true
     * if the requested operation is likely to be accelerated given the
     * current state of cached surfaces.
     *
     * Sometimes it doesn't make sense to use an accelerated surface
     * when there is no special loop which could benefit from the surface
     * being accelerated (e.g. in case of alpha blending operations).
     * Platform-specific implementations can use their knowledge of the
     * underlying graphics system to return the most appropriate value here.
     */
    protected abstract boolean isOperationSupported(SurfaceData dstData,
                                                    CompositeType comp,
                                                    Color bgColor,
                                                    boolean scale);

    /**
     * Returns a new accelerated surface that is compatible with the
     * given GraphicsConfiguration, or null if the creation was unsuccessful.
     */
    protected abstract SurfaceData createAccelSurface(GraphicsConfiguration gc,
						      int width, int height);

    /**
     * Initializes an new accelerated surface that is compatible with the
     * given GraphicsConfiguration, using either previously created and
     * cached surface from accelSurfaces collection for the particular
     * GraphicsConfiguration, or by creating a new one with
     * createAccelSurface. If the surface creation is successful,
     * the surface is stored in the accelSurfaces collection and
     * referenced by sdAccel.
     */
    protected void initAcceleratedSurface(GraphicsConfiguration gc,
                                          int width, int height)
    {
	try {
	    sdAccel = getAccelSurface(gc);
	    if (sdAccel == null) {
		sdAccel = createAccelSurface(gc, width, height);
		if (sdAccel != null) {
                    accelSurfaces.put(gc, sdAccel);
                }
	    }
	} catch (NullPointerException ex) {
	    sdAccel = null;
	} catch (OutOfMemoryError er) {
	    sdAccel = null;
	} catch (InvalidPipeException ex) {
            sdAccel = null;
	}
    }

    /**
     * Return the Color object used to replace any transparent pixels in
     * the backup BufferedImage when copying to the hardware surface.
     */
    protected Color getTransparentPixelColor() {
        return null;
    }

    /**
     * This method copies the contents of the backup surface onto the
     * hardware surface.  This call could be made either when the user
     * updated the contents since our last copy from the surface, or when
     * there has been a surface loss occurrence.
     *
     * REMIND: There are some subtleties here in how we select the appropriate
     * blit loops.  The accelerated bitmask transparent surfaces are derived
     * from type Custom.  So there are no optimized BlitBg loops (or any
     * loops) from any software surface to them.  But we know that the
     * accelerated surface is actually in one of the corresponding software
     * surface types for which we do have optimized software loops, so we
     * can them to copy from the software to the accelerated surface.
     *
     * To achieve that, we have these 'Delegate loops', which are
     * registered as BlitBg loops from Any->SrcNoEA->*_BM surfaces. Their
     * job is to delegate the actual work to a software loop which can render
     * to the destination surface (see X11PMBlitBgLoops.java and
     * Win32BlitLoops.java).
     *
     * Here's an example.
     *     sdDefault - IntArgbBm
     *     sdAccel   - IntRgbDD_BM (derived from Custom)
     *
     * We need to do a BlitBg from sdDefault to sdAccel with transparent
     * pixel as the background color.  There's a delegate loop registered: 
     *     DelegateBlitBgLoop(Any, SrcNoEa, IntRgbDD_BM)
     * which actually delegates the work to a generic software BlitBg loop:
     *     BlitBg(IntArgbBm, SrcOverNoEa, IntRgb)
     * which is still better than doing a custom blit from Any to Custom.
     *
     * We should really clean up our blit loop registration on all platforms,
     * so that the logic here is not so complex.
     */
    protected void copyDefaultToAccelerated() {
	if (accelSurfaces != null) {
	    Collection<SurfaceData> elems = accelSurfaces.values(); 
	    for (SurfaceData sdata : elems) {
		try {
		    if (sdata != sdDefault && !sdata.isSurfaceLost()) {
			SurfaceType srcType = sdDefault.getSurfaceType();
			SurfaceType dstType = sdata.getSurfaceType();
			Color bgcolor = getTransparentPixelColor();
			if (bgcolor == null) {
			    Blit blit = Blit.getFromCache(srcType,
							  CompositeType.SrcNoEa,
							  dstType);
			    blit.Blit(sdDefault, sdata,
				      AlphaComposite.Src, null,
				      0, 0, 0, 0,
				      bImg.getWidth(), bImg.getHeight());
			} else {
				// we know that in this case the image
				// is transparent, so we use SrcNoEa
			    BlitBg blit = BlitBg.getFromCache(srcType,
							      CompositeType.SrcNoEa,
							      dstType);
			    blit.BlitBg(sdDefault, sdata,
					AlphaComposite.SrcOver, null,
                                        bgcolor.getRGB(),
					0, 0, 0, 0,
					bImg.getWidth(), bImg.getHeight());
			}
		    }
		} catch (Exception e) {
		    // Catch the exception so as to not propagate it.  We will
		    // just continue to use the default SurfaceData
		    if (sdata != null) {
			sdata.setSurfaceLost(true);
		    }
		}
	    }
	    sdDefault.setNeedsBackup(false);
	}
    }

    /**
     * Get the hardware surface ready for rendering.  This method is
     * called internally whenever we want to make sure that the surface
     * exists in a usable state.
     *
     * The surface may not be "ready" if either we had problems creating
     * it in the first place (e.g., there was no space in vram) or if
     * the surface was lost (e.g., due to a display change or other
     * surface-loss situation).
     */
    public void validate(GraphicsConfiguration gc) {
	if (localAccelerationEnabled) {
            boolean accelSurfaceRestored = false;
	    
	    sdAccel = getAccelSurface(gc);
	    if (sdAccel == null) {
		// must have lost the surface or had problems creating it
		initAcceleratedSurface(gc, bImg.getWidth(), bImg.getHeight());
		// sdAccel is set in initAccSurf; use that value here
		if (sdAccel != null) {
                    accelSurfaceRestored = true;
		} else {
		    return;
		}
	    } else if (sdAccel.isSurfaceLost()) {
		try {
		    restoreAcceleratedSurface();
                    accelSurfaceRestored = true;
		    sdAccel.setSurfaceLost(false);
		} catch (InvalidPipeException e) {
		    /**
		     * When we fail restoration, we should force the accelerated
		     * surface(s) to be recreated.  Sometimes failure indicates
		     * that we are using a surface in an incompatible display
		     * mode; restoration will continue to fail for as long
		     * as we remain in this display mode.  Better to re-create
		     * the surface from scratch.
		     */
		    flush();
		    return;
		}
	    }

	    if (sdDefault.needsBackup() || accelSurfaceRestored) {
                // update the accelerated surface only if:
                //   - sdDefault has been modified, or
                //   - sdAccel has been restored/recreated
		copyDefaultToAccelerated();
	    }
	}
    }

    /**
     * This method is called by the raster that we registered ourselves with
     * to notify us that the underlying raster for this image has been
     * modified.  Upon receiving this notification, we mark the default
     * surface as needing backup.
     */
    public void rasterChanged() {
        sdDefault.setNeedsBackup(true);
    }

    /**
     * This method is called by the raster that we registered ourselves with
     * to notify us that the underlying raster for this image has been
     * taken by a third party (e.g. through a call to Raster.getDataBuffer()).
     * Upon receiving this notification, we disable acceleration for this
     * image since we no longer have control over the raster.
     */
    public void rasterStolen() {
	if (!allowRasterSteal) {
	    localAccelerationEnabled = false;
	}
    }

    /**
     * Called from platform-specific SurfaceData objects to attempt to 
     * auto-restore the contents of an accelerated surface that has been lost.
     */
    public SurfaceData restoreContents() {
	return sdDefault;
    }

    /**
     * Restore sdAccel in case it was lost.  Do nothing in this
     * default case; platform-specific implementations may do more in
     * this situation as appropriate.
     */
    protected void restoreAcceleratedSurface() {
    }

    /**
     * Called from Toolkit when there has been a display mode change.
     * Note that we simply invalidate hardware surfaces here; we do not
     * attempt to recreate or re-render them.  This is to avoid threading
     * conflicts with the native toolkit and associated threads.  Instead,
     * we just nullify the old surface data object and wait for a future
     * method in the rendering process to recreate the surface and
     * copy the backup if appropriate.
     *
     * REMIND: probably need to re-verify that we can support
     *         the image in this new display mode
     */
    public void displayChanged() {
        if (!accelerationEnabled) {
            return;
        }
        // REMIND: we might want not to invalidate D3D or OGL surfaces,
        // since we are using managed surfaces, and they should survive
        // the display change, but for now there is no harm in doing so
        invalidateAcceleratedSurfaces();
    }

    /**
     * When device palette changes, need to force a new copy
     * of the image into our hardware cache to update the 
     * color indices of the pixels (indexed mode only).
     */
    public void paletteChanged() {
        sdDefault.setNeedsBackup(true);
    }

    /**
     * Disable acceleration for this image if the acceleration
     * priority was set to 0.
     */
    public void setAccelerationPriority(float priority) {
	if (priority == 0.0f) {
            setLocalAccelerationEnabled(false);
	}
    }
    
    public boolean isLocalAccelerationEnabled() {
        return localAccelerationEnabled;
    }
    
    public void setLocalAccelerationEnabled(boolean enabled) {
        localAccelerationEnabled = enabled;
        if (!localAccelerationEnabled) {
            flush();
        } else if (accelSurfaces == null) {
            // if we are re-enabling acceleration make sure accelSurfaces
            // is created
            accelSurfaces = new HashMap<Object, SurfaceData>(2);
	}
    }

    /**
     * Resets the stolen property of the raster of this image, and restores
     * local acceleration capability of this image unless it was disabled
     * by the user via Image.setAccelerationPriority(0.0f).
     *
     * Warning: this method should only be used on images under the control of
     * the implementation (like those in ImageRepresentation) or we created
     * ourselves (like in Robot.createScreenCapture()).
     *
     * @param image image to restore local acceleration
     */
    public static void restoreLocalAcceleration(Image image) {
        // this alone will not restore the local acceleration but it is
        // important to do this first so that if the image doesn't yet have
        // a SurfaceManager it will be created below for the image with the
        // non-stolen raster
        if (image instanceof BufferedImage) {
            WritableRaster raster = ((BufferedImage)image).getRaster();
            if (raster instanceof SunWritableRaster) {
                ((SunWritableRaster)raster).setStolen(false);
            }
        }
        SurfaceManager sm = SurfaceManager.getManager(image);
        if (image.getAccelerationPriority() > 0.0f &&
            sm instanceof CachingSurfaceManager)
        {
            ((CachingSurfaceManager)sm).setLocalAccelerationEnabled(true);
        }
    }

    /**
     * Returns an ImageCapabilities object which can be
     * inquired as to the specific capabilities of this
     * Image.  The capabilities object will return true for
     * isAccelerated() if the image is accelerated on the given
     * GraphicsConfiguration parameter.
     * A null GraphicsConfiguration returns a value based on whether the
     * image is currently accelerated on its default GraphicsConfiguration.
     * @see java.awt.Image#getCapabilities
     * @since 1.5
     */
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
	return new ImageCapabilitiesGc(gc);
    }
    
    class ImageCapabilitiesGc extends ImageCapabilities {
	GraphicsConfiguration gc;
	
	public ImageCapabilitiesGc(GraphicsConfiguration gc) {
	    super(false);
	    this.gc = gc;
	}
	
	public boolean isAccelerated() {
	    GraphicsConfiguration tmpGc = gc;
	    if (tmpGc == null) {
		tmpGc = GraphicsEnvironment.getLocalGraphicsEnvironment().
		    getDefaultScreenDevice().getDefaultConfiguration();
	    }
	    return (isValidAccelSurface(tmpGc));
	}
    }

    public synchronized void flush() {
	sdAccel = null;
	if (accelSurfaces != null) {
	    HashMap oldAccelSurfaces = accelSurfaces;
	    accelSurfaces = localAccelerationEnabled ? 
		new HashMap<Object, SurfaceData>(2) : null;
	    Collection<SurfaceData> elems = oldAccelSurfaces.values(); 
	    for (SurfaceData sdata : elems) {
		sdata.flush();
	    }
	}
    }

    public synchronized void invalidateAcceleratedSurfaces() {
	sdAccel = null;
	if (accelSurfaces != null) {
	    HashMap oldAccelSurfaces = accelSurfaces;
	    accelSurfaces = localAccelerationEnabled ? 
		new HashMap<Object, SurfaceData>(2) : null;
	    Collection<SurfaceData> elems = oldAccelSurfaces.values(); 
	    for (SurfaceData sdata : elems) {
		sdata.invalidate();
	    }
	}
    }
}
