/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api;

import java.util.Set;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.controller.event.range.RangeChangeCompletedListener;
import savant.data.types.Genome;
import savant.util.Range;

/**
 * Utilities for navigating Savant
 * @author mfiume
 */
public class NavigationUtils {

    private static RangeController rc = RangeController.getInstance();
    private static ReferenceController refc = ReferenceController.getInstance();

    /**
     * Tell whether a genome has been loaded yet
     * @return Whether or not a genome has been loaded yet
     */
    public static boolean isGenomeLoaded() {
        return refc.isGenomeLoaded();
    }

    /**
     * Get the loaded genome.
     * @return The loaded genome
     */
    public static Genome getGenome() {
        return refc.getGenome();
    }

    /**
     * Set the genome
     * @param genome The genome to set
     */
    public static void setGenome(Genome genome) {
        refc.setGenome(genome);
    }

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
    public static Range getCurrentReferenceRange() {
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
    public static void navigateTo(Range r) {
        rc.setRange(r);
    }

    /**
     * Navigate to the specified range on the specified reference
     * @param reference The name of the reference that the range applies to
     * @param range The range to set as current
     */
    public static void navigateTo(String reference, Range range) {
        rc.setRange(reference, range);
    }

    /**
     * Get the current range
     * @return The currentViewableRange
     */
    public static Range getCurrentRange() {
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
}
