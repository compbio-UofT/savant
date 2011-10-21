/*
 *    Copyright 2010-2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package savant.api.util;

import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.api.event.LocationChangedEvent;
import savant.controller.LocationController;
import savant.util.MiscUtils;
import savant.util.Range;


/**
 * Utilities for navigating Savant.
 *
 * @author mfiume
 */
public class NavigationUtils {

    private static LocationController lc = LocationController.getInstance();

    /**
     * Get the name of the current reference.
     *
     * @return name of the current reference
     */
    public static String getCurrentReferenceName() {
        return lc.getReferenceName();
    }

    /**
     * Get a list of reference names for this genome (equivalent to <code>GenomeUtils.getGenome().getReferenceNames()</code>).
     */
    public static Set<String> getReferenceNames() {
        return lc.getReferenceNames();
    }

    /**
     * Get the current reference's range.
     *
     * @return the maximum viewable range
     */
    public static RangeAdapter getCurrentReferenceRange() {
        return lc.getMaxRange();
    }

    /**
     * Navigate to the start of the specified reference.
     *
     * @param ref the name of the reference to navigate to (e.g. "chrX")
     */
    public void navigateTo(String ref) {
        lc.setLocation(ref);
    }

    /**
     * Navigate to the specified range.
     *
     * @param range the range to set as current
     */
    public static void navigateTo(RangeAdapter range) {
        lc.setLocation((Range)range);
    }

    /**
     * Navigate to the specified range on the specified reference.
     *
     * @param ref the reference that the range applies to
     * @param range the range to set as current
     */
    public static void navigateTo(String ref, RangeAdapter range) {
        lc.setLocation(homogeniseRef(ref), (Range)range);
    }

    /**
     * Get the current range.
     *
     * @return the current viewable range
     */
    public static RangeAdapter getCurrentRange() {
        return lc.getRange();
    }

    /**
     * Subscribe a listener to be notified when the range changes.
     *
     * @param l the listener to subscribe
     */
    public static synchronized void addLocationChangedListener(Listener<LocationChangedEvent> l) {
        lc.addListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the range changes.
     *
     * @param l the listener to unsubscribe
     */
    public static synchronized void removeLocationChangedListener(Listener<LocationChangedEvent> l) {
        lc.removeListener(l);
    }

    /**
     * Given a string like "1" or "chr1", return it in a form which corresponds to whatever is being
     * used by the current genome.
     * @param orig the string to be homogenised
     * @return <code>orig</code> or an adjusted version thereof
     * @since 1.6.1
     */
    public static String homogeniseRef(String orig) {
        if (!lc.getAllReferenceNames().contains(orig)) {
            for (String ref: lc.getAllReferenceNames()) {
                if (MiscUtils.homogenizeSequence(ref).equals(orig)){
                    return ref;
                }
            }
            // Not good.
        }
        return orig;
    }
}
