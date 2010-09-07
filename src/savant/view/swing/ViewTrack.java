/*
 *    Copyright 2009-2010 University of Toronto
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

package savant.view.swing;

import java.awt.Color;

import savant.data.types.Genome;
import savant.file.SavantUnsupportedVersionException;
import savant.format.SavantFileFormatterUtils;
import savant.util.Resolution;
import savant.view.dialog.BAMParametersDialog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.TrackController;
import savant.controller.ViewTrackController;
import savant.file.FileType;
import savant.file.FileTypeHeader;
import savant.file.FileFormat;
import savant.model.data.RecordTrack;
import savant.model.data.Track;
import savant.model.data.continuous.GenericContinuousTrack;
import savant.model.data.interval.BAMIntervalTrack;
import savant.model.data.interval.BEDIntervalTrack;
import savant.model.data.interval.GenericIntervalTrack;
import savant.model.data.point.GenericPointTrack;
import savant.model.view.ColorScheme;
import savant.model.view.Mode;
import savant.util.RAFUtils;
import savant.util.Range;
import savant.view.swing.continuous.ContinuousViewTrack;
import savant.view.swing.interval.BAMCoverageViewTrack;
import savant.view.swing.interval.BAMViewTrack;
import savant.view.swing.interval.BEDViewTrack;
import savant.view.swing.interval.IntervalViewTrack;
import savant.view.swing.point.PointViewTrack;
import savant.view.swing.sequence.SequenceViewTrack;
import savant.view.swing.util.DialogUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Class to handle the preparation for rendering of a track. Handles colour schemes and
 * drawing instructions, getting and filtering of data, setting of vertical axis, etc. The
 * ranges associated with various resolutions are also handled here, and the drawing modes
 * are defined.
 *
 * @author mfiume
 */
public abstract class ViewTrack {

    private static Log log = LogFactory.getLog(ViewTrack.class);

    private String name;
    private ColorScheme colorScheme;
    private FileFormat dataType;
    private List<Object> dataInRange;
    private List<Mode> drawModes;
    private Mode drawMode;
    private List<TrackRenderer> trackRenderers;
    private RecordTrack track;
    private URI fileURI;

    private ColorSchemeDialog colorDialog = new ColorSchemeDialog();
    private IntervalDialog intervalDialog = new IntervalDialog();

    // FIXME:
    private Frame frame;

    private static BAMParametersDialog paramDialog = new BAMParametersDialog(Savant.getInstance(), true);

    // TODO: put all of this in a ViewTrackFactory class
    // TODO: inform the user when there is a problem
    /**
     * Create one or more tracks from the given file name.
     *
     * @param trackFilename
     * @return List of ViewTrack which can be added to a Fram
     * @throws IOException
     */
    public static List<ViewTrack> create(String trackFilename) throws IOException {

        System.out.println("Opening track " + trackFilename);

        List<ViewTrack> results = new ArrayList<ViewTrack>();

        // determine default track name from filename
        int lastSlashIndex = trackFilename.lastIndexOf(System.getProperty("file.separator"));
        String name = trackFilename.substring(lastSlashIndex+1, trackFilename.length());

        FileType fileType = SavantFileFormatterUtils.getTrackDataTypeFromPath(trackFilename);

        ViewTrack viewTrack = null;
        Track dataTrack = null;

        // BAM
        if (fileType == FileType.INTERVAL_BAM) {

            dataTrack = BAMIntervalTrack.fromfileNameOrURL(trackFilename);
            if (dataTrack != null) {
                viewTrack = new BAMViewTrack(name, (BAMIntervalTrack)dataTrack);
                if(viewTrack != null) viewTrack.setURI(trackFilename);
                results.add(viewTrack);
            }
            else {
                String e = "Could not create BAM track; check that index file exists and is named  filename.bam.bai or filename.bai";
                JOptionPane.showConfirmDialog(Savant.getInstance(), e, "Error loading track", JOptionPane.DEFAULT_OPTION);
                return null;
            }

            // TODO: test BAM to coverage (with v2 formatting) then re-enable adding coverage file
            /* RE-ENABLE STARTING */
            try {
                String coverageFileName = trackFilename + ".cov.savant";

                if ((new File(coverageFileName)).exists()) {
                    dataTrack = new GenericContinuousTrack(coverageFileName);
                    viewTrack = new BAMCoverageViewTrack(name + " (coverage)" , (GenericContinuousTrack)dataTrack);
                }
                else {
                    log.info("No coverage track available");
                    viewTrack = new BAMCoverageViewTrack(name + " (coverage)" , null);
                }
            } catch (IOException e) {
                log.warn("Could not load coverage track", e);
                // create an empty ViewTrack that just displays an error message
                viewTrack = new BAMCoverageViewTrack(name + " (coverage)"  , null);
            } catch (SavantUnsupportedVersionException e) {
                DialogUtils.displayMessage("This file was created using an older version of Savant. Please re-format the source.");
            }

              //RE-ENABLE ENDING HERE
            if(viewTrack != null) viewTrack.setURI( trackFilename + ".cov.savant");
            results.add(viewTrack);

        } else {

            try {

                System.out.println("Opening Savant file");

                // read file header
                RandomAccessFile trkFile = RAFUtils.openFile(trackFilename, false);

                //System.out.println("Reading file type header");
                FileTypeHeader fth = RAFUtils.readFileTypeHeader(trkFile);
                //System.out.println("File type: " + fth.fileType);

                trkFile.close();

                if(fth.fileType == null){
                    Savant s = Savant.getInstance();
                    s.promptUserToFormatFile(trackFilename, "This file does not appear to be formatted. Format now?");
                    return results;
                }

                switch(fth.fileType) {
                    case SEQUENCE_FASTA:
                        Genome g = new Genome(trackFilename);
                        viewTrack = new SequenceViewTrack(name, g);
                        break;
                    case POINT_GENERIC:
                        dataTrack = new GenericPointTrack(trackFilename);
                        viewTrack = new PointViewTrack(name, (GenericPointTrack)dataTrack);
                        break;
                    case CONTINUOUS_GENERIC:
                        dataTrack = new GenericContinuousTrack(trackFilename);
                        viewTrack = new ContinuousViewTrack(name, (GenericContinuousTrack)dataTrack);
                        break;
                    case INTERVAL_GENERIC:
                        dataTrack = new GenericIntervalTrack(trackFilename);
                        viewTrack = new IntervalViewTrack(name, (GenericIntervalTrack)dataTrack);
                        break;
                    case INTERVAL_GFF:
                        dataTrack = new GenericIntervalTrack(trackFilename);
                        viewTrack = new IntervalViewTrack(name, (GenericIntervalTrack)dataTrack);
                        break;
                    case INTERVAL_BED:
                        dataTrack = new BEDIntervalTrack(trackFilename);
                        viewTrack = new BEDViewTrack(name, (BEDIntervalTrack)dataTrack);
                        break;
                    default:
                        Savant s = Savant.getInstance();
                        s.promptUserToFormatFile(trackFilename, "This file does not appear to be formatted. Format now?");

                }
                if (viewTrack != null) results.add(viewTrack);
            } catch (IOException e) {
                Savant s = Savant.getInstance();
                s.promptUserToFormatFile(trackFilename, e.getMessage());
                e.printStackTrace();
            } catch (SavantUnsupportedVersionException e) {
                DialogUtils.displayMessage("This file was created using an older version of Savant. Please re-format the source.");
            }
            if(viewTrack != null) viewTrack.setURI(trackFilename);
        }

        return results;
    }


    /**
     * Constructor
     * @param name track name (typically, the file name)
     * @param dataType FileFormat representing file type, e.g. INTERVAL_BED, CONTINUOUS_GENERIC
     */
    public ViewTrack(String name, FileFormat dataType, RecordTrack track) {
        setName(name);
        setDataType(dataType);
        drawModes = new ArrayList<Mode>();
        trackRenderers = new ArrayList<TrackRenderer>();
        this.track = track;     
    }

    public void notifyViewTrackControllerOfCreation() {
        ViewTrackController tc = ViewTrackController.getInstance();
        tc.addTrack(this);

        if (track != null) {
            TrackController.getInstance().addTrack(track);
        }
    }

    //public void setFilename(String fn) {
    //    this.filename = fn;
    //}

    public String getPath() {
        if (this.getTrack() == null) { return null; }
        return this.getTrack().getPath();
    }

    /**
     * Get the type of file this view track represents
     *
     * @return  FileFormat
     */
    public FileFormat getDataType() {
        return this.dataType;
    }

    /**
     * Get the current colour scheme.
     *
     * @return ColorScheme
     */
    public ColorScheme getColorScheme() {
        return this.colorScheme;
    }

    /**
     * Set individual colour.
     *
     * @param name color name
     * @param color new color
     */
    public void setColor(String name, Color color) {
        this.colorScheme.addColorSetting(name, color);
    }

    /**
     * Get the name of this track. Usually constructed from the file name.
     *
     * @return track name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the track name.
     *
     * @param name new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the data currently being displayed (or ready to be displayed)
     *
     * @return List of data objects
     */
    public List<Object> getDataInRange() {
        return this.dataInRange;
    }

    /*
    public DrawingInstructions getDrawingInstructions() {
        return this.DRAWING_INSTRUCTIONS;
    }
     */

    /**
     * Get current draw mode
     *
     * @return draw mode as Mode
     */
    public Mode getDrawMode() {
        return this.drawMode;
    }

    /**
     * Get all valid draw modes for this track.
     *
     * @return List of draw Modes
     */
    public List<Mode> getDrawModes() {
        return this.drawModes;
    }

    /**
     * Set data type.
     *
     * @param kind
     */
    public void setDataType(FileFormat kind) {
        this.dataType = kind;
    }

    /**
     * Set colour scheme.
     *
     * @param cs new colour scheme
     */
    public void setColorScheme(ColorScheme cs) {
        this.colorScheme = cs;
    }

    /**
     * Reset colour scheme.
     *
     */
    public abstract void resetColorScheme();

    /*
    public void setDrawingInstructions(DrawingInstructions di) {
        this.DRAWING_INSTRUCTIONS = di;
    }
     */

    /**
     * Set the current draw mode.
     *
     * @param mode
     */
    public void setDrawMode(Mode mode) {
        this.drawMode = mode;
    }

    /**
     * Set the current draw mode by its name
     *
     * @param modename
     */
    public void setDrawMode(String modename) {
        for (Mode m : drawModes) {
            if (m.getName().equals(modename)) {
                setDrawMode(m);
                break;
            }
        }
    }

//    public void setDrawMode(Object o) {
//        setDrawMode(o.toString());
//    }

    /**
     * Set the list of valid draw modes
     *
     * @param modes
     */
    public void setDrawModes(List<Mode> modes) {
        this.drawModes = modes;
    }

    /**
     * Get the record (data) track associated with this view track (if any.)
     *
     * @return Record Track or null (in the case of a genome.)
     */
    public RecordTrack getTrack() {
        return this.track;
    }

    // FIXME:
    public Frame getFrame() {
        return frame;
    }

    // FIXME:
    public void setFrame(Frame frame) {
        this.frame = frame;
        colorDialog.setFrame(frame);
        intervalDialog.setFrame(frame);
    }

    /**
     * Prepare this view track to render the given range.
     *
     * @param range
     * @throws Exception
     */
    public abstract void prepareForRendering(String reference, Range range) throws Throwable;


    /**
     * Retrieve data from the underlying data track at the current resolution and save it.
     *
     * @param range The range within which to retrieve objects
     * @return a list of data objects in the given range
     * @throws Exception
     */
    public List<Object> retrieveAndSaveData(String reference, Range range) throws Throwable {
        Resolution resolution = getResolution(range);

        /*
        // Get current time
        long start = System.currentTimeMillis();
         */

        this.dataInRange = retrieveData(reference, range, resolution);

        /*
        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        System.out.println("\tData retreival for " + this.getName() + " took " + elapsedTimeSec + " seconds");
         */

        return this.dataInRange;
    }

    /**
     * Store null to dataInRange.
     *
     * @throws Exception
     */
    public void saveNullData() throws Throwable {
        this.dataInRange = null;
    }

    /**
     * Retrive data from the underlying data track.
     *
     * @param range The range within which to retrieve objects
     * @param resolution The resolution at which to get data
     * @return a List of data objects from the given range and resolution
     * @throws Exception
     */
    public abstract List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Throwable;

    /**
     * Add a renderer to this view track
     *
     * @param renderer
     */
    public void addTrackRenderer(TrackRenderer renderer) { this.trackRenderers.add(renderer); }

    /**
     * Get all renderers attached to this view track
     *
     * @return
     */
    public List<TrackRenderer> getTrackRenderers() { return this.trackRenderers; }

    /**
     * Get the resoultion associated with the given range
     *
     * @param range
     * @return resolution appropriate to the range
     */
    public abstract Resolution getResolution(Range range);

    /**
     * Get the default draw mode.
     *
     * @return  the default draw mode
     */
    public Mode getDefaultDrawMode() {
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static void captureBAMDisplayParameters(BAMViewTrack viewTrack) {
        paramDialog.setVisible(true);
        if (paramDialog.isAccepted()) {
            viewTrack.setArcSizeVisibilityThreshold(paramDialog.getArcLengthThreshold());
            viewTrack.setDiscordantMin(paramDialog.getDiscordantMin());
            viewTrack.setDiscordantMax(paramDialog.getDiscordantMax());
        }

    }

    public void captureColorParameters() {
        colorDialog.update(this);
        colorDialog.setVisible(true);
    }

    public void captureIntervalParameters(){
        intervalDialog.update(this);
        intervalDialog.setVisible(true);
    }

    public void setURI(String name){

        //TODO: are there other cases where this will fail? Maybe URI isnt best option?
        name = name.replace("\\", "/");
        name = name.replace(" ", "_");
        try {
            this.fileURI = new URI(name);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ViewTrack.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public URI getURI(){
        return this.fileURI;
    }
}
