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
package savant.api.event;

import javax.swing.JPanel;

import savant.api.data.Record;
import savant.selection.PopupPanel;


/**
 * Event which is fired when a popup panel is opened over a track.  Plugins can use
 * this event to add their own widgets to the JPanel.
 *
 * @author Andrew
 */
public class PopupEvent {

    private final PopupPanel popup;

    /**
     * For internal use by Savant.
     */
    public PopupEvent(PopupPanel p) {
        popup = p;
    }

    /**
     * Panel which has been popped up by Savant.  As of Savant 2.0.0, this panel
     * has a 1-column GridLayout.
     *
     * @return the panel which is being popped up
     */
    public JPanel getPopup() {
        return popup;
    }

    /**
     * Get the record for which the popup is being popped up.
     * @return the record associated with this popup
     */
    public Record getRecord() {
        return popup.getRecord();
    }

}
