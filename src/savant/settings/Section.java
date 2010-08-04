/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

}
