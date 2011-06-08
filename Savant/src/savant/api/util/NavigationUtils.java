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
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.controller.event.RangeChangeCompletedListener;
import savant.util.Range;


/**
 * Utilities for navigating Savant
 * @author mfiume
 */
public class NavigationUtils {

    private static RangeController rc = RangeController.getInstance();
    private static ReferenceController refc = ReferenceController.getInstance();

    /**
     * Get the name of the current reference
     * @return The name of the current reference
     */
    public static String getCurrentReferenceName() {
        return refc.getReferenceName();
    }

    /**
     * Get a list of reference names for this genome
     * @return
     */
    public static Set<String> getReferenceNames() {
        return refc.getReferenceNames();
    }

    /**
     * Get the current reference's range.
     * @return The maximumViewableRange
     */
    public static RangeAdapter getCurrentReferenceRange() {
        return rc.getMaxRange();
    }

    /**
     * Navigate to the start of the specified reference
     * @param ref The name of the reference to navigate to
     */
    public void navigateTo(String ref) {
        refc.setReference(ref);
    }

    /**
     * Navigate to the specified range
     * @param r The range to set as current
     */
    public static void navigateTo(RangeAdapter r) {
        rc.setRange((Range) r);
    }

    /**
     * Navigate to the specified range on the specified reference
     * @param reference The name of the reference that the range applies to
     * @param range The range to set as current
     */
    public static void navigateTo(String reference, RangeAdapter range) {
        rc.setRange(reference, (Range)range);
    }

    /**
     * Get the current range
     * @return The currentViewableRange
     */
    public static RangeAdapter getCurrentRange() {
        return rc.getRange();
    }

    /**
     * Subscribe a listener to be notified when the range changes
     * @param l The listener to subscribe
     */
    public static synchronized void addRangeChangeListener(RangeChangeCompletedListener l) {
        rc.addRangeChangeCompletedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the range changes
     * @param l The listener to unsubscribe
     */
    public static synchronized void removeRangeChangeListener(RangeChangeCompletedListener l) {
        rc.removeRangeChangeCompletedListener(l);
    }

    /**
     * Construct a new Range object representing the given range of bases.
     *
     * @param from start-point of the range
     * @param to end-point of the range
     * @return a newly-constructed RangeAdapter
     * @deprecated Use RangeUtils.createFange instead.
     */
    public static RangeAdapter createRange(int from, int to) {
        return new Range(from, to);
    }
}
