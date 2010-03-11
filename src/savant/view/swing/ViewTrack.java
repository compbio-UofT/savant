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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.TrackController;
import savant.controller.ViewTrackController;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.model.data.RecordTrack;
import savant.model.data.interval.BEDIntervalTrack;
import savant.util.IOUtils;
import savant.model.Genome;
import savant.model.Resolution;
import savant.model.FileFormat;
import savant.model.data.Track;
import savant.model.data.continuous.GenericContinuousTrack;
import savant.model.data.interval.BAMIntervalTrack;
import savant.model.data.interval.GenericIntervalTrack;
import savant.model.data.point.GenericPointTrack;
import savant.model.view.ColorScheme;
import savant.model.view.Mode;
import savant.util.DataUtils;
import savant.util.Range;
import savant.view.swing.continuous.ContinuousViewTrack;
import savant.view.swing.interval.BAMCoverageViewTrack;
import savant.view.swing.interval.BAMViewTrack;
import savant.view.swing.interval.BEDViewTrack;
import savant.view.swing.interval.IntervalViewTrack;
import savant.view.swing.point.PointViewTrack;
import savant.view.swing.sequence.SequenceViewTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
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

    /**
     * Create one or more tracks based on the given file name.
     *
     * @param trackFilename
     * @return List of ViewTrack which can be added to a Fram
     */
    public static List<ViewTrack> create(String trackFilename) {

        System.out.println("Opening track " + trackFilename);

        List<ViewTrack> results = new ArrayList<ViewTrack>();

        // determine default track name from filename
        int lastSlashIndex = trackFilename.lastIndexOf(System.getProperty("file.separator"));
        String name = trackFilename.substring(lastSlashIndex+1, trackFilename.length());

        FileType fileType = DataUtils.getTrackDataTypeFromPath(trackFilename);

        ViewTrack viewTrack = null;
        Track dataTrack = null;

        // BAM
        if (fileType == FileType.INTERVAL_BAM) {

            // infer index file name from track filename
            dataTrack = new BAMIntervalTrack(new File(trackFilename), new File(trackFilename + ".bai"));
            viewTrack = new BAMViewTrack(name, (BAMIntervalTrack)dataTrack);
            results.add(viewTrack);

            // FIXME: this is a hack; should be done with formatting (when that is fixed)?
            /*BAMToCoverage bamConverter = new BAMToCoverage(trackFilename, trackFilename + ".bai",
                    MiscUtils.getTemporaryDirectory() + name + ".cov.savant",
                    null,
                    RangeController.getInstance().getMaxRange().getTo());
            bamConverter.format();
            */
            try {
                dataTrack = new GenericContinuousTrack(trackFilename + ".cov");
                viewTrack = new BAMCoverageViewTrack(name + " coverage" , (GenericContinuousTrack)dataTrack);
                results.add(viewTrack);
            } catch (IOException e) {
                log.error("Could not create coverage track", e);
            }

            // proprietary
        } else {

            try {

                // read file header
                RandomAccessFile trkFile = IOUtils.openFile(trackFilename, false);
                FileTypeHeader fth = DataUtils.readFileTypeHeader(trkFile);
                trkFile.close();

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
                    case INTERVAL_BED:
                        dataTrack = new BEDIntervalTrack(trackFilename);
                        viewTrack = new BEDViewTrack(name, (BEDIntervalTrack)dataTrack);
                        break;
                    default:
                        throw new Exception("Unrecognized file format");
                }
                if (viewTrack != null) results.add(viewTrack);
            } catch (Exception e) {
                Savant.promptUserToFormatFile(trackFilename);
            }
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

        ViewTrackController tc = ViewTrackController.getInstance();
        tc.addTrack(this);

        if (track != null) {
            TrackController.getInstance().addTrack(track);
        }
    }

    public FileFormat getDataType() {
        return this.dataType;
    }

    public ColorScheme getColorScheme() {
        return this.colorScheme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getDataInRange() {
        return this.dataInRange;
    }

    /*
    public DrawingInstructions getDrawingInstructions() {
        return this.DRAWING_INSTRUCTIONS;
    }
     */

    public Mode getDrawMode() {
        return this.drawMode;
    }

    public List<Mode> getDrawModes() {
        return this.drawModes;
    }

    public void setDataType(FileFormat kind) {
        this.dataType = kind;
    }

    public void setColorScheme(ColorScheme cs) {
        this.colorScheme = cs;
    }

    /*
    public void setDrawingInstructions(DrawingInstructions di) {
        this.DRAWING_INSTRUCTIONS = di;
    }
     */

    public void setDrawMode(Mode mode) {
        this.drawMode = mode;
    }

    public void setDrawMode(String modename) {
        for (Mode m : drawModes) {
            if (m.getName().equals(modename)) {
                setDrawMode(m);
                break;
            }
        }
    }

    public void setDrawMode(Object o) {
        setDrawMode(o.toString());
    }

    public void setDrawModes(List<Mode> modes) {
        this.drawModes = modes;
    }

    public RecordTrack getTrack() {
        return this.track;
    }
    
    // TODO: don't just throw generic Exception
    public abstract void prepareForRendering(Range range) throws Exception;

    // TODO: don't just throw generic Exception
    public List<Object> retrieveAndSaveData(Range range) throws Exception {
        Resolution resolution = getResolution(range);
        this.dataInRange = retrieveData(range, resolution);
        return this.dataInRange;
    }
    
    // TODO: don't just throw generic Exception
    public abstract List<Object> retrieveData(Range range, Resolution resolution) throws Exception;

    public void addTrackRenderer(TrackRenderer renderer) { this.trackRenderers.add(renderer); }
    public List<TrackRenderer> getTrackRenderers() { return this.trackRenderers; }

    public abstract Resolution getResolution(Range range);

    private void addDrawMode(Mode mode) {
        drawModes.add(mode);
    }

    public Mode getDefaultDrawMode() {
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }
   
}
