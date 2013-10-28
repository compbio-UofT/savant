/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.api.event.LocationChangedEvent;
import savant.api.event.LocationChangeCompletedEvent;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.controller.event.GraphPaneEvent;
import savant.util.MiscUtils;
import savant.util.Range;


/**
 * Utilities for navigating Savant.
 *
 * @author mfiume
 */
public class NavigationUtils {

    private static LocationController lc = LocationController.getInstance();
    private static List<Listener<LocationChangeCompletedEvent>> completionListeners = new ArrayList<Listener<LocationChangeCompletedEvent>>();

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
        lc.setLocation(ref, false);
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
     * Subscribe a listener to be notified when the range has finished changing.
     *
     * @param l the listener to subscribe
     */
    public static synchronized void addLocationChangeCompletedListener(Listener<LocationChangeCompletedEvent> l) {
        completionListeners.add(l);
    }

    /**
     * Unsubscribe a listener from being notified when the range has finished changing.
     *
     * @param l the listener to unsubscribe
     */
    public static synchronized void removeLocationChangeCompletedListener(Listener<LocationChangeCompletedEvent> l) {
        completionListeners.remove(l);
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
    
    public static void fireLocationChangeCompletedEvent() {
        for (Listener l: completionListeners) {
            l.handleEvent(new LocationChangeCompletedEvent());
        }
    }
}
