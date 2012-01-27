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
package savant.view.swing.variation;

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
import savant.data.types.ParticipantRecord;
import savant.selection.PopupPanel;
import savant.util.Hoverer;
import savant.view.tracks.Track;

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
            Point globalPt = SwingUtilities.convertPoint(panel, hoverPos, null);
            if (rec instanceof ParticipantRecord) {
                VariantType[] partVars = ((ParticipantRecord)rec).getVariants();

                // Only display a popup if this participant actually has variation here.
                if (partVars[0] != VariantType.NONE || (partVars.length > 1 && partVars[1] != VariantType.NONE)) {
                    PopupPanel.showPopup(this, globalPt, VariationController.getInstance().tracks.get(0), rec);
                }
            } else {
                PopupPanel.showPopup(this, globalPt, VariationController.getInstance().tracks.get(0), rec);
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
        VariantRecord varRec = rec instanceof ParticipantRecord ? ((ParticipantRecord)rec).getVariantRecord() : (VariantRecord)rec;
        List<VariantRecord> constituents = getConstituents(varRec);
        for (Track t: VariationController.getInstance().tracks) {
            for (VariantRecord rec2: constituents) {
                t.getRenderer().addToSelected(rec2);
            }
            t.repaintSelection();
        }
    }
    
    /**
     * When choosing Select/Deselect from the popup menu, we need to know which actual
     * VariantRecords were used to create this one.
     */
    private List<VariantRecord> getConstituents(VariantRecord rec) {
        List<VariantRecord> result = new ArrayList<VariantRecord>();
        if (rec instanceof MergedVariantRecord) {
            result.addAll(getConstituents(((MergedVariantRecord)rec).original1));
            result.addAll(getConstituents(((MergedVariantRecord)rec).original2));
        } else if (rec instanceof PaddedVariantRecord) {
            result.addAll(getConstituents(((PaddedVariantRecord)rec).original));
        } else {
            result.add(rec);
        }
        return result;
    }
}
