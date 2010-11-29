/*
 *    Copyright 2010 University of Toronto
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

import savant.api.adapter.ViewTrackAdapter;
import savant.controller.SelectionController;
import savant.controller.event.SelectionChangedListener;
import savant.data.types.Record;

/**
 * Utilities for data selection in Savant
 *
 * @author mfiume
 */
public class SelectionUtils {

    private static SelectionController sc = SelectionController.getInstance();

    /**
     * 
     * @param t The track for which to add the selection
     * @param data The data point to select
     */
    public static void addSelection(ViewTrackAdapter t, Record data) {
        sc.addSelection(t.getURI(), data);
    }

    /**
     *
     * @param t The track for which to toggle the selection
     * @param data The data point to select/deselect
     */
    public static void toggleSelection(ViewTrackAdapter t, Record data) {
        sc.toggleSelection(t.getURI(), data);
    }

    /**
     * Subscribe a listener to be notified when the selection changes.
     *
     * @param l The listener to subscribe
     */
    public static void addSelectionChangedListener(SelectionChangedListener l) {
        sc.addSelectionChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the selection changes.
     *
     * @param l The listener to unsubscribe
     */
    public static void removeSelectionChangedListener(SelectionChangedListener l) {
        sc.removeSelectionChangedListener(l);
    }
}
