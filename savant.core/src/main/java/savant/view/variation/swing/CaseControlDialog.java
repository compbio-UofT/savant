/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.view.variation.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import savant.api.util.DialogUtils;
import savant.util.MiscUtils;
import savant.view.tracks.VariantTrack;
import savant.view.variation.VariationController;


/**
 * Dialog which lets user select which individuals to use as controls.
 * @author tarkvara
 */
public class CaseControlDialog extends JDialog {
    VariationController controller;
    JPanel checksPanel;
    
    CaseControlDialog(VariationController vc) {
        super(DialogUtils.getMainWindow(), Dialog.ModalityType.APPLICATION_MODAL);
        controller = vc;
        setTitle("Select Controls");
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        checksPanel = new JPanel();
        checksPanel.setLayout(new GridBagLayout());

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        for (VariantTrack t: controller.getTracks()) {
            gbc.insets = new Insets(0, 0, 0, 0);
            JCheckBox cb1 = new JCheckBox(t.getName());
            boolean trackSelected = controller.isAControl(t.getName());
            cb1.setSelected(trackSelected);
            checksPanel.add(cb1, gbc);

            List<JCheckBox> dependents = new ArrayList<JCheckBox>();
            gbc.insets = new Insets(0, 40, 0, 0);
            for (String p: t.getParticipantNames()) {
                JCheckBox cb2 = new JCheckBox(p);
                cb2.setSelected(trackSelected || controller.isAControl(p));
                cb2.addActionListener(new ParticipantCheckListener(cb1));
                checksPanel.add(cb2, gbc);
                dependents.add(cb2);
            }
            cb1.addActionListener(new TrackCheckListener(dependents));
        }

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                updateControls();
                setVisible(false);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setVisible(false);
            }
        });

        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(new JScrollPane(checksPanel), gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(okButton, gbc);
        gbc.weightx = 0.0;
        add(cancelButton, gbc);

        pack();
        getRootPane().setDefaultButton(okButton);
        MiscUtils.registerCancelButton(cancelButton);
        setLocationRelativeTo(getParent());
    }
    
    void updateControls() {
        Set<String> newControls = new HashSet<String>();
        for (Component c: checksPanel.getComponents()) {
            if (((JCheckBox)c).isSelected()) {
                newControls.add(((JCheckBox)c).getText());
            }
        }
        controller.setControls(newControls);
    }
    
    static class TrackCheckListener implements ActionListener {
        List<JCheckBox> dependents;
        
        TrackCheckListener(List<JCheckBox> deps) {
            dependents = deps;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            JCheckBox sender = (JCheckBox)ae.getSource();
            for (JCheckBox dep: dependents) {
                dep.setSelected(sender.isSelected());
            }
        }
    }

    static class ParticipantCheckListener implements ActionListener {
        JCheckBox trackCheck;
        
        ParticipantCheckListener(JCheckBox cb) {
            trackCheck = cb;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            trackCheck.setSelected(false);
        }
    }
}
