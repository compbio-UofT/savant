/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;

/**
 *
 * @author mfiume
 */
public class ResolutionSettingsSection extends Section {

    @Override
    public String getSectionName() {
        return "Track Resolutions";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void lazyInitialize() {

        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);
        JButton eb = new JButton("Enable");
        eb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableApplyButton();
            }
        });
        add(eb, BorderLayout.EAST);

        JButton db = new JButton("Disable");
        db.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                disableApplyButton();
            }
        });
        add(db, BorderLayout.WEST);
        //add(new JLabel("This is just a demo. This page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        //this.add(new JLabel("empty section"));
    }

    @Override
    public void applyChanges() {
        System.out.println("ResolutionSettingsSection: unsupported");
    }
}
