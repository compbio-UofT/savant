/*
 *    Copyright 2010 University of Toronto
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

package savant.snp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

import savant.api.adapter.GenomeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.util.BookmarkUtils;
import savant.api.util.GenomeUtils;
import savant.api.util.NavigationUtils;
import savant.api.util.TrackUtils;
import savant.controller.event.RangeChangeCompletedListener;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.Record;
import savant.file.DataFormat;
import savant.plugin.PluginAdapter;
import savant.plugin.SavantPanelPlugin;
import savant.snp.Pileup.Nucleotide;

public class SNPFinderPlugin extends SavantPanelPlugin
        implements
        RangeChangeCompletedListener,
        TrackListChangedListener {

    // stop looking for SNPs when range is bigger than this number
    private final int MAX_RANGE_TO_SEARCH = 5000;

    // text area to write to
    private JTextArea info;

    // whether the snp finder is on or not
    private boolean isSNPFinderOn = false;
    private boolean addBookmarks = false;
    // sensitivity and transparency
    private int confidence = 10;
    private int transparency = 50;
    private double snpPrior = 0.001;

    // reference sequence in this range
    private byte[] sequence;

    // keep track of canvases
    private Map<TrackAdapter, JPanel> viewTrackToCanvasMap;

    // keep track of piles
    private Map<TrackAdapter, List<Pileup>> viewTrackToPilesMap;

    // keep track of snps in range
    private Map<TrackAdapter, List<Pileup>> viewTrackToSNPsMap;

    // keep track of all snps found
    private Map<TrackAdapter, List<Pileup>> snpsFound;

    /* == INITIALIZATION == */

    /* INITIALIZE THE SNP FINDER */

    @Override
    public void init(JPanel canvas, PluginAdapter pluginAdapter) {
        // subscribe to events
        NavigationUtils.addRangeChangeListener(this);
        TrackUtils.addTracksChangedListener(this);

        // initialize SNP list
        snpsFound = new HashMap<TrackAdapter, List<Pileup>>();
        
        // set up the GUI
        setupGUI(canvas);

        addMessage("SNP finder initialized");
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public String getTitle() {
        return "SNP Finder 2";
    }


    /* INIT THE UI */
    private void setupGUI(JPanel panel) {

        // add a toolbar
        JToolBar tb = new JToolBar();
        tb.setName("SNP Finder Toolbar");

        // add an ON/OFF checkbox
        JLabel lab_on = new JLabel("On/Off: ");
        JCheckBox cb_on = new JCheckBox();
        cb_on.setSelected(isSNPFinderOn);

        // what to do when a user clicks the checkbox
        cb_on.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // switch the SNP finder on/off
                setIsOn(!isSNPFinderOn);
                addMessage("Turning SNP finder " + (isSNPFinderOn ? "on" : "off"));
            }
        });
        // add a  Bookmarking ON/OFF checkbox
        JLabel lab_bm = new JLabel("Add Bookmarks: ");
        JCheckBox cb_bm = new JCheckBox();
        cb_bm.setSelected(addBookmarks);

        // what to do when a user clicks the checkbox
        cb_bm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // switch the SNP finder on/off
                setBookmarking(!addBookmarks);
                addMessage("Turning Bookmarking " + (addBookmarks ? "on" : "off"));
            }
        });

        JLabel lab_sp = new JLabel("Heterozygosity: ");
        	//add snp prior textfield
         final JTextField snpPriorField = new JTextField(String.valueOf(snpPrior), 4);
         snpPriorField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                  setSNPPrior(Double.valueOf(snpPriorField.getText()));
                } catch (NumberFormatException ex) {
                    snpPriorField.setText(String.valueOf(snpPrior));
                }
            }
        });
        int tfwidth = 35;
        int tfheight = 22;
        snpPriorField.setPreferredSize(new Dimension(tfwidth, tfheight));
        snpPriorField.setMaximumSize(new Dimension(tfwidth, tfheight));
        snpPriorField.setMinimumSize(new Dimension(tfwidth, tfheight));

        // add a sensitivity slider
        JLabel lab_confidence = new JLabel("Confidence: ");
        final JSlider sens_slider = new JSlider(0, 50);
        sens_slider.setValue(confidence);
        final JLabel lab_confidence_status = new JLabel("" + sens_slider.getValue());

        // what to do when a user slides the slider
        sens_slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // set the snp finder's sensitivity
                lab_confidence_status.setText("" + sens_slider.getValue());
                setSensitivity(sens_slider.getValue());
            }
        });
        // don't report the new setting until the user stops sliding
        sens_slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed confidence to " + confidence);
            }
        });

        // add a transparency slider
        JLabel lab_trans = new JLabel("Transparency: ");
        final JSlider trans_slider = new JSlider(0, 100);
        trans_slider.setValue(transparency);
        final JLabel lab_transparency_status = new JLabel("" + trans_slider.getValue());

        // what to do when a user slides the slider
        trans_slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // set the snp finder's transparency
                lab_transparency_status.setText("" + trans_slider.getValue());
                setTransparency(trans_slider.getValue());
            }
        });

        // don't report the new setting until the user stops sliding
        trans_slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed transparency to " + transparency);
            }
        });

        // add the components to the GUI
        panel.setLayout(new BorderLayout());
        tb.add(lab_on);
        tb.add(cb_on);
        tb.add(lab_bm);
        tb.add(cb_bm);
        tb.add(new JToolBar.Separator());
        tb.add(lab_sp);
        tb.add(snpPriorField);
        tb.add(new JToolBar.Separator());
        tb.add(lab_confidence);
        tb.add(sens_slider);
        tb.add(lab_confidence_status);

        tb.add(new JToolBar.Separator());

        tb.add(lab_trans);
        tb.add(trans_slider);
        tb.add(lab_transparency_status);

        panel.add(tb, BorderLayout.NORTH);

        // add a text area to the GUI
        info = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(info);
        panel.add(scrollPane, BorderLayout.CENTER);

    }

    /**
     * Add info to the UI.
     */
    private void addMessage(String msg) {
        info.setText(String.format("%s[%tT]    %s\n", info.getText(), new Date(), msg));
        info.setCaretPosition(info.getText().length());
    }

    /**
     * Retrieve canvases.
     *
     * @return
     */
    private List<PileupPanel> getPileupPanels() {
        List<PileupPanel> panels = new ArrayList<PileupPanel>();
        if (viewTrackToCanvasMap != null) {
            for (JPanel p : this.viewTrackToCanvasMap.values()) {
                try {
                    panels.add((PileupPanel) p.getComponent(0));
                } catch (ArrayIndexOutOfBoundsException e) {}
            }
        }
        return panels;
    }

    /**
     * Range change.
     *
     * @param event
     */
    @Override
    public void rangeChangeCompletedReceived(RangeChangedEvent event) {
        // get the reference sequencing in range (if allowed by resolution)
        setSequence();

        // if a sequence could be retreived, run the SNP finder on the
        // data in range
        if (sequence != null) {
            updateTrackCanvasMap();
            runSNPFinder();
        }
    }

    /**
     * Track change.
     */
    @Override
    public void trackListChangeReceived(TrackListChangedEvent event) {
        //updateTrackCanvasMap();
    }

    /**
     * Refresh list of canvases.
     */
    private void updateTrackCanvasMap() {

        if (viewTrackToCanvasMap == null) {
            viewTrackToCanvasMap = new HashMap<TrackAdapter, JPanel>();
        }

        // TODO: should get rid of old JPanels here!
        // START
        for (JPanel p : viewTrackToCanvasMap.values()) {
            p.removeAll();
        }
        viewTrackToCanvasMap.clear();
        // END

        Map<TrackAdapter, JPanel> newmap = new HashMap<TrackAdapter, JPanel>();

        for (TrackAdapter t : TrackUtils.getTracks()) {

            if (t.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {

                if (viewTrackToCanvasMap.containsKey(t)) {
                    newmap.put(t, viewTrackToCanvasMap.get(t));
                    viewTrackToCanvasMap.remove(t);
                } else {
                    //System.out.println("putting " + t.getName() + " in BAM map");
                    if (t.getLayerCanvas() != null) {
                        newmap.put(t, t.getLayerCanvas());
                    }
                }
            }
        }

        viewTrackToCanvasMap = newmap;
    }

    /**
     * Set this SNP finder on/off.
     *
     * @param isOn
     */
    private void setIsOn(boolean isOn) {
        this.isSNPFinderOn = isOn;
        List<PileupPanel> panels = this.getPileupPanels();
        for (PileupPanel p : panels) {
            p.setIsOn(isSNPFinderOn);
        }
        this.repaintPileupPanels();
        this.doEverything();
    }

    private void setBookmarking(boolean isOn) {
        this.addBookmarks = isOn;
        this.doEverything();
    }

    /**
     * Change the sensitivity of the finder.
     */
    private void setSensitivity(int s) {
        confidence = s;
        this.doEverything();
    }

    private void setSNPPrior(double s) {
        snpPrior = s;
        this.doEverything();
    }


    /**
     * Change the transparency of the renderer.
     */
    private void setTransparency(int t) {
        transparency = t;
        List<PileupPanel> panels = this.getPileupPanels();
        for (PileupPanel p : panels) {
            p.setTransparency(this.transparency);
        }
        repaintPileupPanels();
    }

    /**
     * Run the finder.
     */
    private void runSNPFinder() {
        if (isSNPFinderOn) {

            if (NavigationUtils.getCurrentRange().getLength() > MAX_RANGE_TO_SEARCH) {
                addMessage("Won't look for SNPs above range of " + MAX_RANGE_TO_SEARCH + " basepairs");
                return;
            }

            doEverything();
        }
    }

    /**
     * Do everything.
     */
    private void doEverything() {
        if (!isSNPFinderOn)
            return;
        if (sequence == null) {
            setSequence();
        }
        if (sequence != null) {
            updateTrackCanvasMap();
            createPileups();
            callSNPs();
            drawPiles();
        }
    }

    /**
     * Pile up.
     */
    private void createPileups() {

        // clear existing piles
        if (this.viewTrackToPilesMap != null) {
            viewTrackToPilesMap.clear();
        }
        // create a new map (key=track, value=list of piles)
        viewTrackToPilesMap = new HashMap<TrackAdapter, List<Pileup>>();

        // for each track
        for (TrackAdapter t : viewTrackToCanvasMap.keySet()) {
            try {
                // get the genomic start position of the current range
                int startPosition = NavigationUtils.getCurrentRange().getFrom();

                // make piles from SAM records in this track
                List<Pileup> piles = makePileupsFromSAMRecords(t.getName(), t.getDataInRange(), sequence, startPosition);

                // save the piles
                this.viewTrackToPilesMap.put(t, piles);

            // report errors and move on
            } catch (IOException ex) {
                addMessage("Error: " + ex.getMessage());
                break;
            }
        }
    }

    /**
     * Make pileups for SAM records.
     */
    private List<Pileup> makePileupsFromSAMRecords(String viewTrackName, List<Record> samRecords, byte[] sequence, int startPosition) throws IOException {

        // list of pileups, one per genomic position in range
        List<Pileup> pileups = new ArrayList<Pileup>();

        // initialize the piles
        int length = sequence.length;
        for (int i = 0; i < length; i++) {
            pileups.add(new Pileup(viewTrackName, startPosition + i, Pileup.getNucleotide(sequence[i])));
        }

        // go through all the samrecords and update the pileups
        for (Record r : samRecords) {
            SAMRecord sr = ((BAMIntervalRecord)r).getSamRecord();
            updatePileupsFromSAMRecord(pileups, GenomeUtils.getGenome(), sr, startPosition);
        }

        return pileups;
    }

    private void updatePileupsFromSAMRecord(List<Pileup> pileups, GenomeAdapter genome, SAMRecord samRecord, long startPosition) throws IOException {

        // the start and end of the alignment
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        // the read sequence
        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0; // true iff read sequence is set

   
        byte[] baseQualities = samRecord.getBaseQualities();

        // return if no bases (can't be used for SNP calling)
        if (!sequenceSaved) {
            return;
        }

        // the reference sequence
        byte[] refSeq = genome.getSequence(NavigationUtils.getCurrentReferenceName(), NavigationUtils.createRange(alignmentStart, alignmentEnd));

        // get the cigar object for this alignment
        Cigar cigar = samRecord.getCigar();

        // set cursors for the reference and read
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        int pileupcursor = (int) (alignmentStart - startPosition);

        // cigar variables
        CigarOperator operator;
        int operatorLength;

        // consider each cigar element
        for (CigarElement cigarElement : cigar.getCigarElements()) {

            operatorLength = cigarElement.getLength();
            operator = cigarElement.getOperator();

            // delete
            if (operator == CigarOperator.D) {}
            // insert
            else if (operator == CigarOperator.I) {}
            // match **or mismatch**
            else if (operator == CigarOperator.M) {

                // some SAM files do not contain the read bases
                if (sequenceSaved) {

                    // determine if there's a mismatch
                    for (int i = 0; i < operatorLength; i++) {

                        int refIndex = sequenceCursor - alignmentStart + i;
                        int readIndex = readCursor - alignmentStart + i;

                        // get the base of the read at this position
                        byte[] readBase = new byte[1];
                        readBase[0] = readBases[readIndex];

                        /*
                         * MIKE: use this base quality to update pileup
                         * via the 'pileOn' method.
                         */
                        byte[] baseQ = new byte[1];
                        baseQ[0] = baseQualities[readIndex];

                        // get the nucleotide corresponding the the readbase
                        Nucleotide readN = Pileup.getNucleotide(readBase[0]);

                        // adjust the pileup for this position, if it lies in the
                        // current window
                        int j = i + (int) (alignmentStart - startPosition);
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN, baseQ[0]);
                        }
                    }
                }
            } // skipped
            else if (operator == CigarOperator.N) {
                // draw nothing
            } // padding
            else if (operator == CigarOperator.P) {
                // draw nothing
            } // hard clip
            else if (operator == CigarOperator.H) {
                // draw nothing
            } // soft clip
            else if (operator == CigarOperator.S) {
                // draw nothing
            }

            if (operator.consumesReadBases()) {
                readCursor += operatorLength;
            }
            if (operator.consumesReferenceBases()) {
                sequenceCursor += operatorLength;
                pileupcursor += operatorLength;
            }
        }
    }

    /* CALL SNPS FOR ALL VIEWTRACKS*/
    private void callSNPs() {

        if (this.viewTrackToSNPsMap != null) { this.viewTrackToSNPsMap.clear(); }
        this.viewTrackToSNPsMap = new HashMap<TrackAdapter, List<Pileup>>();

        for (TrackAdapter t : viewTrackToCanvasMap.keySet()) {
            List<Pileup> piles = viewTrackToPilesMap.get(t);
            List<Pileup> snps = callSNPsFromPileups(piles, sequence);
            this.viewTrackToSNPsMap.put(t, snps);
            addFoundSNPs(t, snps);
        }
    }

    /**
     * Call SNP for piles for current sequence.
     */
    private List<Pileup> callSNPsFromPileups(List<Pileup> piles, byte[] sequence) {

        //addMessage("Calling SNPs");

        List<Pileup> snps = new ArrayList<Pileup>();

        int length = sequence.length;
        Pileup.Nucleotide n;
        Pileup p;
        for (int i = 0; i < length; i++) {
            n = Pileup.getNucleotide(sequence[i]);
            p = piles.get(i);
            //System.out.println ("I am here");
            if (p.getSNPNucleotide(snpPrior) != null) {
              //  System.out.println ("I am inside");
                double snpConfidence = -10 * Math.log10(1-p.getSNPNucleotideConfidence(snpPrior));
                double z = this.confidence; //-Math.log(1 - ((double)this.confidence)/50.0);

                // criteria for calling snps
                if (snpConfidence > z) {
                    snps.add(p);
                }
                System.out.println("conf " + p.getSNPNucleotideConfidence(snpPrior) + "logconf " + snpConfidence + " sens "+ z);
            }
        }

        addMessage(snps.size() + " SNPs found");

        return snps;
    }

    /* == RENDERING == */

    /**
     * Repaint canvases (e.g. because of change in transparency).
     */
    private void repaintPileupPanels() {

        List<PileupPanel> canvases = this.getPileupPanels();

        for (PileupPanel c : canvases) {
            c.repaint();
            c.revalidate();
        }
    }

    /**
     * Draw piles on panels for all ViewTracks.
     */
    private void drawPiles() {

        //lastViewTrackToCanvasMapDrawn = viewTrackToCanvasMap;

        //System.out.println("Drawing annotations");

        for (TrackAdapter t : viewTrackToSNPsMap.keySet()) {
            List<Pileup> pile = viewTrackToSNPsMap.get(t);
            drawPiles(pile, viewTrackToCanvasMap.get(t));
        }

        //System.out.println("Done drawing annotations");
    }

    /**
     * Draw piles on panel.
     */
    private void drawPiles(List<Pileup> piles, JPanel p) {

        p.removeAll();
        PileupPanel pup = new PileupPanel(piles);
        pup.setTransparency(this.transparency);

        p.setLayout(new BorderLayout());
        p.add(pup, BorderLayout.CENTER);

        this.repaintPileupPanels();
    }

    /**
     * Set reference sequence.
     */
    private void setSequence() {
        sequence = null;
        if (GenomeUtils.isGenomeLoaded()) {
            try {
                sequence = GenomeUtils.getGenome().getSequence(NavigationUtils.getCurrentReferenceName(), NavigationUtils.getCurrentRange());
            } catch (IOException ex) {
                addMessage("Error: could not get sequence");
                return;
            }
        } else {
            addMessage("Error: no reference sequence loaded");
            return;
        }
    }

    private boolean foundSNPAlready(TrackAdapter t, Pileup p) {
        if (snpsFound.containsKey(t) && snpsFound.get(t).contains(p)) {
            //System.out.println(p.getPosition() + " found already");
            return true;
        } else {
            //System.out.println(p.getPosition() + " is new");
            return false;
        }
    }

    private void addFoundSNPs(TrackAdapter t, List<Pileup> snps) {
        for (Pileup snp : snps) {
            addFoundSNP(t,snp);
        }
    }

    private void addFoundSNP(TrackAdapter t, Pileup snp) {
        if (!this.foundSNPAlready(t, snp)) {
            if (!snpsFound.containsKey(t)) {
                List<Pileup> snps = new ArrayList<Pileup>();
                snpsFound.put(t, snps);
            }
            if (addBookmarks) {
                BookmarkUtils.addBookmark(BookmarkUtils.createBookmark(NavigationUtils.getCurrentReferenceName(), NavigationUtils.createRange(snp.getPosition(), snp.getPosition()),
                    snp.getSNPNucleotide() + "/" + snp.getReferenceNucleotide()
                    + " SNP "
                    + (int) snp.getCoverage(snp.getSNPNucleotide()) + "/" + (int) snp.getCoverage(snp.getReferenceNucleotide())
                    + "; Conf = " + shortenPercentage(snp.getSNPNucleotideConfidence(snpPrior))
                    + " in " + t.getName()));
            }
            snpsFound.get(t).add(snp);
        }
    }

    private String shortenPercentage(double p) {
        String s = ((int) Math.round(p*100)) + "";
        return s + "%";
    }
}
