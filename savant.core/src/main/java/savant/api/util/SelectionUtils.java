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

import savant.api.adapter.TrackAdapter;
import savant.api.event.SelectionChangedEvent;
import savant.api.data.Record;
import savant.selection.SelectionController;

/**
 * Utilities for data selection in Savant.
 *
 * @author mfiume
 */
public class SelectionUtils {

    private static SelectionController sc = SelectionController.getInstance();

    /**
     * Add a selection to the given track.  Equivalent to selecting the given data point in the user interface
     * (at which point it would normally turn green).
     *
     * @param t the track for which to add the selection
     * @param data the data point to select
     */
    public static void addSelection(TrackAdapter t, Record data) {
        sc.addSelection(t.getName(), data);
    }

    /**
     * Toggle the selection state of the given data point.
     *
     * @param t the track for which to toggle the selection
     * @param data the data point to select/deselect
     */
    public static void toggleSelection(TrackAdapter t, Record data) {
        sc.toggleSelection(t.getName(), data);
    }

    /**
     * Subscribe a listener to be notified when the selection changes.
     *
     * @param l the listener to subscribe
     */
    public static void addSelectionChangedListener(Listener<SelectionChangedEvent> l) {
        sc.addListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the selection changes.
     *
     * @param l the listener to unsubscribe
     */
    public static void removeSelectionChangedListener(Listener<SelectionChangedEvent> l) {
        sc.removeListener(l);
    }
}
