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
package savant.chromatogram;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.chromatogram.Chromatogram;
import org.biojava.bio.chromatogram.ChromatogramFactory;
import org.biojava.bio.chromatogram.UnsupportedChromatogramFormatException;

import savant.api.adapter.TrackAdapter;
import savant.api.event.GenomeChangedEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.GenomeUtils;
import savant.api.util.Listener;
import savant.api.util.NavigationUtils;
import savant.plugin.SavantPanelPlugin;


public class ChromatogramPlugin extends SavantPanelPlugin {
    static final Log LOG = LogFactory.getLog(ChromatogramPlugin.class);

    /** Field which displays currently selected chromatogram file. */
    private JTextField pathField;
            
    /** Fields which display the start and end bases. */
    private JTextField startField, endField;
 
    /** BioJava chromatogram object. */
    private Chromatogram chromatogram;

    /** Canvas on which we draw chromatograms. */
    private ChromatogramCanvas canvas;

    @Override
    public void init(JPanel panel) {

        NavigationUtils.addLocationChangedListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                updateChromatogram();
            }
        });
        GenomeUtils.addGenomeChangedListener(new Listener<GenomeChangedEvent>() {
            @Override
            public void handleEvent(GenomeChangedEvent event) {
                updateChromatogram();
            }
        });
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("File:"), gbc);
        
        pathField = new JTextField();
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;        
        panel.add(pathField, gbc);
        
        JButton browseButton = new JButton("Browse\u2026");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                File f = DialogUtils.chooseFileForOpen("Chromatogram File", null, null);
                if (f != null) {
                    try {
                        pathField.setText(f.getAbsolutePath());
                        if (canvas != null) {
                            canvas.getParent().remove(canvas);
                            canvas = null;
                        }
                        chromatogram = ChromatogramFactory.create(f);
                        updateEndField();
                        updateChromatogram();
                    } catch (UnsupportedChromatogramFormatException x) {
                        DialogUtils.displayMessage("Unable to Open Chromatogram", String.format("<html><i>%s</i> does not appear to be a valid chromatogram file.<br><br><small>Supported formats are ABI and SCF.</small></html>", f.getName()));
                    } catch (Exception x) {
                        DialogUtils.displayException("Unable to Open Chromatogram", String.format("<html>There was an error opening <i>%s</i>.</html>", f.getName()), x);
                    }
                }
            }
        });
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(browseButton, gbc);
        
        JLabel startLabel = new JLabel("Start Base:");
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(startLabel, gbc);
        
        startField = new JTextField("0");
        startField.setColumns(12);
        startField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                updateEndField();
            }
        });
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(startField, gbc);
        
        JLabel endLabel = new JLabel("End Base:");
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(endLabel, gbc);
        
        endField = new JTextField();
        endField.setColumns(12);
        endField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    NumberFormat numberParser = NumberFormat.getIntegerInstance();
                    int endBase = numberParser.parse(endField.getText()).intValue();
                    if (chromatogram != null) {
                        int startBase = endBase - chromatogram.getSequenceLength();
                        startField.setText(String.valueOf(startBase));
                        if (canvas != null) {
                            canvas.updatePos(startBase);
                        }
                    }
                } catch (ParseException x) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(endField, gbc);
        
        JCheckBox fillCheck = new JCheckBox("Fill Background");
        fillCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                canvas.updateFillbackground(((JCheckBox)ae.getSource()).isSelected());
            }
        });
        gbc.gridy++;
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        panel.add(fillCheck, gbc);

        // Add a filler panel at the bottom to force our components to the top.
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);
    }

    @Override
    public String getTitle() {
        return "Chromatogram";
    }

    /**
     * The start field has changed or a chromatogram has been loaded.  Update the end field appropriately.
     */
    private void updateEndField() {
        try {
            NumberFormat numberParser = NumberFormat.getIntegerInstance();
            int startBase = numberParser.parse(startField.getText()).intValue();
            if (chromatogram != null) {
                endField.setText(String.valueOf(startBase + chromatogram.getSequenceLength()));
                if (canvas != null) {
                    canvas.updatePos(startBase);
                }
            }
        } catch (ParseException x) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void updateChromatogram() {
        if (chromatogram != null) {
            if (GenomeUtils.isGenomeLoaded() && GenomeUtils.getGenome().isSequenceSet()) {
                TrackAdapter sequenceTrack = GenomeUtils.getGenome().getSequenceTrack();
                JPanel layerCanvas = sequenceTrack.getLayerCanvas(this);
                if (canvas == null) {
                    layerCanvas.setOpaque(false);
                    layerCanvas.setLayout(new BorderLayout());
                    canvas = new ChromatogramCanvas(chromatogram, sequenceTrack);
                    layerCanvas.add(canvas, BorderLayout.CENTER);
                }
                layerCanvas.revalidate();
                layerCanvas.repaint();
            }
        }
    }
}
