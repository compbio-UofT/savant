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
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

public class SNPFinderPlugin extends SavantPanelPlugin implements RangeChangeCompletedListener, TrackListChangedListener {
    private static final Log LOG = LogFactory.getLog(SNPFinderPlugin.class);

    private final int MAX_RANGE_TO_SEARCH = 5000;
    private JTextArea info;
    private boolean isSNPFinderOn = true;
    private int sensitivity = 80;
    private int transparency = 50;
    private byte[] sequence;

    private Map<TrackAdapter, JPanel> trackToCanvasMap;
    private Map<TrackAdapter, List<Pileup>> trackToPilesMap;
    private Map<TrackAdapter, List<Pileup>> trackToSNPsMap;
    private Map<TrackAdapter, List<Pileup>> snpsFound;



    //private Map<Track,JPanel> lastTrackToCanvasMapDrawn;

    /* == INITIALIZATION == */

    /* INITIALIZE THE SNP FINDER */

    @Override
    public void init(JPanel canvas, PluginAdapter pluginAdapter) {
        NavigationUtils.addRangeChangeListener(this);
        TrackUtils.addTracksChangedListener(this);
        snpsFound = new HashMap<TrackAdapter, List<Pileup>>();
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
        return "SNP Finder";
    }


    /* INIT THE UI */
    private void setupGUI(JPanel panel) {

        JToolBar tb = new JToolBar();
        tb.setName("SNP Finder Toolbar");

        JLabel lab_on = new JLabel("On: ");
        JCheckBox cb_on = new JCheckBox();
        cb_on.setSelected(isSNPFinderOn);
        cb_on.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setIsOn(!isSNPFinderOn);
                addMessage("Turning SNP finder " + (isSNPFinderOn ? "on" : "off"));
            }
        });

        JLabel lab_sensitivity = new JLabel("Sensitivity: ");
        final JSlider sens_slider = new JSlider(0, 100);
        sens_slider.setValue(sensitivity);
        final JLabel lab_sensitivity_status = new JLabel("" + sens_slider.getValue());
        sens_slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                lab_sensitivity_status.setText("" + sens_slider.getValue());
                setSensitivity(sens_slider.getValue());
            }
        });
        sens_slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed sensitivity to " + sensitivity);
            }
        });

        JLabel lab_trans = new JLabel("Transparency: ");
        final JSlider trans_slider = new JSlider(0, 100);
        trans_slider.setValue(transparency);
        final JLabel lab_transparency_status = new JLabel("" + trans_slider.getValue());
        trans_slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                lab_transparency_status.setText("" + trans_slider.getValue());
                setTransparency(trans_slider.getValue());
            }
        });
        trans_slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed transparency to " + transparency);
            }
        });


        panel.setLayout(new BorderLayout());
        tb.add(lab_on);
        tb.add(cb_on);
        tb.add(new JToolBar.Separator());

        tb.add(lab_sensitivity);
        tb.add(sens_slider);
        tb.add(lab_sensitivity_status);

        tb.add(new JToolBar.Separator());

        tb.add(lab_trans);
        tb.add(trans_slider);
        tb.add(lab_transparency_status);

        panel.add(tb, BorderLayout.NORTH);

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
        if (trackToCanvasMap != null) {
            for (JPanel p : this.trackToCanvasMap.values()) {
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
        setSequence();
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

        if (trackToCanvasMap == null) {
            trackToCanvasMap = new HashMap<TrackAdapter, JPanel>();
        }

        // TODO: should get rid of old JPanels here!
        // START
        for (JPanel p : trackToCanvasMap.values()) {
            p.removeAll();
        }
        trackToCanvasMap.clear();
        // END

        Map<TrackAdapter, JPanel> newmap = new HashMap<TrackAdapter, JPanel>();

        for (TrackAdapter t : TrackUtils.getTracks()) {

            if (t.getDataSource().getDataFormat() == DataFormat.INTERVAL_BAM) {

                if (trackToCanvasMap.containsKey(t)) {
                    newmap.put(t, trackToCanvasMap.get(t));
                    trackToCanvasMap.remove(t);
                } else {
                    //System.out.println("putting " + t.getName() + " in BAM map");
                    newmap.put(t, t.getLayerCanvas());
                }
            }
        }

        trackToCanvasMap = newmap;
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
    }

    /**
     * Change the sensitivity of the finder.
     */
    private void setSensitivity(int s) {
        sensitivity = s;
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

            //System.out.println("checking for snps");
            doEverything();
        }

        //System.out.println("done checking for snps");
    }

    /**
     * Do everything.
     */
    private void doEverything() {
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

        if (this.trackToPilesMap != null) {
            trackToPilesMap.clear();
        }
        trackToPilesMap = new HashMap<TrackAdapter, List<Pileup>>();

        for (TrackAdapter t : trackToCanvasMap.keySet()) {
            try {
                //List<Integer> snps =
                long startPosition = NavigationUtils.getCurrentRange().getFrom();
                List<Pileup> piles = makePileupsFromSAMRecords(t.getName(), t.getDataInRange(), sequence, startPosition);
                this.trackToPilesMap.put(t, piles);
                //drawPiles(piles, trackToCanvasMap.get(t));

                //addMessag(snps.)
            } catch (IOException ex) {
                addMessage("Error: " + ex.getMessage());
                break;
            }
            //addMessag(snps.)
        }
    }

    /**
     * Make pileups for SAM records.
     */
    private List<Pileup> makePileupsFromSAMRecords(String trackName, List<Record> samRecords, byte[] sequence, long startPosition) throws IOException {

        //addMessage("Examining each position");

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        int length = sequence.length;
        for (int i = 0; i < length; i++) {
            pileups.add(new Pileup(trackName, startPosition + i, Pileup.getNucleotide(sequence[i])));
        }

        //System.out.println("Pileup start: " + startPosition);

        // go through the samrecords and edit the pileups
        for (Record r : samRecords) {
            SAMRecord sr = ((BAMIntervalRecord)r).getSamRecord();
            updatePileupsFromSAMRecord(pileups, GenomeUtils.getGenome(), sr, startPosition);
        }

        //addMessage("Done examining each position");

        return pileups;
    }

    /* UPDATE PILEUP INFORMATION FROM SAM RECORD */
    private void updatePileupsFromSAMRecord(List<Pileup> pileups, GenomeAdapter genome, SAMRecord samRecord, long startPosition) throws IOException {

        // the start and end of the alignment
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        // the read sequence
        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0; // true iff read sequence is set

        // return if no bases (can't be used for SNP calling)
        if (!sequenceSaved) {
            return;
        }

        // the reference sequence
        //byte[] refSeq = genome.getSequence(new Range(alignmentStart, alignmentEnd)).getBytes();
        byte[] refSeq = genome.getSequence(NavigationUtils.getCurrentReferenceName(), NavigationUtils.createRange(alignmentStart, alignmentEnd));

        // get the cigar object for this alignment
        Cigar cigar = samRecord.getCigar();

        // set cursors for the reference and read
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        //System.out.println("Alignment start: " + alignmentStart);

        int pileupcursor = (int) (alignmentStart - startPosition);

        // cigar variables
        CigarOperator operator;
        int operatorLength;

        // consider each cigar element
        for (CigarElement cigarElement : cigar.getCigarElements()) {

            operatorLength = cigarElement.getLength();
            operator = cigarElement.getOperator();

            // delete
            if (operator == CigarOperator.D) {
                // [ DRAW ]
            } // insert
            else if (operator == CigarOperator.I) {
                // [ DRAW ]
            } // match **or mismatch**
            else if (operator == CigarOperator.M) {

                // some SAM files do not contain the read bases
                if (sequenceSaved) {
                    // determine if there's a mismatch
                    for (int i = 0; i < operatorLength; i++) {
                        int refIndex = sequenceCursor - alignmentStart + i;
                        int readIndex = readCursor - alignmentStart + i;

                        byte[] readBase = new byte[1];
                        readBase[0] = readBases[readIndex];

                        Nucleotide readN = Pileup.getNucleotide(readBase[0]);

                        int j = i + (int) (alignmentStart - startPosition);
                        //for (int j = pileupcursor; j < operatorLength; j++) {
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN);
//                            /System.out.println("(P) " + readN + "\t@\t" + p.getPosition());
                        }
                        //}
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


    private void addSNPBookmarks() {

    }

    /**
     * Call SNPs for all tracks.
     */
    private void callSNPs() {

        if (this.trackToSNPsMap != null) { this.trackToSNPsMap.clear(); }
        this.trackToSNPsMap = new HashMap<TrackAdapter, List<Pileup>>();

        for (TrackAdapter t : trackToCanvasMap.keySet()) {
            List<Pileup> piles = trackToPilesMap.get(t);
            List<Pileup> snps = callSNPsFromPileups(piles, sequence);
            this.trackToSNPsMap.put(t, snps);
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

            double confidence = p.getSNPNucleotideConfidence()*100;

            /*
            System.out.println("Position: " + p.getPosition());
            System.out.println("\tAverage coverage: " + p.getCoverageProportion(n));
            System.out.println("\tAverage quality: " + p.getAverageQuality(n));
            System.out.println("\tConfidence: " + confidence);
            System.out.println("\tSensitivity: " + sensitivity);
             */

            if (confidence > 100-this.sensitivity) {
                //System.out.println("== Adding " + p.getPosition() + " as SNP");
                snps.add(p);
            }
        }

        addMessage(snps.size() + " SNPs found");

        //addMessage("Done calling SNPs");

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
     * Draw piles on panels for all Tracks.
     */
    private void drawPiles() {

        //lastTrackToCanvasMapDrawn = trackToCanvasMap;

        //System.out.println("Drawing annotations");

        for (TrackAdapter t : trackToSNPsMap.keySet()) {
            List<Pileup> pile = trackToSNPsMap.get(t);
            drawPiles(pile, trackToCanvasMap.get(t));
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

            BookmarkUtils.addBookmark(BookmarkUtils.createBookmark(NavigationUtils.getCurrentReferenceName(), NavigationUtils.createRange(snp.getPosition(), snp.getPosition()),
                    snp.getSNPNucleotide() + "/" + snp.getReferenceNucleotide()
                    + " SNP "
                    + (int) snp.getCoverage(snp.getSNPNucleotide()) + "/" + (int) snp.getCoverage(snp.getReferenceNucleotide())
                    + " = " + shortenPercentage(snp.getSNPNucleotideConfidence())
                    + " in " + t.getName()));
            snpsFound.get(t).add(snp);
        }
    }

    private String shortenPercentage(double p) {
        String s = ((int) Math.round(p*100)) + "";
        return s + "%";
    }
}
