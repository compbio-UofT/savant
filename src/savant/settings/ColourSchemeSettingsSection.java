/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandBarFactory;
import java.awt.BorderLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author mfiume
 */
public class ColourSchemeSettingsSection extends Section {

    @Override
    public String getSectionName() {
        return "Colour Schemes";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {

        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);
        this.add(new JLabel("empty section"));
    }

    @Override
    public void applyChanges() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
