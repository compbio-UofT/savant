/*
 *    Copyright 2010-2011 University of Toronto
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.chromatogram.Chromatogram;
import org.biojava.bio.chromatogram.ChromatogramFactory;
import org.biojava.bio.chromatogram.UnsupportedChromatogramFormatException;

import savant.api.adapter.TrackAdapter;
import savant.api.util.DialogUtils;
import savant.api.util.GenomeUtils;
import savant.api.util.NavigationUtils;
import savant.controller.GenomeController;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangeCompletedListener;
import savant.plugin.SavantPanelPlugin;
import savant.util.Listener;


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

        NavigationUtils.addLocationChangeListener(new LocationChangeCompletedListener() {
            @Override
            public void locationChangeCompleted(LocationChangedEvent event) {
                updateChromatogram();
            }
        });
        GenomeController.getInstance().addListener(new Listener<GenomeChangedEvent>() {
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
        
        JButton browseButton = new JButton("Browse…");
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
