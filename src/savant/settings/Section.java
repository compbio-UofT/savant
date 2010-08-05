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

package savant.settings;

import com.jidesoft.dialog.AbstractDialogPage;
import javax.swing.Icon;

/**
 *
 * @author mfiume
 */
public abstract class Section extends AbstractDialogPage {

    public Section() {
        super("","",null);
        this.setTitle(getSectionName());
        this.setIcon(getSectionIcon());
    }

    public abstract String getSectionName();

    public abstract Icon getSectionIcon();

    public abstract void applyChanges();

    public void enableApplyButton() {
        fireButtonEvent(com.jidesoft.dialog.ButtonEvent.ENABLE_BUTTON, com.jidesoft.dialog.MultiplePageDialog.APPLY);
    }

    public void disableApplyButton() {
        fireButtonEvent(com.jidesoft.dialog.ButtonEvent.DISABLE_BUTTON, com.jidesoft.dialog.MultiplePageDialog.APPLY);
    }

    public void populate(){};

}
