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
package savant.ucsc;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.api.util.SettingsUtils;
import savant.sql.*;
import savant.sql.MappingDialog.FormatDef;
import savant.util.MiscUtils;


/**
 * UCSC-specific dialog for selecting the desired table.
 *
 * @author tarkvara
 */
public class UCSCNavigationDialog extends JDialog implements SQLConstants {
    private static final Log LOG = LogFactory.getLog(UCSCNavigationDialog.class);

    private MappingPanel mappingPanel;
    private ColumnMapping knownMapping;
    private Table table;

    private UCSCDataSourcePlugin plugin = null;

    /**
     * Dialog which lets the user navigate through the UCSC hierarchy and select
     * the table they want.
     *
     * @param parent parent window (cannot be null)
     * @param plug associated Plugin object
     * @param t current table (determines initial state of dialog)
     */
    public UCSCNavigationDialog(Window parent, UCSCDataSourcePlugin plug, Table t) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.plugin = plug;
        this.table = t;
        initComponents();
        MiscUtils.registerCancelButton(cancelButton);

        mappingPanel = new MappingPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        add(mappingPanel, gbc);

        formatCombo.setModel(MappingDialog.FORMAT_COMBO_MODEL);
        populateCladeCombo();
        pack();
        setLocationRelativeTo(parent);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        javax.swing.JButton okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        javax.swing.JPanel navigationPanel = new javax.swing.JPanel();
        javax.swing.JLabel cladeLabel = new javax.swing.JLabel();
        cladeCombo = new javax.swing.JComboBox();
        javax.swing.JLabel genomeLabel = new javax.swing.JLabel();
        genomeCombo = new javax.swing.JComboBox();
        javax.swing.JLabel groupLabel = new javax.swing.JLabel();
        groupCombo = new javax.swing.JComboBox();
        javax.swing.JLabel trackLabel = new javax.swing.JLabel();
        trackCombo = new javax.swing.JComboBox();
        javax.swing.JLabel formatLabelLabel = new javax.swing.JLabel();
        formatLabel = new javax.swing.JLabel();
        formatCombo = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("UCSC Genome Database");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(okButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(cancelButton, gridBagConstraints);

        navigationPanel.setLayout(new java.awt.GridBagLayout());

        cladeLabel.setText("Clade:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(cladeLabel, gridBagConstraints);

        cladeCombo.setMaximumSize(new java.awt.Dimension(300, 32767));
        cladeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cladeComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(cladeCombo, gridBagConstraints);

        genomeLabel.setText("Genome:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(genomeLabel, gridBagConstraints);

        genomeCombo.setMaximumSize(new java.awt.Dimension(300, 32767));
        genomeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genomeComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(genomeCombo, gridBagConstraints);

        groupLabel.setText("Group:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(groupLabel, gridBagConstraints);

        groupCombo.setMaximumRowCount(9);
        groupCombo.setMaximumSize(new java.awt.Dimension(300, 32767));
        groupCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(groupCombo, gridBagConstraints);

        trackLabel.setText("Track:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(trackLabel, gridBagConstraints);

        trackCombo.setMaximumSize(new java.awt.Dimension(300, 32767));
        trackCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(trackCombo, gridBagConstraints);

        formatLabelLabel.setText("Format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(formatLabelLabel, gridBagConstraints);

        formatLabel.setText("BED");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(formatLabel, gridBagConstraints);

        formatCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(formatCombo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(navigationPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        ColumnMapping mapping = mappingPanel.getMapping();
        mapping.save(plugin);
        setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

    private void cladeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cladeComboActionPerformed
        populateGenomeCombo();
    }//GEN-LAST:event_cladeComboActionPerformed

    private void genomeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genomeComboActionPerformed
        populateGroupCombo();
    }//GEN-LAST:event_genomeComboActionPerformed

    private void groupComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupComboActionPerformed
        populateTrackCombo();
    }//GEN-LAST:event_groupComboActionPerformed

    private void trackComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackComboActionPerformed
        TrackDef track = (TrackDef)trackCombo.getSelectedItem();
        table = new Table(track.table, plugin.genomeDB);
        knownMapping = UCSCDataSourcePlugin.getStandardMapping(track.type);
        formatLabel.setText(track.type);
        if (knownMapping != null) {
            switch (knownMapping.format) {
                case INTERVAL_RICH:
                    formatCombo.setSelectedIndex(0);
                    break;
                case INTERVAL_GENERIC:
                    formatCombo.setSelectedIndex(1);
                    break;
                case CONTINUOUS_VALUE_COLUMN:
                    formatCombo.setSelectedIndex(2);
                    break;
                case CONTINUOUS_WIG:
                    formatCombo.setSelectedIndex(3);
                    break;
                case EXTERNAL_FILE:
                    formatCombo.setSelectedIndex(4);
                    break;
            }
        }
    }//GEN-LAST:event_trackComboActionPerformed

    private void formatComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatComboActionPerformed
        mappingPanel.setFormat(((FormatDef)formatCombo.getSelectedItem()).format);
        try {
            TrackDef trackDef = (TrackDef)trackCombo.getSelectedItem();
            mappingPanel.populate(table.getColumns(), knownMapping, !table.getName().equals(trackDef.track) && !table.getName().equals("all_" + trackDef.track));
        } catch (SQLException sqlx) {
            DialogUtils.displayException("SQL Error", "Unable to get list of columns.", sqlx);
        }
    }//GEN-LAST:event_formatComboActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox cladeCombo;
    private javax.swing.JComboBox formatCombo;
    private javax.swing.JLabel formatLabel;
    private javax.swing.JComboBox genomeCombo;
    private javax.swing.JComboBox groupCombo;
    private javax.swing.JComboBox trackCombo;
    // End of variables declaration//GEN-END:variables

    private void populateCladeCombo() {
        plugin.selectGenomeDB(table);
        new CladesFetcher(plugin) {
            @Override
            public void done(String selectedClade) {
                cladeCombo.setModel(new DefaultComboBoxModel(UCSCDataSourcePlugin.STANDARD_CLADES));
                if (selectedClade != null) {
                    cladeCombo.setSelectedItem(selectedClade);
                } else {
                    cladeCombo.setSelectedIndex(0);
                }
            }
        }.execute();
    }

    private void populateGenomeCombo() {
        String clade = (String)cladeCombo.getSelectedItem();
        genomeCombo.setModel(new DefaultComboBoxModel(plugin.getCladeGenomes(clade)));
        genomeCombo.setSelectedItem(plugin.getCurrentGenome(clade));
    }

    private void populateGroupCombo() {
        new GroupsFetcher(plugin, (GenomeDef)genomeCombo.getSelectedItem()) {
            @Override
            public void done(List<GroupDef> groups) {
                if (groups != null) {
                    groupCombo.setModel(new DefaultComboBoxModel(groups.toArray()));
                    if (table != null) {
                        TrackDef curTrack = new TrackDef(table.getName(), null, null, null);
                        for (GroupDef g: groups) {
                            if (g.tracks.contains(curTrack)) {
                                LOG.debug("populateGroupCombo setting selected group to " + g + " for track " + curTrack);
                                groupCombo.setSelectedItem(g);
                                return;
                            }
                        }
                    }
                    LOG.debug("populateGroupCombo setting selected group to 0.");
                    groupCombo.setSelectedIndex(0);
                }
            }
        }.execute();
    }

    private void populateTrackCombo() {
        GroupDef group = (GroupDef)groupCombo.getSelectedItem();
        LOG.trace("populating track combo based on " + group);
        trackCombo.setModel(new DefaultComboBoxModel(group.tracks.toArray()));
        int index = 0;
        if (table != null) {
            index = group.tracks.indexOf(new TrackDef(table.getName(), null, null, null));
            if (index < 0) {
                index = 0;
            }
        }
        trackCombo.setSelectedIndex(index);
    }

    public MappedTable getMapping() {
        if (table != null) {
            SettingsUtils.setString(plugin, "GENOME", plugin.genomeDB.getName());
            return new MappedTable(table, mappingPanel.getMapping(), ((TrackDef)trackCombo.getSelectedItem()).track);
        }
        return null;
    }
}
