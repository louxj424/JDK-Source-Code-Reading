/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#warn This file is preprocessed before being compiled

/*
 * This class contains a map which records the locale list string for
 * each resource in sun.util.resources & sun.text.resources.
 * It is used to avoid loading non-existent localized resources so that
 * jar files won't be opened unnecessary to look up them.
 *
 * @since 1.6
 */
package sun.util;

import java.util.HashMap;


public class LocaleDataMetaInfo {

    private static final HashMap<String, String> resourceNameToLocales = 
	new HashMap<String, String>(6);


    static {
	/* During JDK build time, #XXX_YYY# will be replaced by a string contain all the locales
	   supported by the resource. 

	   Don't remove the space character between " and #. That is put there purposely so that
	   look up locale string such as "en" could be based on if it contains " en ".
	*/
	resourceNameToLocales.put("sun.text.resources.FormatData",
				  " #FormatData_EuroLocales# | #FormatData_NonEuroLocales# ");

	resourceNameToLocales.put("sun.text.resources.CollationData",
				  " #CollationData_EuroLocales# | #CollationData_NonEuroLocales# ");

	resourceNameToLocales.put("sun.util.resources.TimeZoneNames",
				  " #TimeZoneNames_EuroLocales# | #TimeZoneNames_NonEuroLocales# ");

	resourceNameToLocales.put("sun.util.resources.LocaleNames",
				  " #LocaleNames_EuroLocales# | #LocaleNames_NonEuroLocales# ");

	resourceNameToLocales.put("sun.util.resources.CurrencyNames",
				  " #CurrencyNames_EuroLocales# | #CurrencyNames_NonEuroLocales# ");
	
	resourceNameToLocales.put("sun.util.resources.CalendarData",
				  " #CalendarData_EuroLocales# | #CalendarData_NonEuroLocales# ");
    }

    /* 
     * @param resourceName the resource name 
     * @return the supported locale string for the passed in resource.
     */
    public static String getSupportedLocaleString(String resourceName) {
	
	return resourceNameToLocales.get(resourceName);
    }

}
