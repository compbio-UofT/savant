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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;

import net.sf.samtools.SAMRecord;

import savant.api.data.Record;
import savant.api.event.DataRetrievalEvent;
import savant.api.util.Listener;
import savant.api.util.RangeUtils;
import savant.controller.LocationController;
import savant.data.types.BAMIntervalRecord;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.tracks.Track;


/**
 *
 * @author AndrewBrook
 */
public class IntervalBamPopup extends PopupPanel implements Listener<DataRetrievalEvent> {

    private SAMRecord samRec;

    protected IntervalBamPopup(){
    }

    @Override
    protected void initInfo() {
        samRec = ((BAMIntervalRecord)record).getSAMRecord();
        name = samRec.getReadName();
        ref = samRec.getReferenceName();
        start = samRec.getAlignmentStart();
        end = samRec.getAlignmentEnd();

        add(new JLabel("Read Name: " + name));
        add(new JLabel("Position: " + start));
        add(new JLabel("Read Length: " + samRec.getReadLength()));
        add(new JLabel("Mapping Quality: " + samRec.getMappingQuality()));
        add(new JLabel("Base Quality: " + samRec.getBaseQualityString()));

        if (samRec.getReadPairedFlag()) {
            add(new JLabel("Mate Position: " + homogenizeRef(samRec.getMateReferenceName()) + ": " + samRec.getMateAlignmentStart()));
        }
    }

    @Override
    protected void initSpecificButtons() {

        if (samRec.getReadPairedFlag() && !(samRec.getMateReferenceName().equals("*") || samRec.getMateAlignmentStart() == 0)) {
            //jump to mate button
            Buttonoid mateJump = new Buttonoid("Jump to Mate");
            mateJump.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LocationController lc = LocationController.getInstance();
                    int offset = (int)Math.ceil(((float) lc.getRange().getLength())/2);
                    int start = samRec.getMateAlignmentStart()-offset;
                    int end = start + lc.getRange().getLength() - 1;
                    lc.setLocation(homogenizeRef(samRec.getMateReferenceName()), new Range(start, end));
                    hidePopup();
                }
            });
            add(mateJump);

            //jump to mate and select button
            Buttonoid pairSelect = new Buttonoid("Select Pair");
            if (samRec.getReferenceName().equals(samRec.getMateReferenceName())) {
                pairSelect.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        for (Track t: ((GraphPane)host).getTracks()) {
                            if (t.getDataFormat() == fileFormat){
                                t.getRenderer().forceAddToSelected(record);
                                break;
                            }
                        }
                        int start = Math.min(samRec.getAlignmentStart(), samRec.getMateAlignmentStart());
                        int end = Math.max(samRec.getAlignmentEnd(), samRec.getMateAlignmentStart() + samRec.getReadLength());
                        ((GraphPane)host).getTracks()[0].addListener(IntervalBamPopup.this);
                        LocationController.getInstance().setLocation((Range)RangeUtils.addMargin(new Range(start, end)));
                    }
                });
            } else {
                // Mate is in a different ref, so Select Pair unavailable.
                pairSelect.setEnabled(false);
                pairSelect.setToolTipText("Disabled because mate is in " + samRec.getMateReferenceName());
            }
            add(pairSelect);
        }

        initIntervalJumps();
    }

    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case COMPLETED:
                for (Record r: evt.getData()) {
                    SAMRecord current = ((BAMIntervalRecord)r).getSAMRecord();
                    if (MiscUtils.isMate(samRec, current, true)) {
                        for (Track t: ((GraphPane)host).getTracks()) {
                            if (t.getDataFormat() == fileFormat){
                                t.getRenderer().forceAddToSelected(r);
                                break;
                            }
                        }
                        break;
                    }
                }       
                hidePopup();
                ((GraphPane)host).getTracks()[0].removeListener(this);
                break;
        }
    }
}
