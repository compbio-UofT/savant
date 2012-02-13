/*
 *    Copyright 2012 University of Toronto
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
package savant.view.variation.swing;

import java.text.ParseException;
import savant.view.variation.VariationController;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

import savant.api.data.VariantRecord;
import savant.api.util.DialogUtils;
import savant.controller.LocationController;
import savant.settings.TrackResolutionSettings;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.swing.ProgressPanel;
import savant.view.icon.SavantIconFactory;


/**
 * Panel which displays aggregated variation information in a variety of different ways.
 *
 * @author tarkvara
 */
public class VariationModule extends JPanel {
    private static final String ZOOM_MESSAGE = MiscUtils.MAC ? "<html><center>Zoom in to see data<br><small>To view data at this range, change Preferences > Track Resolutions</small></center></html>" : "<html><center>Zoom in to see data<br><small>To view data at this range, change Edit > Preferences > Track Resolutions</small></center></html>";

    private VariationController controller;

    // The cards we can show.
    private JTabbedPane tabs;
    private JLabel messageLabel;
    private ProgressPanel progressPanel;
    private JComponent currentCard;

    private JTable table;
    private VariantMap map;
    private AlleleFrequencyPlot frequencyPlot;
    private LDPlot ldPlot;
    
    private JTextField rangeField;
    private JScrollBar mapScroller;
    private JScrollBar frequencyScroller;
    private ButtonGroup methodGroup;

    /** Listener shared by mapScroller and frequencyScroller. */
    private AdjustmentListener scrollerListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent ae) {
            if (!ae.getValueIsAdjusting()) {
                controller.setVisibleRange(ae.getValue());
            }
        }
    };

    public VariationModule(VariationController vc) {
        super(new GridBagLayout());
        controller = vc;

        // Toolbar shared by all panels.
        JToolBar tools = new JToolBar();
        tools.setFloatable(false);
        rangeField = new JTextField();
        rangeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String text = rangeField.getText();
                try {
                    Bookmark bm = new Bookmark(text, controller.getReference(), controller.getVisibleRange());
                    controller.setLocation(bm.getReference(), (Range)bm.getRange());
                } catch (ParseException x) {
                    DialogUtils.displayMessage(String.format("Unable to parse \"%s\" as a location.", text));
                }
            } 
        });
        tools.add(rangeField);
        tools.addSeparator();
        
        JButton zoomInButton = new JButton();
        zoomInButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMIN));
        zoomInButton.setBorderPainted(false);
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.zoomIn();
            }
        });
        tools.add(zoomInButton);

        JButton zoomOutButton = new JButton();
        zoomOutButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.ZOOMOUT));
        zoomOutButton.setBorderPainted(false);
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.zoomOut();
            }
        });
        tools.add(zoomOutButton);

        tools.addSeparator();
        JButton controlsButton = new JButton("Controls");
        controlsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller.getTracks().length > 0) {
                    new CaseControlDialog(controller).setVisible(true);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        tools.add(controlsButton);
        
        tabs = new JTabbedPane();
        table = new JTable(new VariantTableModel(null));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    controller.navigateToRecord(controller.getData().get(table.getSelectedRow()));
                }
            }
        });
        tabs.addTab("Table", new JScrollPane(table));

        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new GridBagLayout());
        
        mapScroller = new JScrollBar();
        mapScroller.addAdjustmentListener(scrollerListener);

        map = new VariantMap(controller);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mapPanel.add(map, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mapPanel.add(mapScroller, gbc);
        tabs.addTab("Map", mapPanel);

        JPanel frequencyPanel = new JPanel();
        frequencyPanel.setLayout(new GridBagLayout());
        
        frequencyScroller = new JScrollBar();
        frequencyScroller.addAdjustmentListener(scrollerListener);

        frequencyPlot = new AlleleFrequencyPlot(controller);
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        frequencyPanel.add(frequencyPlot, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        frequencyPanel.add(frequencyScroller, gbc);
        tabs.addTab("Allele Frequency", frequencyPanel);
        
        JPanel ldPanel = new JPanel();
        ldPanel.setLayout(new GridBagLayout());

        ActionListener redrawForcer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ldPlot.recalculate();
                ldPlot.repaint();
            }
        };
        JRadioButton dPrimeButton = new JRadioButton("D′", true);
        dPrimeButton.setActionCommand("true");
        dPrimeButton.addActionListener(redrawForcer);
        JRadioButton rSquaredButton = new JRadioButton("r²", false);
        rSquaredButton.setActionCommand("false");
        rSquaredButton.addActionListener(redrawForcer);
        
        methodGroup = new ButtonGroup();
        JPanel methodPanel = new JPanel();
        methodPanel.setBorder(BorderFactory.createTitledBorder("Calculation Method"));
        methodPanel.add(dPrimeButton);
        methodGroup.add(dPrimeButton);
        methodPanel.add(rSquaredButton);
        methodGroup.add(rSquaredButton);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        ldPanel.add(methodPanel, gbc);

        ldPlot = new LDPlot(controller);
        gbc.weighty = 1.0;
        ldPanel.add(ldPlot, gbc);

        tabs.addTab("LD Plot", ldPanel);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        add(tools, gbc);

        // Create the informative cards, but don't use them.
        messageLabel = new JLabel();
        messageLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 24));
        messageLabel.setAlignmentX(0.5f);
        progressPanel = new ProgressPanel(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.cancelDataRequests();
            }
        });
        
        showCard(tabs, null);
    }

    public boolean isDPrimeSelected() {
        return Boolean.parseBoolean(methodGroup.getSelection().getActionCommand());
    }

    private void showCard(JComponent card, String message) {
        if (currentCard != card) {
            if (currentCard != null) {
                remove(currentCard);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            
            if (card == tabs) {
                gbc.fill = GridBagConstraints.BOTH;
            } else {
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(20, 20, 20, 20);
                gbc.anchor = GridBagConstraints.NORTH;
                if (message != null) {
                    if (card == messageLabel) {
                        messageLabel.setText(message);
                    } else {
                        progressPanel.setMessage(message);
                    }
                }
            }
            add(card, gbc);
            currentCard = card;
            repaint();
            validate();
        }
    }
    
    public void showMessage(String message) {
        showCard(messageLabel, message);
    }
    
    public void showProgress(String message) {
        showCard(progressPanel, message);
    }
    
    public void visibleRangeChanged(String ref, Range r) {
        if (r.getLength() > TrackResolutionSettings.getVariantLowToHighThreshold()) {
            showMessage(ZOOM_MESSAGE);
        } else {
            mapScroller.setMaximum(LocationController.getInstance().getMaxRangeEnd());
            mapScroller.setValue(r.getFrom());
            mapScroller.setVisibleAmount(r.getLength());
            mapScroller.setBlockIncrement(r.getLength());
            mapScroller.repaint();
            frequencyScroller.setMaximum(LocationController.getInstance().getMaxRangeEnd());
            frequencyScroller.setValue(r.getFrom());
            frequencyScroller.setVisibleAmount(r.getLength());
            frequencyScroller.setBlockIncrement(r.getLength());
            frequencyScroller.repaint();
        }
        rangeField.setText(String.format("%s:%d-%d", ref, r.getFrom(), r.getTo()));
    }

    public void recalculated(List<VariantRecord> data) {
        table.setModel(new VariantTableModel(data));
        if (data.size() > 0) {
            showCard(tabs, null);
            map.repaint();
            ldPlot.recalculate();
            ldPlot.repaint();
        } else {
            showMessage("No data in range");
        }

    }
}
