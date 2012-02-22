/*
 *    Copyright 2011 University of Toronto
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

package savant.ucscexplorer;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import savant.ucsc.GroupDef;
import savant.ucsc.TrackDef;

/**
 * Panel which contract to just a label, or expands to show a list of tracks.
 *
 * @author tarkvara
 */
public class GroupPanel extends JPanel {
    private GroupDef group;
    private JButton revealButton;
    private JLabel titleLabel;
    private JPanel expansion;
    private int widestCheck;

    /**
     * Updates the title to reflect the number of selected tracks.
     */
    private ActionListener totalUpdater = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            int total = 0;
            for (Component c: expansion.getComponents()) {
                if (((JCheckBox)c).isSelected()) {
                    total++;
                }
            }
            if (total > 0) {
                titleLabel.setText(String.format("%s (%d selected)", group, total));
            } else {
                titleLabel.setText(group.toString());
            }
        }
    };

    public GroupPanel(GroupDef g) {
        group = g;
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        Font f = getFont();

        revealButton = new JButton("+");
        revealButton.setFont(new Font(Font.MONOSPACED, Font.PLAIN, f.getSize()));
        revealButton.putClientProperty("JButton.buttonType", "square");
        revealButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (expansion.isVisible()) {
                    revealButton.setText("+");
                    expansion.setVisible(false);
                } else {
                    revealButton.setText("-");
                    ((GridLayout)expansion.getLayout()).setColumns(getWidth() / widestCheck);
                    expansion.setVisible(true);
                }
                validate();
            }
        });
        add(revealButton, gbc);
        
        titleLabel = new JLabel(g.toString());
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(titleLabel, gbc);

        expansion = new JPanel();
        expansion.setLayout(new GridLayout(0, 1, 5, 5));
        expansion.setVisible(false);
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        add(expansion, gbc);
        
        widestCheck = 1;
        Font smallFont = f.deriveFont(f.getSize() - 2.0f);
        for (TrackDef t: g.getTracks()) {
            TrackCheck check = new TrackCheck(t);
            check.setFont(smallFont);
            expansion.add(check);
            widestCheck = Math.max(widestCheck, check.getPreferredSize().width);
        }
    }
    
    List<TrackDef> getSelectedTracks() {
        List<TrackDef> result = new ArrayList<TrackDef>();
        for (Component c: expansion.getComponents()) {
            if (((TrackCheck)c).isSelected()) {
                result.add(((TrackCheck)c).track);
            }
        }
        return result;
    }

    void clearSelectedTracks() {
        for (Component c: expansion.getComponents()) {
            ((TrackCheck)c).setSelected(false);
        }
        totalUpdater.actionPerformed(null);
    }

    private class TrackCheck extends JCheckBox {
        private final TrackDef track;

        private TrackCheck(TrackDef t) {
            super(t.toString());
            track = t;
            addActionListener(totalUpdater);
        }
    }
}
