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
package savant.selection;

import javax.swing.JLabel;
import savant.api.data.ContinuousRecord;


/**
 * Popup panel for displaying information about a ContinuousRecord.
 *
 * @author AndrewBrook
 */
public class ContinuousPopup extends PopupPanel {

    ContinuousPopup() {
    }

    @Override
    protected void initInfo() {
        ref = record.getReference();
        ContinuousRecord rec = (ContinuousRecord)record;
        start = rec.getPosition();
        end = rec.getPosition();
        name = "Value: " + rec.getValue();
        add(new JLabel("Position: " + start));
        add(new JLabel("Value: " + rec.getValue()));
    }
}
