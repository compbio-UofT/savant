/*
 *    Copyright 2010-2011 University of Toronto
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


package savant.selection;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JLabel;

import net.sf.samtools.SAMRecord;

import savant.controller.LocationController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.Track;


/**
 *
 * @author AndrewBrook
 */
public class IntervalBamPopup extends PopupPanel implements DataRetrievalListener {

    private BAMIntervalRecord rec;
    private SAMRecord samRec;
    private IntervalBamPopup instance;

    public IntervalBamPopup(BAMIntervalRecord rec){
        this.rec = rec;
    }

    @Override
    protected void calculateInfo() {
        samRec = rec.getSamRecord();
        name = samRec.getReadName();
        ref = samRec.getReferenceName();
        start = samRec.getAlignmentStart();
        end = samRec.getAlignmentEnd();
    }

    @Override
    protected void initInfo() {
        String readName = "Read Name: " + name;
        this.add(new JLabel(readName));

        String readPosition = "Position: " + start;
        this.add(new JLabel(readPosition));

        String readLength = "Read Length: " + samRec.getReadLength();
        this.add(new JLabel(readLength));

        String mq = "Mapping Quality: " + samRec.getMappingQuality();
        this.add(new JLabel(mq));

        String bq = "Base Quality: " + samRec.getBaseQualityString();
        this.add(new JLabel(bq));

        if (samRec.getReadPairedFlag()) {
            String matePosition = "Mate Position: " + homogenizeRef(samRec.getMateReferenceName()) + ": " + samRec.getMateAlignmentStart();
            this.add(new JLabel(matePosition));
        }
    }

    @Override
    protected void initSpecificButtons() {

        if(samRec.getReadPairedFlag() && !(samRec.getMateReferenceName().equals("*") || samRec.getMateAlignmentStart() == 0)){
            //jump to mate button
            JLabel mateJump = new JLabel("Jump to Mate");
            mateJump.setForeground(Color.BLUE);
            mateJump.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            this.add(mateJump);

            //jump to mate and select button
            JLabel mateJump1 = new JLabel("Select Pair");
            mateJump1.setForeground(Color.BLUE);
            mateJump1.setCursor(new Cursor(Cursor.HAND_CURSOR));
            mateJump1.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (Track t: gp.getTracks()) {
                        if (t.getDataFormat() == fileFormat){
                            t.getRenderer().forceAddToSelected(record);
                            break;
                        }
                    }
                    //SelectionController.getInstance().addSelection(gp.getTracks()[0].getName(), rec);
                    LocationController lc = LocationController.getInstance();
                    int offset = (int)Math.ceil(((float) lc.getRange().getLength())/2);
                    int start = samRec.getMateAlignmentStart()-offset;
                    int end = start + lc.getRange().getLength() - 1;
                    gp.getTracks()[0].addDataRetrievalListener(IntervalBamPopup.this);
                    lc.setLocation(homogenizeRef(samRec.getMateReferenceName()), new Range(start, end));
                }
            });
            this.add(mateJump1);
        }

        initIntervalJumps(rec);
    }

    @Override
    public void dataRetrievalStarted(DataRetrievalEvent evt) {}

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        for (Record rec: evt.getData()) {
            SAMRecord current = ((BAMIntervalRecord)rec).getSamRecord();
            if(MiscUtils.isMate(samRec, current)){
                for (Track t: gp.getTracks()) {
                    if (t.getDataFormat() == fileFormat){
                        t.getRenderer().forceAddToSelected(rec);
                        break;
                    }
                }
                break;
            }
        }       
        hidePopup();

        //remove listener (doing it immediately throws concurrent mod exception)
        instance = this;
        Runnable runnable = new RemoveDataThread();
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {}

    //remove DataRetrievalListener after "Select Pair".
    class RemoveDataThread implements Runnable {
        @Override
        public void run() {
            gp.getTracks()[0].removeDataRetrievalListener(instance);
        }
    }

}
