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
package savant.api.adapter;

import savant.api.data.Record;
import savant.api.event.PopupEvent;
import savant.api.util.Listener;
import savant.selection.PopupPanel;


/**
 * Interface implemented by components which allow a popup to be displayed.  Currently
 * implemented by GraphPane, VariantMap, and LDPlot.
 *
 * This interface is intended for internal use only.
 *
 * @author tarkvara
 * @since 2.0.0
 */
public interface PopupHostingAdapter {
    
    /**
     * Add a listener which is notified when the track popup is about to be shown.
     *
     * @param l the listener to be added
     */
    public void addPopupListener(Listener<PopupEvent> l);
    
    /**
     * Remove a listener which has been added by <code>addPopupListener</code>
     *
     * @param l the listener to be removed
     */
    public void removePopupListener(Listener<PopupEvent> l);
    
    public void firePopupEvent(PopupPanel panel);

    /**
     * Invoked when the popup has been hidden so that the host can do any extra cleanup.
     */
    public void popupHidden();

    /**
     * Invoked when user chooses Select/Deselect from the popup menu.
     * @param rec the record which should be selected
     */
    public void recordSelected(Record rec);
}
