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
import savant.data.types.GenericContinuousRecord;

/**
 *
 * @author AndrewBrook
 */
public class ContinuousPopup extends PopupPanel {

    GenericContinuousRecord rec;

    public ContinuousPopup(){}

    @Override
    protected void calculateInfo() {
        rec = (GenericContinuousRecord) o;
        //ref = rec.getReference();
        start = rec.getPosition();
        end = rec.getPosition();
        name = "Value: " + rec.getValue().getValue(); //for bookmarking annotation
    }

    @Override
    protected void initInfo() {

        String readPosition = "Position: " + start;
        this.add(new JLabel(readPosition));

        String readValue = "Value: " + rec.getValue().getValue();
        this.add(new JLabel(readValue));

    }

}
