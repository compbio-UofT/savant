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
import savant.data.types.TabixIntervalRecord;

/**
 *
 * @author AndrewBrook
 */
public class TabixPopup extends PopupPanel {

    private TabixIntervalRecord rec;

    public TabixPopup(TabixIntervalRecord rec){
        this.rec = rec;
    }

    @Override
    protected void calculateInfo() {
        //name = rec.getDescription();
        ref = rec.getReference();
        start = rec.getInterval().getStart();
        end = rec.getInterval().getEnd();
    }

    @Override
    protected void initInfo() {
        //String readName = "Description: " + name;
        //this.add(new JLabel(readName));

        String readStart = "Start:\t" + rec.getInterval().getStart();
        this.add(new JLabel(readStart));

        String readEnd = "End:\t" + rec.getInterval().getEnd();
        this.add(new JLabel(readEnd));

        String readLength = "Length:\t" + rec.getInterval().getLength();
        this.add(new JLabel(readLength));

        int fieldnum = 3;
        for (String s : rec.getOtherValues()) {
            this.add(new JLabel("Field" + (++fieldnum) + ":\t" + s));
            if (fieldnum == 10) {
                this.add(new JLabel("..."));
                break;
            }
        }
    }

}

