/*
 *    Copyright 2012 University of Toronto
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
