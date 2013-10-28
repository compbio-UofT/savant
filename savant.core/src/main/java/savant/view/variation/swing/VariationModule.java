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

import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.VariantRecord;
import savant.api.event.LocationChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.LocationController;
import savant.settings.ResolutionSettings;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.swing.ProgressPanel;
import savant.util.swing.RecordTable;
import savant.view.icon.SavantIconFactory;
import savant.view.variation.VariationController;


/**
 * Panel which displays aggregated variation information in a variety of different ways.
 *
 * @author tarkvara
 */
public class VariationModule extends JPanel {
    static final Log LOG = LogFactory.getLog(VariationModule.class);
    private static final String ZOOM_MESSAGE = MiscUtils.MAC ? "<html><center>Zoom in to see data<br><small>To view data at this range, change Preferences > Track Resolutions</small></center></html>" : "<html><center>Zoom in to see data<br><small>To view data at this range, change Edit > Preferences > Track Resolutions</small></center></html>";
    static final Font MESSAGE_FONT = new Font("Sans-Serif", Font.PLAIN, 24);
    private static final Insets MESSAGE_INSETS = new Insets(20, 20, 20, 20);


    private VariationController controller;

    // The cards we can show.
    private JTabbedPane tabs;
    private JPanel messagePanel;
    private ProgressPanel progressPanel;
    private JComponent currentCard;

    private JTable table;
    private VariantMap map;
    private AlleleFrequencyPlot frequencyPlot;
    private LDPlot ldPlot;
    
    private JTextField rangeField;
    private List<JScrollBar> scrollers = new ArrayList<JScrollBar>();
    private ButtonGroup methodGroup;
    private JLabel messageLabel;

    /** Listener shared by mapScroller and frequencyScroller. */
    private AdjustmentListener scrollerListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent ae) {
            if (!ae.getValueIsAdjusting()) {
                controller.setVisibleRange(ae.getValue());
            }
        }
    };
    
    /** Wheel listener shared by map and frequencyPlot. */
    private MouseWheelListener wheelListener = new MouseWheelListener() {

        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {
            int notches = mwe.getWheelRotation();
            Range visRange = controller.getVisibleRange();
            controller.setVisibleRange(visRange.getFrom() + visRange.getLength() * notches / 10);
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
                    frequencyPlot.repaint();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        tools.add(controlsButton);
        
        tabs = new JTabbedPane();
        table = new RecordTable(new VariantTableModel(null));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    controller.navigateToRecord(controller.getData().get(table.getSelectedRow()));
                }
            }
        });
        tabs.addTab("Table", new JScrollPane(table));

        map = new VariantMap(controller);
        map.addMouseWheelListener(wheelListener);
        LocationController.getInstance().addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                map.repaint();
            }
        });
        JPanel mapPanel = populatePanel(map);
        tabs.addTab("Map", mapPanel);

        frequencyPlot = new AlleleFrequencyPlot(controller);
        frequencyPlot.addMouseWheelListener(wheelListener);
        JPanel frequencyPanel = populatePanel(frequencyPlot);
        tabs.addTab("Allele Frequency", frequencyPanel);
        
        JPanel ldPanel = new JPanel();
        ldPanel.setLayout(new GridBagLayout());

        ActionListener redrawForcer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ldPlot.repaint();
            }
        };
        JRadioButton dPrimeButton = new JRadioButton("D\u2032", true);
        dPrimeButton.setActionCommand("true");
        dPrimeButton.addActionListener(redrawForcer);
        JRadioButton rSquaredButton = new JRadioButton("r\u00B2", false);
        rSquaredButton.setActionCommand("false");
        rSquaredButton.addActionListener(redrawForcer);
        
        methodGroup = new ButtonGroup();
        JPanel methodPanel = new JPanel();
        methodPanel.setBorder(BorderFactory.createTitledBorder("Calculation Method"));
        methodPanel.add(dPrimeButton);
        methodGroup.add(dPrimeButton);
        methodPanel.add(rSquaredButton);
        methodGroup.add(rSquaredButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        ldPanel.add(methodPanel, gbc);

        ldPlot = new LDPlot(controller);
        JPanel lowerLDPanel = populatePanel(ldPlot);
        gbc.weighty = 1.0;
        ldPanel.add(lowerLDPanel, gbc);

        tabs.addTab("LD Plot", ldPanel);

        gbc.weighty = 0.0;
        add(tools, gbc);

        // Create the informative cards, but don't use them.
        messageLabel = new JLabel();
        messageLabel.setFont(MESSAGE_FONT);
        messagePanel = populatePanel(messageLabel);

        progressPanel = new ProgressPanel(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.cancelDataRequests();
            }
        });
        
        showCard(tabs, null);
    }

    /**
     * Three of our panels consist of a component next to a scroll-bar
     */
    private JPanel populatePanel(JComponent content) {
        JScrollBar scroller = new JScrollBar();
        scroller.setMinimum(1);
        scroller.addAdjustmentListener(scrollerListener);
        scrollers.add(scroller);

        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        if (content instanceof JLabel) {
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.insets = MESSAGE_INSETS;
            gbc.fill = GridBagConstraints.HORIZONTAL;
        } else {
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
        }
        container.add(content, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        container.add(scroller, gbc);

        return container;
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
            
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTH;
            if (message != null) {
                if (card == messagePanel) {
                    messageLabel.setText(message);
                } else {
                    gbc.insets = new Insets(20, 20, 20, 20);
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    progressPanel.setMessage(message);
                }
            }
            add(card, gbc);
            currentCard = card;
            repaint();
            validate();
        }
    }
    
    public void showTabs() {
        showCard(tabs, null);
    }

    public void showMessage(String message) {
        showCard(messagePanel, message);
    }
    
    public void showProgress(String message, double fract) {
        showCard(progressPanel, message);
        progressPanel.setFraction(fract);
    }
    
    public void visibleRangeChanged(String ref, Range r) {
        if (r.getLength() > ResolutionSettings.getVariantLowToHighThreshold()) {
            showMessage(ZOOM_MESSAGE);
        } else {
            try {
                // Detach the adjustment listeners so that setting the maximum doesn't fire an event.
                for (JScrollBar sb: scrollers) {
                    sb.removeAdjustmentListener(scrollerListener);
                }

                for (JScrollBar sb: scrollers) {
                    sb.setMaximum(LocationController.getInstance().getMaxRangeEnd());
                    sb.setValue(r.getFrom());
                    sb.setVisibleAmount(r.getLength());
                    sb.setBlockIncrement(r.getLength());
                    sb.repaint();
                }
            } finally {
                // Reattach the adjustment listeners.
                for (JScrollBar sb: scrollers) {
                    sb.addAdjustmentListener(scrollerListener);
                }
            }
        }
        rangeField.setText(String.format("%s:%d-%d", ref, r.getFrom(), r.getTo()));
    }

    public void recalculated(List<VariantRecord> data) {
        table.setModel(new VariantTableModel(data));
        if (data != null && data.size() > 0) {
            showCard(tabs, null);
            map.repaint();
            ldPlot.recalculate();
        } else {
            showMessage("No data in range");
        }
    }
}
