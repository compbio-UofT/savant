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
package savant.view.variation.swing;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import savant.api.adapter.PopupHostingAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.api.event.PopupEvent;
import savant.api.util.Listener;
import savant.selection.PopupPanel;
import savant.util.AggregateRecord;
import savant.util.Hoverer;
import savant.view.tracks.VariantTrack;
import savant.view.variation.LDRecord;
import savant.view.variation.ParticipantRecord;
import savant.view.variation.VariationController;


/**
 * Hoverer which monitors mouse position for hovering and displays a popup panel when appropriate.
 *
 * @author tarkvara
 */
public class VariantPopper extends Hoverer implements PopupHostingAdapter {
    VariationPlot panel;

    VariantPopper(VariationPlot vp) {
        panel = vp;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        Record rec = panel.pointToRecord(hoverPos);
        if (rec != null) {
            PopupPanel.hidePopup();
            Point globalPos = new Point(hoverPos.x, hoverPos.y);
            SwingUtilities.convertPointToScreen(globalPos, panel);
            if (rec instanceof ParticipantRecord) {
                VariantType[] partVars = ((ParticipantRecord)rec).getVariants();

                // Only display a popup if this participant actually has variation here.
                if (partVars != null && partVars[0] != VariantType.NONE || (partVars.length > 1 && partVars[1] != VariantType.NONE)) {
                    PopupPanel.showPopup(this, globalPos, VariationController.getInstance().getTracks()[0], rec);
                }
            } else {
                PopupPanel.showPopup(this, globalPos, VariationController.getInstance().getTracks()[0], rec);
            }
        }
        hoverPos = null;
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        VariationController.getInstance().updateStatusBar(panel.pointToVariantRecord(evt.getPoint()));
        Point oldHover = hoverPos;
        super.mouseMoved(evt);
        if (oldHover != null && !isHoverable(oldHover)) {
            PopupPanel.hidePopup();
        }
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        VariationController.getInstance().navigateToRecord(panel.pointToVariantRecord(evt.getPoint()));
    }

    @Override
    public void addPopupListener(Listener<PopupEvent> l) {
    }

    @Override
    public void removePopupListener(Listener<PopupEvent> l) {
    }

    @Override
    public void firePopupEvent(PopupPanel panel) {
    }

    @Override
    public void popupHidden() {
    }

    /**
     * Invoked when user chooses Select/Deselect from the popup menu.
     * @param rec the Participant record which should be selected
     */
    @Override
    public void recordSelected(Record rec) {
        Record varRec = rec instanceof ParticipantRecord ? ((ParticipantRecord)rec).getVariantRecord() : rec;
        List<VariantRecord> selection = new ArrayList<VariantRecord>();
        if (varRec instanceof LDRecord) {
            // When selecting on the LD plot, we want to select both the subrecords.
            selection.addAll(((AggregateRecord<VariantRecord>)varRec).getConstituents());
        } else if (varRec instanceof AggregateRecord) {
            // While the aggregate may contain several records, they will all correspond to the same position, so
            // the record comparator will consider them equivalent.
            selection.add(((AggregateRecord<VariantRecord>)varRec).getConstituents().get(0));
        } else {
            selection.add((VariantRecord)varRec);
        }
        for (VariantTrack t: VariationController.getInstance().getTracks()) {
            for (VariantRecord selRec: selection) {
                t.getRenderer().addToSelected(selRec);
            }
            t.repaintSelection();
        }
        VariationController.getInstance().navigateToRecord(varRec);
    }
}
