/*
 *    Copyright 2010 University of Toronto
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.sf.samtools.SAMRecord;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.data.types.BAMIntervalRecord;
import savant.util.Range;

/**
 *
 * @author AndrewBrook
 */
public class IntervalBamPopup extends PopupPanel {

    private BAMIntervalRecord rec;
    private SAMRecord samRec;
    private String homogenizedRef;

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

        if (samRec.getReadPairedFlag()) {
            String matePosition = "Mate Position: " + homogenizeRef(samRec.getMateReferenceName()) + ": " + samRec.getMateAlignmentStart();
            this.add(new JLabel(matePosition));

            final long matepos = samRec.getMateAlignmentStart();

            JButton matebutton = new JButton("Jump to mate");
            matebutton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    RangeController rc = RangeController.getInstance();
                    long offset = (long) Math.ceil(((float) rc.getRange().getLength())/2);
                    long start = samRec.getMateAlignmentStart()-offset;
                    long end = start + rc.getRange().getLength() - 1;
                    RangeController.getInstance().setRange(samRec.getMateReferenceName(), new Range(start, end));
                }
            });

            this.add(matebutton);
        }
    }

    @Override
    protected void initSpecificButtons() {

        //JUMP TO MATE
        /*if(fileFormat.equals(fileFormat.INTERVAL_BAM) && !mode.getName().equals("SNP")){
            JLabel mateJump = new JLabel("Jump to Mate");
            mateJump.setForeground(Color.BLUE);
            mateJump.setCursor(new Cursor(Cursor.HAND_CURSOR));
            mateJump.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    String mateRef = samRec.getMateReferenceName();
                    int mateStart = samRec.getMateAlignmentStart();
                    int length = samRec.getReadLength(); //TODO: how can I get the mate length?
                    mateRef = homogenizeRef(mateRef);
                    RangeController.getInstance().setRange(mateRef, new Range(mateStart, mateStart + length));
                    hidePanel();
                }
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
            });
            this.add(mateJump);
        }*/
    }

}
