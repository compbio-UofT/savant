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

import javax.swing.JLabel;
import net.sf.samtools.SAMRecord;
import savant.data.types.BAMIntervalRecord;

/**
 *
 * @author AndrewBrook
 */
public class IntervalBamPopup extends PopupPanel {

    BAMIntervalRecord rec;
    SAMRecord samRec;
    String homogenizedRef;

    public IntervalBamPopup(){}

    @Override
    protected void calculateInfo() {
        rec = (BAMIntervalRecord) o;
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

        String matePosition = "Mate Position: " + homogenizeRef(samRec.getMateReferenceName()) + ": " + samRec.getMateAlignmentStart();
        this.add(new JLabel(matePosition));
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
