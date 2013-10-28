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
package savant.ucscexplorer;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
    private ComponentListener widthListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent ce) {
            GridLayout expansionLayout = (GridLayout)expansion.getLayout();
            int cols = Math.max(1, expansion.getWidth() / widestCheck);
            if (cols != expansionLayout.getColumns()) {
                expansionLayout.setColumns(cols);
                expansion.validate();
            }
        }
    };


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
                    expansion.removeComponentListener(widthListener);
                    expansion.setVisible(false);
                } else {
                    revealButton.setText("-");
                    ((GridLayout)expansion.getLayout()).setColumns(getWidth() / widestCheck);
                    expansion.addComponentListener(widthListener);
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
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
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
