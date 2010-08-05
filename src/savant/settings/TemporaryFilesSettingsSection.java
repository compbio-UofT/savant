/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import java.awt.BorderLayout;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author mfiume
 */
public class TemporaryFilesSettingsSection extends Section {

    @Override
    public String getSectionName() {
        return "Temporary Files";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    public void lazyInitialize() {

        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);
        add(new JLabel("This is just a demo. This page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        //this.add(new JLabel("empty section"));
    }

    @Override
    public void applyChanges() {
        System.out.println("TemporaryFilesSettingsSection: unsupported");
    }
}
