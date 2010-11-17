/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin.util;

import savant.controller.SelectionController;
import savant.controller.event.selection.SelectionChangedListener;

/**
 *
 * @author mfiume
 */
public class SelectionUtils {

    private static SelectionController sc = SelectionController.getInstance();


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
