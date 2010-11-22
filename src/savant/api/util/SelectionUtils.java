/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api.util;

import savant.api.adapter.ViewTrackAdapter;
import savant.controller.SelectionController;
import savant.controller.event.selection.SelectionChangedListener;
import savant.view.swing.ViewTrack;

/**
 * Utilities for data selection in Savant
 * @author mfiume
 */
public class SelectionUtils {

    private static SelectionController sc = SelectionController.getInstance();

    /**
     * 
     * @param t The track for which to add the selection
     * @param data The data point to select
     */
    public void addSelection(ViewTrackAdapter t, Comparable data) {
        sc.addSelection(((ViewTrack) t).getURI(), data);
    }

    /**
     * Subscribe a listener to be notified when the selection list changes
     * @param l The listener to subscribe
     */
    public static void addSelectionChangedListener(SelectionChangedListener l) {
        sc.addSelectionChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the selection list changes
     * @param l The listener to unsubscribe
     */
    public static void removeSelectionChangedListener(SelectionChangedListener l) {
        sc.removeSelectionChangedListener(l);
    }

}
