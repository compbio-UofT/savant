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
