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
package savant.ucscchooser;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.event.PluginEvent;
import savant.api.util.Listener;
import savant.plugin.PluginController;
import savant.plugin.SavantPanelPlugin;
import savant.ucsc.CladesFetcher;
import savant.ucsc.GenomeDef;
import savant.ucsc.GroupDef;
import savant.ucsc.GroupsFetcher;
import savant.ucsc.UCSCDataSourcePlugin;


/**
 * User interface for UCSC plugin which lets you pick the tracks you want to use.
 *
 * @author tarkvara
 */
public class UCSCChooserPlugin extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(UCSCChooserPlugin.class);

    /** Hard-coded ID of UCSC DataSource plugin. */
    private static final String UCSC_PLUGIN_ID = "savant.ucsc";

    /** Top-level topLevelPanel provided by Savant. */
    JPanel topLevelPanel;

    private JProgressBar progressBar;
    private JLabel progressLabel;

    /** Combo-box containing standard list of clades available from UCSC. */
    private JComboBox cladeCombo;

    /** Combo-box containing genomes for the currently-selected clade. */
    private JComboBox genomeCombo;
    
    /** Panel containing list of groups. */
    private JPanel groupsPanel;

    
    @Override
    public void init(JPanel panel) {
        topLevelPanel = panel;
        panel.setLayout(new GridBagLayout());
        
        // If the UCSC plugin is already loaded, we can build our UI.  Otherwise,
        // we leave a placeholder here.
        if (getUCSCPlugin() != null) {
            buildUI();
        } else {
            panel.add(new JLabel("Waiting for UCSC plugin to load."), new GridBagConstraints());
            PluginController.getInstance().addListener(new Listener<PluginEvent>() {
                @Override
                public void handleEvent(PluginEvent event) {
                    if (event.getPluginID().equals(UCSC_PLUGIN_ID)) {
                        if (event.getType().equals(PluginEvent.Type.LOADED)) {
                            // Yay!  The UCSC plugin has finally made an appearance.
                            buildUI();
                        } else {
                            LOG.info("Something went wrong with the UCSC plugin.");
                        }
                        PluginController.getInstance().removeListener(this);
                    }
                }
            });
        }
    }

    private void buildUI() {
        topLevelPanel.removeAll();
        topLevelPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
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

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        topLevelPanel.add(cladeCombo, gbc);
        
        JLabel genomeLabel = new JLabel("Genome:");
        gbc.gridwidth = 1;
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
                            LOG.info("GroupsFetcher returned " + groups.size() + " defs.");
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

                            topLevelPanel.validate();
                        } else {
                            LOG.info("GroupFetcher returned null.");
                        }
                    }

                    @Override
                    public void showProgress(double value) {
                        updateProgress(progressMessage, value);
                    }
                }.execute();
            }
        });
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        topLevelPanel.add(genomeCombo, gbc);
        
        groupsPanel = new JPanel();
        groupsPanel.setLayout(new GridBagLayout());
        
        JScrollPane groupsScroller = new JScrollPane(groupsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        topLevelPanel.add(groupsScroller, gbc);

        buildProgressUI();

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
    }

    /**
     * While the database is loading we want to have a progress-bar.
     */
    private void buildProgressUI() {
        groupsPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        progressLabel = new JLabel();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(5, 5, 5, 5);
        groupsPanel.add(progressLabel, gbc);

        progressBar = new JProgressBar();
        groupsPanel.add(progressBar, gbc);
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

    private UCSCDataSourcePlugin getUCSCPlugin() {
        return (UCSCDataSourcePlugin)PluginController.getInstance().getPlugin(UCSC_PLUGIN_ID);
    }

    @Override
    public String getTitle() {
        return "UCSC Chooser";
    }
}
