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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.diff;

import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;


/**
 * Dialog which lets a user select which underlying continuous datasources will be
 * used as inputs for the plugin.
 *
 * @author tarkvara
 */
public class SourceDialog extends JDialog {
    private JList aList, bList;
    URI result;

    /**
     * Create the dialog which allows the user to select sources.
     *
     * @param availableTracks an array containing at least two continuous Tracks.
     */
    public SourceDialog(TrackAdapter[] availableTracks) {
        super(DialogUtils.getMainWindow(), Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(9, 9, 9, 9);

        aList = new JList(availableTracks);
        aList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        aList.setSelectedIndex(0);
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(aList, gbc);

        bList = new JList(availableTracks);
        bList.setSelectedIndex(1);
        bList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(bList, gbc);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                TrackAdapter trackA = (TrackAdapter)aList.getSelectedValue();
                TrackAdapter trackB = (TrackAdapter)bList.getSelectedValue();
                if (trackA != trackB) {
                    try {
                        result = DiffDataSource.getDiffURI(trackA, trackB);
                        setVisible(false);
                    } catch (URISyntaxException x) {
                        DialogUtils.displayException("URI Syntax Exception", String.format("Unable to create a URI from %s and %s.", trackA, trackB), x);
                    }
                } else {
                    DialogUtils.displayMessage("Sorry", "Plugin sources must be different tracks.");
                }
            }
        });
        getRootPane().setDefaultButton(okButton);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(okButton, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                result = null;
                setVisible(false);
            }
        });
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        add(cancelButton, gbc);
        pack();
        setLocationRelativeTo(DialogUtils.getMainWindow());
    }
}
