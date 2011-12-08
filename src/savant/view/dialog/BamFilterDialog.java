/*
 *    Copyright 2009-2011 University of Toronto
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
package savant.view.dialog;

import java.awt.*;
import javax.swing.JTextField;

import savant.controller.LocationController;
import savant.data.filters.BAMRecordFilter;
import savant.util.MiscUtils;
import savant.util.SAMReadUtils;
import savant.view.tracks.BAMTrack;


/**
 *
 * @author vwilliam
 */
public class BamFilterDialog extends javax.swing.JDialog {
    private static final int DEFAULT_DISCORDANT_MIN = 50;
    private static final int DEFAULT_DISCORDANT_MAX = 1000;
    private static final int DEFAULT_ARC_YMAX_THRESHOLD = 10000;

    private final BAMTrack track;
    private final BAMRecordFilter filter;
    private JTextField errField = null;

    /** Creates new form BamFilterDialog */
    public BamFilterDialog(Window parent, BAMTrack t) {
        super(parent, Dialog.ModalityType.APPLICATION_MODAL);
        initComponents();
        track = t;
        filter = t.getFilter();

        duplicateReadsCheck.setSelected(filter.getIncludeDuplicateReads());
        vendorFailedReadsCheck.setSelected(filter.getIncludeVendorFailedReads());
        mappingQualitySlider.setValue(filter.getMappingQualityThreshold());

        double arcThreshold = filter.getArcLengthThreshold();
        if (arcThreshold < 1.0 && arcThreshold > 0.0) {
            arcThresholdField.setText(String.format("%d%%", (int)(arcThreshold * 100.0)));
        } else {
            arcThresholdField.setText(String.valueOf((int)arcThreshold));
        }
        discordantMinField.setText(String.valueOf(t.getConcordantMin()));
        discordantMaxField.setText(String.valueOf(t.getConcordantMax()));
        arcYMaxThresholdField.setText(String.valueOf(t.getMaxBPForYMax()));
        if (t.getPairedProtocol() == SAMReadUtils.PairedSequencingProtocol.PAIREDEND) {
            pairedEndRadio.setSelected(true);
        } else {
            oppositeRadio.setSelected(true);
        }

        getRootPane().setDefaultButton(okButton);
        MiscUtils.registerCancelButton(cancelButton);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        duplicateReadsCheck = new javax.swing.JCheckBox();
        vendorFailedReadsCheck = new javax.swing.JCheckBox();
        javax.swing.JLabel mappingQualityLabel = new javax.swing.JLabel();
        mappingQualitySlider = new javax.swing.JSlider();
        jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel arcYMaxThresholdLabel = new javax.swing.JLabel();
        javax.swing.JLabel oppositeIcon = new javax.swing.JLabel();
        javax.swing.JLabel minConcordantLabel = new javax.swing.JLabel();
        oppositeRadio = new javax.swing.JRadioButton();
        javax.swing.JLabel maxConcordantLabel = new javax.swing.JLabel();
        arcYMaxThresholdField = new javax.swing.JTextField();
        javax.swing.JLabel arcThresholdLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        discordantMinField = new javax.swing.JTextField();
        discordantMaxField = new javax.swing.JTextField();
        javax.swing.JLabel pairedEndIcon = new javax.swing.JLabel();
        arcThresholdField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        pairedEndRadio = new javax.swing.JRadioButton();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel pairLabel = new javax.swing.JLabel();

        buttonGroup1.add(pairedEndRadio);
        buttonGroup1.add(oppositeRadio);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Read Pair Settings");

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("General Filters"));

        duplicateReadsCheck.setSelected(true);
        duplicateReadsCheck.setText("Include duplicate reads");

        vendorFailedReadsCheck.setSelected(true);
        vendorFailedReadsCheck.setText("Include vendor failed reads");

        mappingQualityLabel.setText("Mapping quality threshold");

        mappingQualitySlider.setMajorTickSpacing(50);
        mappingQualitySlider.setMaximum(255);
        mappingQualitySlider.setPaintLabels(true);
        mappingQualitySlider.setValue(0);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(duplicateReadsCheck)
                    .addComponent(vendorFailedReadsCheck)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(mappingQualityLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mappingQualitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(duplicateReadsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(vendorFailedReadsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(mappingQualitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mappingQualityLabel))
                .addGap(44, 44, 44))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Arc Mode Settings"));

        arcYMaxThresholdLabel.setText("Don't adjust ymax for arcs larger than:");

        oppositeIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/reads_opposite.png"))); // NOI18N

        minConcordantLabel.setText("Min concordant insert size:");

        oppositeRadio.setSelected(true);
        oppositeRadio.setText("opposite strands");

        maxConcordantLabel.setText("Max concordant insert size:");

        arcThresholdLabel.setText("Ignore sizes smaller than:");

        jLabel2.setText("bp");

        pairedEndIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/reads_same.png"))); // NOI18N

        arcThresholdField.setToolTipText("Either enter an absolute number of base pairs, or a percentage of ymax");

        jLabel1.setText("bp");

        jLabel4.setText("bp");

        pairedEndRadio.setText("same strand");

        jLabel3.setText("bp or %");

        pairLabel.setText("Pairs are sequenced from:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pairLabel)
                    .addComponent(minConcordantLabel)
                    .addComponent(maxConcordantLabel)
                    .addComponent(arcYMaxThresholdLabel)
                    .addComponent(arcThresholdLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(oppositeRadio)
                            .addComponent(pairedEndRadio))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pairedEndIcon)
                            .addComponent(oppositeIcon)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(arcYMaxThresholdField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(arcThresholdField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(discordantMaxField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(discordantMinField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jLabel4))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(oppositeIcon)
                            .addComponent(oppositeRadio))
                        .addGap(5, 5, 5)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(pairedEndIcon)
                            .addComponent(pairedEndRadio))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(discordantMinField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(minConcordantLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(discordantMaxField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(maxConcordantLabel)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(arcThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(arcThresholdLabel)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(arcYMaxThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(arcYMaxThresholdLabel)
                            .addComponent(jLabel4)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(pairLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            SAMReadUtils.PairedSequencingProtocol prot = parseProtocol();
            int discordantMin = parseField(discordantMinField, DEFAULT_DISCORDANT_MIN);
            int discordantMax = parseField(discordantMaxField, DEFAULT_DISCORDANT_MAX);
            double arcThreshold = parseArcThreshold();
            int maxBPForYMax = parseField(arcYMaxThresholdField, DEFAULT_ARC_YMAX_THRESHOLD);

            // Everything parsed okay, so update the filter and track properties.
            filter.setArcLengthThreshold(arcThreshold);
            filter.setIncludeDuplicateReads(duplicateReadsCheck.isSelected());
            filter.setIncludeVendorFailedReads(vendorFailedReadsCheck.isSelected());
            filter.setMappingQualityThreshold(mappingQualitySlider.getValue());
            track.setPairedProtocol(prot);
            track.setConcordantMin(discordantMin);
            track.setConcordantMax(discordantMax);
            track.setMaxBPForYMax(maxBPForYMax);
            setVisible(false);
            track.prepareForRendering(LocationController.getInstance().getReferenceName() , LocationController.getInstance().getRange());
            track.repaint();
        } catch (NumberFormatException e) {
            Toolkit.getDefaultToolkit().beep();
            errField.setText("");
            errField.grabFocus();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private SAMReadUtils.PairedSequencingProtocol parseProtocol() {
        return pairedEndRadio.isSelected() ? SAMReadUtils.PairedSequencingProtocol.PAIREDEND : SAMReadUtils.PairedSequencingProtocol.MATEPAIR;
    }

    private int parseField(JTextField f, int dflt) throws NumberFormatException {
        errField = f;
        String str = f.getText();
        return str.equals("") ? dflt : Integer.parseInt(str);
    }

    /**
     * Parsing the arc threshold is more complicated, because it can be either an absolute number or a percentage.
     */
    private double parseArcThreshold() {
        errField = arcThresholdField;
        double result;
        String threshStr = arcThresholdField.getText();
        if (threshStr.equals("")) {
            result = BAMRecordFilter.DEFAULT_ARC_LENGTH_THRESHOLD;
        } else {
            if (threshStr.endsWith("%")) {
                // It's a percentage value.  Note that if they type in a percentage greater than 100, it
                // will be misinterpreted as an absolute number of bps.
                result = Double.parseDouble(threshStr.substring(0, threshStr.length() - 1)) * 0.01;
                if (result >= 1.0) {
                    throw new NumberFormatException();
                }
            } else {
                result = Double.parseDouble(threshStr);
            }
        }
        return result;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField arcThresholdField;
    private javax.swing.JTextField arcYMaxThresholdField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField discordantMaxField;
    private javax.swing.JTextField discordantMinField;
    private javax.swing.JCheckBox duplicateReadsCheck;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSlider mappingQualitySlider;
    private javax.swing.JButton okButton;
    private javax.swing.JRadioButton oppositeRadio;
    private javax.swing.JRadioButton pairedEndRadio;
    private javax.swing.JCheckBox vendorFailedReadsCheck;
    // End of variables declaration//GEN-END:variables
}
