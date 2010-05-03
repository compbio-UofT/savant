/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GenomeLengthForm.java
 *
 * Created on Apr 8, 2010, 4:49:19 PM
 */

package savant.view.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author mfiume
 */
public class GenomeLengthForm extends javax.swing.JDialog {

    public boolean isLengthSet = false;
    public int length;

    private void initGenomeInformation() {
        genomeInformation = new ArrayList<SpeciesInfo>();
        genomeInformation.add(getHumanGenomeInformation());
        genomeInformation.add(getMouseGenomeInformation());
    }

    private SpeciesInfo getMouseGenomeInformation() {
        SpeciesInfo mouseInfo = new SpeciesInfo("Mouse");

        BuildInfo build37 = new BuildInfo("Build 37");
        mouseInfo.addBuildInfo(build37);
        build37.addChromosomeInfo(new ChromosomeInfo("chr1",197195432));
        build37.addChromosomeInfo(new ChromosomeInfo("chr2",181748087));
        build37.addChromosomeInfo(new ChromosomeInfo("chr3",159599783));
        build37.addChromosomeInfo(new ChromosomeInfo("chr4",155630120));
        build37.addChromosomeInfo(new ChromosomeInfo("chr5",152537259));
        build37.addChromosomeInfo(new ChromosomeInfo("chr6",149517037));
        build37.addChromosomeInfo(new ChromosomeInfo("chr7",152524553));
        build37.addChromosomeInfo(new ChromosomeInfo("chr8",131738871));
        build37.addChromosomeInfo(new ChromosomeInfo("chr9",124076172));
        build37.addChromosomeInfo(new ChromosomeInfo("chr10",129993255));
        build37.addChromosomeInfo(new ChromosomeInfo("chr11",121843856));
        build37.addChromosomeInfo(new ChromosomeInfo("chr12",121257530));
        build37.addChromosomeInfo(new ChromosomeInfo("chr13",120284312));
        build37.addChromosomeInfo(new ChromosomeInfo("chr14",125194864));
        build37.addChromosomeInfo(new ChromosomeInfo("chr15",103494974));
        build37.addChromosomeInfo(new ChromosomeInfo("chr16",98319150));
        build37.addChromosomeInfo(new ChromosomeInfo("chr17",95272651));
        build37.addChromosomeInfo(new ChromosomeInfo("chr18",90772031));
        build37.addChromosomeInfo(new ChromosomeInfo("chr19",61342430));
        build37.addChromosomeInfo(new ChromosomeInfo("chrX",166650296));
        build37.addChromosomeInfo(new ChromosomeInfo("chrY",15902555));

        BuildInfo build36 = new BuildInfo("Build 36");
        mouseInfo.addBuildInfo(build36);
        build36.addChromosomeInfo(new ChromosomeInfo("chr1",197069962));
        build36.addChromosomeInfo(new ChromosomeInfo("chr2",181976762));
        build36.addChromosomeInfo(new ChromosomeInfo("chr3",159872112));
        build36.addChromosomeInfo(new ChromosomeInfo("chr4",155029701));
        build36.addChromosomeInfo(new ChromosomeInfo("chr5",152003063));
        build36.addChromosomeInfo(new ChromosomeInfo("chr6",149525685));
        build36.addChromosomeInfo(new ChromosomeInfo("chr7",145134094));
        build36.addChromosomeInfo(new ChromosomeInfo("chr8",132085098));
        build36.addChromosomeInfo(new ChromosomeInfo("chr9",124000669));
        build36.addChromosomeInfo(new ChromosomeInfo("chr10",129959148));
        build36.addChromosomeInfo(new ChromosomeInfo("chr11",121798632));
        build36.addChromosomeInfo(new ChromosomeInfo("chr12",120463159));
        build36.addChromosomeInfo(new ChromosomeInfo("chr13",120614378));
        build36.addChromosomeInfo(new ChromosomeInfo("chr14",123978870));
        build36.addChromosomeInfo(new ChromosomeInfo("chr15",103492577));
        build36.addChromosomeInfo(new ChromosomeInfo("chr16",98252459));
        build36.addChromosomeInfo(new ChromosomeInfo("chr17",95177420));
        build36.addChromosomeInfo(new ChromosomeInfo("chr18",90736837));
        build36.addChromosomeInfo(new ChromosomeInfo("chr19",61321190));
        build36.addChromosomeInfo(new ChromosomeInfo("chrX",165556469));
        build36.addChromosomeInfo(new ChromosomeInfo("chrY",16029404));


        return mouseInfo;
    }

    private SpeciesInfo getHumanGenomeInformation() {
        SpeciesInfo humanInfo = new SpeciesInfo("Human");

        BuildInfo hg19BuildInfo = new BuildInfo("hg19");
        humanInfo.addBuildInfo(hg19BuildInfo);
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr1",249250621));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr2",243199373));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr3",198022430));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr4",191154276));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr5",180915260));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr6",171115067));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr7",159138663));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr8",146364022));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr9",141213431));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr10",135534747));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr11",135006516));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr12",133851895));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr13",115169878));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr14",107349540));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr15",102531392));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr16",90354753));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr17",81195210));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr18",78077248));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr19",59128983));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr20",63025520));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr21",48129895));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr22",51304566));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrX",155270560));
        hg19BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrY",59373566));

        BuildInfo hg18BuildInfo = new BuildInfo("hg18");
        humanInfo.addBuildInfo(hg18BuildInfo);
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr1",247249719));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr2",242951149));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr3",199501827));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr4",191273063));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr5",180857866));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr6",170899992));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr7",158821424));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr8",146274826));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr9",140273252));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr10",135374737));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr11",134452384));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr12",132349534));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr13",114142980));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr14",106368585));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr15",100338915));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr16",88827254));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr17",78774742));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr18",76117153));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr19",63811651));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr20",62435964));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr21",46944323));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr22",49691432));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrX",154913754));
        hg18BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrY",57772954));

        BuildInfo hg17BuildInfo = new BuildInfo("hg17");
        humanInfo.addBuildInfo(hg17BuildInfo);
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr1",245442847));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr2",242818229));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr3",199450740));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr4",191401218));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr5",180837866));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr6",170972699));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr7",158628139));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr8",146274826));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr9",138429268));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr10",135413628));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr11",134452384));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr12",132389811));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr13",114127980));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr14",106360585));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr15",100338915));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr16",88822254));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr17",78654742));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr18",76117153));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr19",63806651));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr20",62435964));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr21",46944323));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chr22",49534710));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrX",154824264));
        hg17BuildInfo.addChromosomeInfo(new ChromosomeInfo("chrY",57701691));

        return humanInfo;
    }

    private void initDropDowns() {
        updateSpeciesList();

        this.dropdown_species.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                updateBuildList();
            }
        });

        this.dropdown_build.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                updateChromosomeList();
            }
        });
    }

    private void updateSpeciesList() {
        this.dropdown_species.removeAllItems();
        for (SpeciesInfo si : this.genomeInformation) {
            this.dropdown_species.addItem(si);
        }
        this.dropdown_species.setSelectedIndex(0);
        updateBuildList();
    }

    private void updateBuildList() {
        if (this.dropdown_species.getItemCount() == 0) { return; }
        SpeciesInfo si = (SpeciesInfo) this.dropdown_species.getSelectedItem();
        if (si == null) { si = (SpeciesInfo) this.dropdown_species.getItemAt(0); }
        this.dropdown_build.removeAllItems();
        for (BuildInfo bi : si.builds) {
            this.dropdown_build.addItem(bi);
        }
        this.dropdown_build.setSelectedIndex(0);
        updateChromosomeList();
    }

    private void updateChromosomeList() {
        if (this.dropdown_build.getItemCount() == 0) { return; }
        BuildInfo bi = (BuildInfo) this.dropdown_build.getSelectedItem();
        if (bi == null) { bi = (BuildInfo) this.dropdown_build.getItemAt(0); }
        this.dropdown_chromosome.removeAllItems();
        for (ChromosomeInfo ci : bi.chromosomes) {
            this.dropdown_chromosome.addItem(ci);
        }
        this.dropdown_chromosome.setSelectedIndex(0);
    }

    public class SpeciesInfo {
        public String name;
        public List<BuildInfo> builds;
        public SpeciesInfo(String name) {
            this.name = name;
            this.builds = new ArrayList<BuildInfo>();
        }
        public SpeciesInfo(String name, List<BuildInfo> builds) {
            this.name = name;
            this.builds = builds;
        }
        public void addBuildInfo(BuildInfo bi) {
            this.builds.add(bi);
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    public class BuildInfo {
        public String name;
        public List<ChromosomeInfo> chromosomes;
        public BuildInfo(String name) {
            this.name = name;
            this.chromosomes = new ArrayList<ChromosomeInfo>();
        }
        public BuildInfo(String name, List<ChromosomeInfo> chromosomes) {
            this.name = name;
            this.chromosomes = chromosomes;
        }
        public void addChromosomeInfo(ChromosomeInfo ci) {
            this.chromosomes.add(ci);
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    public class ChromosomeInfo {
        public String name;
        public int length;
        public ChromosomeInfo (String name, int length) {
            this.name = name;
            this.length = length;
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    List<SpeciesInfo> genomeInformation;

    /** Creates new form GenomeLengthForm */
    public GenomeLengthForm(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        initGenomeInformation();
        initDropDowns();
        updateEnabledControls();
        this.setVisible(true);
    }

    private void updateEnabledControls() {
        setPublishedGenomeControlsEnabled(this.radio_commongenome.isSelected());
        setUserSpecifiedControlsEnabled(this.radio_userspecified.isSelected());
    }

    private void setPublishedGenomeControlsEnabled(boolean isEnabled) {
        this.dropdown_species.setEnabled(isEnabled);
        this.dropdown_build.setEnabled(isEnabled);
        this.dropdown_chromosome.setEnabled(isEnabled);
    }

    private void setUserSpecifiedControlsEnabled(boolean isEnabled) {
        this.textfield_length.setEnabled(isEnabled);
    }

    private boolean validateUserSpecifiedLength() {
        String text = this.textfield_length.getText();
        try {
            int i = Integer.parseInt(text);
            if (i <= 0) {
                throw new Exception();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setLength() {

        if (this.radio_commongenome.isSelected()) {

            this.length = ((ChromosomeInfo) this.dropdown_chromosome.getSelectedItem()).length;

        } else if (this.radio_userspecified.isSelected()) {

            this.length = Integer.parseInt(this.textfield_length.getText());
        }

        this.isLengthSet = true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttongroup_whichkind = new javax.swing.ButtonGroup();
        radio_commongenome = new javax.swing.JRadioButton();
        radio_userspecified = new javax.swing.JRadioButton();
        dropdown_species = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        dropdown_build = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        dropdown_chromosome = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        textfield_length = new javax.swing.JTextField();
        button_ok = new javax.swing.JButton();
        button_cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        buttongroup_whichkind.add(radio_commongenome);
        radio_commongenome.setSelected(true);
        radio_commongenome.setText("Published Genome");
        radio_commongenome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radio_commongenomeActionPerformed(evt);
            }
        });

        buttongroup_whichkind.add(radio_userspecified);
        radio_userspecified.setText("User-specified");
        radio_userspecified.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radio_userspecifiedActionPerformed(evt);
            }
        });

        dropdown_species.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel1.setText("Species:");

        jLabel2.setText("Build:");

        dropdown_build.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel3.setText("Chromosome:");

        dropdown_chromosome.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel4.setText("Length:");

        textfield_length.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                textfield_lengthFocusLost(evt);
            }
        });

        button_ok.setText("OK");
        button_ok.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button_okMouseClicked(evt);
            }
        });

        button_cancel.setText("Cancel");
        button_cancel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button_cancelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(jLabel1)
                                .addGap(30, 30, 30)
                                .addComponent(dropdown_species, 0, 209, Short.MAX_VALUE))
                            .addComponent(radio_commongenome)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dropdown_build, 0, 209, Short.MAX_VALUE)
                            .addComponent(dropdown_chromosome, 0, 209, Short.MAX_VALUE))))
                .addGap(10, 10, 10))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel4)
                        .addGap(37, 37, 37)
                        .addComponent(textfield_length, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE))
                    .addComponent(radio_userspecified))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(188, Short.MAX_VALUE)
                .addComponent(button_ok)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(button_cancel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(radio_commongenome)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(dropdown_species, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(dropdown_build, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(dropdown_chromosome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(radio_userspecified)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textfield_length, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_cancel)
                    .addComponent(button_ok))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void radio_userspecifiedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radio_userspecifiedActionPerformed
        updateEnabledControls();
    }//GEN-LAST:event_radio_userspecifiedActionPerformed

    private void radio_commongenomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radio_commongenomeActionPerformed
        updateEnabledControls();
    }//GEN-LAST:event_radio_commongenomeActionPerformed

    private void textfield_lengthFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textfield_lengthFocusLost
        validateUserSpecifiedLength();
    }//GEN-LAST:event_textfield_lengthFocusLost

    private void button_cancelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button_cancelMouseClicked
        this.setVisible(false);
    }//GEN-LAST:event_button_cancelMouseClicked

    private void button_okMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button_okMouseClicked

        if (this.radio_userspecified.isSelected() && !validateUserSpecifiedLength()) { 
            JOptionPane.showMessageDialog(this, "Invalid length.");
            this.textfield_length.requestFocus();
            return;
        }
        
        setLength();
        this.setVisible(false);
    }//GEN-LAST:event_button_okMouseClicked

    public GenomeLengthForm() {
        this(new javax.swing.JFrame(), true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_ok;
    private javax.swing.ButtonGroup buttongroup_whichkind;
    private javax.swing.JComboBox dropdown_build;
    private javax.swing.JComboBox dropdown_chromosome;
    private javax.swing.JComboBox dropdown_species;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JRadioButton radio_commongenome;
    private javax.swing.JRadioButton radio_userspecified;
    private javax.swing.JTextField textfield_length;
    // End of variables declaration//GEN-END:variables


}
