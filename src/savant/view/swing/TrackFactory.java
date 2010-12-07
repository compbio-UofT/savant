/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.api.util.DialogUtils;
import savant.data.sources.DataSource;
import savant.data.sources.file.BAMFileDataSource;
import savant.data.sources.file.BEDFileDataSource;
import savant.data.sources.file.FASTAFileDataSource;
import savant.data.sources.file.GenericContinuousFileDataSource;
import savant.data.sources.file.GenericIntervalFileDataSource;
import savant.data.sources.file.GenericPointFileDataSource;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedFileTypeException;
import savant.file.SavantUnsupportedVersionException;
import savant.format.SavantFileFormatterUtils;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.view.swing.continuous.ContinuousViewTrack;
import savant.view.swing.interval.BAMCoverageViewTrack;
import savant.view.swing.interval.BAMViewTrack;
import savant.view.swing.interval.BEDViewTrack;
import savant.view.swing.interval.IntervalViewTrack;
import savant.view.swing.point.PointViewTrack;
import savant.view.swing.sequence.SequenceViewTrack;

/**
 *
 * @author mfiume
 */
public class TrackFactory {

    private static final Log LOG = LogFactory.getLog(ViewTrack.class);

    public static ViewTrack createTrack(DataSource ds) throws SavantTrackCreationCancelledException {

        if (ds == null) { return null; }

        switch (ds.getDataFormat()) {
            case SEQUENCE_FASTA:
                return new SequenceViewTrack(ds);
            case INTERVAL_BED:
                return new BEDViewTrack(ds);
            case POINT_GENERIC:
                return new PointViewTrack(ds);
            case CONTINUOUS_GENERIC:
                return new ContinuousViewTrack(ds);
            case INTERVAL_BAM:
                return new BAMViewTrack(ds);
            case INTERVAL_GENERIC:
                return new IntervalViewTrack(ds);
            default:
                return null;
        }
    }

    /**
     * Create one or more tracks from the given file name.
     *
     * @param trackURI
     * @return List of ViewTrack which can be added to a Frame
     * @throws IOException
     */
    public static List<ViewTrack> createTrack(URI trackURI) throws SavantTrackCreationCancelledException {

        List<ViewTrack> results = new ArrayList<ViewTrack>();

        // determine default track name from filename
        String trackPath = trackURI.getPath();
        int lastSlashIndex = trackPath.lastIndexOf(System.getProperty("file.separator"));
        String name = trackPath.substring(lastSlashIndex + 1, trackPath.length());

        FileType fileType = SavantFileFormatterUtils.guessFileTypeFromPath(trackPath);

        ViewTrack viewTrack = null;
        DataSource ds = null;

        /**
         * TODO: Creating a list of view tracks, one of which may have a null datasource,
         * is very kludgy and causes many headaches downstream. It is very high priority
         * to do this more elegantly.
         */

        // BAM
        if (fileType == FileType.INTERVAL_BAM) {

            LOG.info("Opening BAM file " + trackURI);

            try {
                ds = BAMFileDataSource.fromURI(trackURI);
                if (ds != null) {
                    viewTrack = createTrack((BAMFileDataSource) ds);
                    results.add(viewTrack);
                } else {
                    DialogUtils.displayError("Error loading track", String.format("Could not create BAM track; check that index file exists and is named \"%1$s.bai\".", name));
                    return null;
                }

                // TODO: Only resolves coverage files for local data.  Should also work for network URIs.
                viewTrack = null;
                URI coverageURI = new URI(trackURI.toString() + ".cov.savant");
                if (NetworkUtils.exists(coverageURI)) {
                    ds = new GenericContinuousFileDataSource(coverageURI);
                    viewTrack = new BAMCoverageViewTrack((GenericContinuousFileDataSource) ds);
                } else {
                    // Coverage file is missing.  Always the case for remote tracks.
                    viewTrack = new BAMCoverageViewTrack(MiscUtils.getNeatPathFromURI(trackURI) + " (coverage)", null);
                }
            } catch (IOException e) {
                LOG.warn("Could not load coverage track", e);

                //FIXME: this should not happen! plugins expect tracks to contain data, and not be vacuous
                // create an empty ViewTrack that just displays an error message << see the above FIXME
                viewTrack = new BAMCoverageViewTrack(MiscUtils.getNeatPathFromURI(trackURI) + " (coverage)", null);
            } catch (SavantFileNotFormattedException e) {
                LOG.warn("Coverage track appears to be unformatted", e);
                viewTrack = new BAMCoverageViewTrack(MiscUtils.getNeatPathFromURI(trackURI) + " (coverage)", null);
            } catch (SavantUnsupportedVersionException e) {
                DialogUtils.displayMessage("Sorry", "This file was created using an older version of Savant. Please re-format the source.");
            } catch (URISyntaxException e) {
                DialogUtils.displayError("Savant Error", "Syntax error on URI; file URI is not valid");
            }

            results.add(viewTrack);

        } else {

            try {

                ds = TrackFactory.createDataSource(trackURI);
                if (ds != null && (viewTrack = createTrack(ds)) != null) {
                    results.add(viewTrack);
                }

            } catch (SavantUnsupportedFileTypeException ex) {
                DialogUtils.displayMessage("Sorry", "Files of this type are not supported.");
            } catch (SavantFileNotFormattedException e) {
                Savant s = Savant.getInstance();
                s.promptUserToFormatFile(trackURI);
            } catch (SavantUnsupportedVersionException e) {
                DialogUtils.displayMessage("Sorry", "This file was created using an older version of Savant. Please re-format the source.");
            } catch (IOException e) {
                DialogUtils.displayException("Error opening track", "There was a problem opening this file.", e);
            }
        }

        return results;
    }

    public static DataSource createDataSource(URI trackURI) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException, SavantUnsupportedFileTypeException {

        DataSource dataTrack = null;

        // read file header
        SavantROFile trkFile = new SavantROFile(trackURI);

        trkFile.close();

        if (trkFile.getFileType() == null) {
            throw new SavantFileNotFormattedException();
        }

        switch (trkFile.getFileType()) {
            case SEQUENCE_FASTA:
                dataTrack = new FASTAFileDataSource(trackURI);
                break;
            case POINT_GENERIC:
                dataTrack = new GenericPointFileDataSource(trackURI);
                break;
            case CONTINUOUS_GENERIC:
                dataTrack = new GenericContinuousFileDataSource(trackURI);
                break;
            case INTERVAL_GENERIC:
                dataTrack = new GenericIntervalFileDataSource(trackURI);
                break;
            case INTERVAL_GFF:
                dataTrack = new GenericIntervalFileDataSource(trackURI);
                break;
            case INTERVAL_BED:
                dataTrack = new BEDFileDataSource(trackURI);
                break;
            default:
                throw new SavantUnsupportedFileTypeException("This version of Savant does not support file type " + trkFile.getFileType());
        }
        return dataTrack;
    }

}
