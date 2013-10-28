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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GenomeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.event.GenomeChangedEvent;
import savant.api.event.PluginEvent;
import savant.api.util.DialogUtils;
import savant.api.util.GenomeUtils;
import savant.api.util.Listener;
import savant.api.util.PluginUtils;
import savant.api.util.TrackUtils;
import savant.plugin.PluginController;
import savant.plugin.SavantPanelPlugin;
import savant.sql.MappedTable;
import savant.ucsc.*;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;


/**
 * User interface for UCSC plugin which lets you pick the tracks you want to use.
 *
 * @author tarkvara
 */
public class UCSCExplorerPlugin extends SavantPanelPlugin {
    static final Log LOG = LogFactory.getLog(UCSCExplorerPlugin.class);

    /** Hard-coded ID of UCSC DataSource plugin. */
    private static final String UCSC_PLUGIN_ID = "savant.ucsc";

    /** Hard-coded URL of UCSC DataSource plugin. */
    private static final URL UCSC_PLUGIN_URL = NetworkUtils.getKnownGoodURL("http://www.savantbrowser.com/plugins/esmith/savant.ucsc-1.1.6.jar");

    /** Top-level panel provided by Savant. */
    JPanel topLevelPanel;

    private JProgressBar progressBar;
    private JLabel progressLabel;

    /** Combo-box containing standard list of clades available from UCSC. */
    private JComboBox cladeCombo;

    /** Combo-box containing genomes for the currently-selected clade. */
    private JComboBox genomeCombo;
    
    /** Panel containing list of groups. */
    private JPanel groupsPanel;

    /** Button which actually loads the tracks. */
    private JButton loadButton;

    @Override
    public void init(JPanel panel) {
        topLevelPanel = panel;
        panel.setLayout(new GridBagLayout());
        
        // If the UCSC plugin is already loaded, we can build our UI.  Otherwise,
        // we leave a placeholder here.
        if (getUCSCPlugin() != null) {
            buildUI();
        } else {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(new JLabel("Unable to access UCSC DataSource plugin."), gbc);

            JButton installUCSCButton = new JButton("Install Now");
            installUCSCButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    PluginUtils.installPlugin(UCSC_PLUGIN_URL);
                }
            });
            panel.add(installUCSCButton, gbc);

            PluginController.getInstance().addListener(new Listener<PluginEvent>() {
                @Override
                public void handleEvent(PluginEvent event) {
                    if (event.getPluginID().equals(UCSC_PLUGIN_ID)) {
                        if (event.getType().equals(PluginEvent.Type.LOADED)) {
                            // Yay!  The UCSC plugin has finally made an appearance.
                            buildUI();
                        }
                        PluginController.getInstance().removeListener(this);
                    }
                }
            });
        }
    }

    @Override
    public String getTitle() {
        return "UCSC Explorer";
    }

    private void buildUI() {
        topLevelPanel.removeAll();
        topLevelPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        try {
            UCSCDataSourcePlugin ucsc = getUCSCPlugin();
            ucsc.getConnection();
            JLabel cladeLabel = new JLabel("Clade:");
            gbc.anchor = GridBagConstraints.EAST;
            topLevelPanel.add(cladeLabel, gbc);

            cladeCombo = new JComboBox();
            cladeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    UCSCDataSourcePlugin ucsc = getUCSCPlugin();
                    String clade = (String)cladeCombo.getSelectedItem();
                    genomeCombo.setModel(new DefaultComboBoxModel(ucsc.getCladeGenomes(clade)));
                    genomeCombo.setSelectedItem(ucsc.getCurrentGenome(clade));
                }
            });

            gbc.anchor = GridBagConstraints.WEST;
            topLevelPanel.add(cladeCombo, gbc);

            JLabel genomeLabel = new JLabel("Genome:");
            gbc.anchor = GridBagConstraints.EAST;
            topLevelPanel.add(genomeLabel, gbc);

            genomeCombo = new JComboBox();
            genomeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    buildProgressUI();
                    new GroupsFetcher(getUCSCPlugin(), (GenomeDef)genomeCombo.getSelectedItem()) {
                        @Override
                        public void done(List<GroupDef> groups) {
                            if (groups != null) {
                                GridBagConstraints gbc = new GridBagConstraints();
                                gbc.gridwidth = GridBagConstraints.REMAINDER;
                                gbc.fill = GridBagConstraints.BOTH;
                                gbc.weightx = 1.0;
                                for (GroupDef g: groups) {
                                    groupsPanel.add(new GroupPanel(g), gbc);
                                }

                                // Add a filler panel to force everything to the top.
                                gbc.weighty = 1.0;
                                groupsPanel.add(new JPanel(), gbc);
                                loadButton.setEnabled(true);
                                topLevelPanel.validate();
                            }
                        }

                        @Override
                        public void showProgress(double value) {
                            updateProgress(progressMessage, value);
                        }
                    }.execute();
                }
            });
            gbc.anchor = GridBagConstraints.WEST;
            topLevelPanel.add(genomeCombo, gbc);

            loadButton = new JButton("Load Selected Tracks");
            loadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        loadSelectedTracks();
                    } catch (Throwable x) {
                        DialogUtils.displayException(getTitle(), "Unable to load selected tracks.", x);
                    }
                }            
            });
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            topLevelPanel.add(loadButton, gbc);

            groupsPanel = new GroupsPanel();
            groupsPanel.setLayout(new GridBagLayout());

            JScrollPane groupsScroller = new JScrollPane(groupsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            topLevelPanel.add(groupsScroller, gbc);

            buildProgressUI();

            GenomeUtils.addGenomeChangedListener(new Listener<GenomeChangedEvent>() {
                @Override
                public void handleEvent(GenomeChangedEvent event) {
                    UCSCDataSourcePlugin ucsc = getUCSCPlugin();
                    ucsc.selectGenomeDB(null);
                    GenomeAdapter newGenome = event.getNewGenome();
                    GenomeDef g = new GenomeDef(newGenome.getName(), null);
                    String newClade = ucsc.findCladeForGenome(g);

                    // newClade could be null if the user has opened a genome which has no UCSC equivalent.
                    if (newClade != null) {
                        cladeCombo.setSelectedItem(newClade);
                    }
                }
            });
            
            ucsc.selectGenomeDB(null);
            new CladesFetcher(getUCSCPlugin()) {
                @Override
                public void done(String selectedClade) {
                    cladeCombo.setModel(new DefaultComboBoxModel(UCSCDataSourcePlugin.STANDARD_CLADES));
                    if (selectedClade != null) {
                        cladeCombo.setSelectedItem(selectedClade);
                    } else {
                        cladeCombo.setSelectedIndex(0);
                    }
                }

                @Override
                public void showProgress(double value) {
                    updateProgress(progressMessage, value);
                }
            }.execute();
        } catch (Exception x) {
            LOG.error("Unable to connect to UCSC database.", x);
            topLevelPanel.removeAll();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            topLevelPanel.add(new JLabel("Unable to connect to UCSC database."), gbc);
            JLabel error = new JLabel(MiscUtils.getMessage(x));
            Font f = topLevelPanel.getFont();
            f = f.deriveFont(Font.ITALIC, f.getSize() - 2.0f);
            error.setFont(f);
            topLevelPanel.add(error, gbc);
        }
    }

    /**
     * While the database is loading we want to have a progress-bar.
     */
    private void buildProgressUI() {
        loadButton.setEnabled(false);
        groupsPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        progressLabel = new JLabel();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(5, 5, 5, 5);
        groupsPanel.add(progressLabel, gbc);

        progressBar = new JProgressBar();
        groupsPanel.add(progressBar, gbc);
        groupsPanel.validate();
    }

    private void updateProgress(final String message, final double value) {
        if (value >= 1.0) {
            // We're done.  Clear away the progress.
            groupsPanel.removeAll();
            progressLabel = null;
            progressBar = null;
        } else {
            progressLabel.setText(message);
            if (value < 0.0) {
                // Indefinite progress.
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setIndeterminate(false);
                progressBar.setValue((int)(value * 100.0));
            }
        }
        groupsPanel.validate();
    }

    /**
     * Utility method to get the UCSC DataSource plugin we're piggybacking off of.
     */
    static UCSCDataSourcePlugin getUCSCPlugin() {
        return (UCSCDataSourcePlugin)PluginController.getInstance().getPlugin(UCSC_PLUGIN_ID);
    }
    
    private void loadSelectedTracks() throws Throwable {
        TrackAdapter[] existingTracks = TrackUtils.getTracks();
        List<MappedTable> newTracks = new ArrayList<MappedTable>();

        List<TrackDef> selectedTracks = new ArrayList<TrackDef>();
        for (Component c: groupsPanel.getComponents()) {
            if (c instanceof GroupPanel) {
                selectedTracks.addAll(((GroupPanel)c).getSelectedTracks());
            }
        }

        UCSCDataSourcePlugin ucsc = getUCSCPlugin();
        for (TrackDef t: selectedTracks) {
            MappedTable table = ucsc.getTableWithStandardMapping(t);
            if (table.isMapped()) {
                table.getMapping().save(ucsc);
            } else {
                LOG.warn(t + " was not successfully mapped.");
            }
            URI mappedURI = table.getURI();
            for (TrackAdapter t2: existingTracks) {
                if (mappedURI.equals(t2.getDataSource().getURI())) {
                    // Track already exists.  Drop it from our list.
                    table = null;
                    LOG.info(mappedURI + " already found in loaded tracks.");
                    break;
                }
            }
            if (table != null) {
                newTracks.add(table);
            }
        }
        
        LOG.info("After eliminating duplicates, " + newTracks.size() + " new tracks to be loaded.");
        if (newTracks.size() > 0) {
            for (MappedTable mt: newTracks) {
                TrackUtils.createTrack(mt.getURI());
            }
        }
        
        // Success.  Let's clear away the selections.
        for (Component c: groupsPanel.getComponents()) {
            if (c instanceof GroupPanel) {
                ((GroupPanel)c).clearSelectedTracks();
            }
        }
    }
    
    private class GroupsPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle rctngl, int i, int i1) {
            return 1;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle rctngl, int i, int i1) {
            return 10;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getPreferredSize().height < getParent().getHeight();
        }
    }
}
