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

package savant.view.tracks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.util.RuntimeIOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.util.DialogUtils;
import savant.controller.DataSourcePluginController;
import savant.data.sources.*;
import savant.exception.SavantTrackCreationCancelledException;
import savant.exception.UnknownSchemeException;
import savant.file.FileType;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantROFile;
import savant.file.SavantUnsupportedFileTypeException;
import savant.file.SavantUnsupportedVersionException;
import savant.format.SavantFileFormatterUtils;
import savant.plugin.SavantDataSourcePlugin;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;


/**
 * Factory class responsible for creating all flavours of Track objects.
 *
 * @author mfiume, tarkvara
 */
public class TrackFactory {

    private static final Log LOG = LogFactory.getLog(Track.class);

    /**
     * Create a track from an existing DataSource.  This method is synchronous, because
     * it's assumed that creating the DataSource is the time-consuming part of the process.
     *
     * @param ds the DataSource for the track
     * @return a freshly-constructed track.
     * @throws SavantTrackCreationCancelledException
     */
    public static Track createTrack(DataSourceAdapter ds) throws SavantTrackCreationCancelledException {
        if (ds == null) {
            throw new IllegalArgumentException("DataSource cannot be null.");
        }

        Track t;
        switch (ds.getDataFormat()) {
            case SEQUENCE:
                t = new SequenceTrack(ds);
                break;
            case RICH_INTERVAL:
                t = new RichIntervalTrack(ds);   // BED or Tabix
                break;
            case POINT:
                t = new PointTrack(ds);
                break;
            case CONTINUOUS:
                t = new ContinuousTrack(ds);     // Savant or TDF
                break;
            case ALIGNMENT:
                t = new BAMTrack(ds);
                break;
            case GENERIC_INTERVAL:
                t = new IntervalTrack(ds);
                break;
            case VARIANT:
                t = new VariantTrack(ds);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown data format: %s.", ds.getDataFormat()));
        }
        return t;
    }

    /**
     * Asynchronously create one or more tracks from the given file name.
     *
     * @param trackURI URI of the tracks being created
     * @param l the listener which is invoked when the track-creation thread completes
     */
    public static void createTrack(URI trackURI, TrackCreationListener l) {
        l.handleEvent(new TrackCreationEvent());
        Thread t = new Thread(new TrackCreator(trackURI, l), "TrackCreator");
        t.start();
        try {
            t.join(1000);
            // Join timed out, but we are still waiting for tracks to be created.
            LOG.debug("Join timed out, putting up progress-bar for track creation.");
        } catch (InterruptedException ix) {
            LOG.error("TrackCreator interrupted during join.", ix);
        }
    }

    /**
     * Create a DataSource, guessing the file-type from the file's extension.
     */
    public static DataSourceAdapter createDataSource(URI trackURI, TrackCreationListener l) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException, SavantUnsupportedFileTypeException {
        return createDataSource(trackURI, SavantFileFormatterUtils.guessFileTypeFromPath(trackURI.toString()), l);
    }

    /**
     * Create a DataSource for the given URI.
     */
    public static DataSourceAdapter createDataSource(URI trackURI, FileType fileType, TrackCreationListener l) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException, SavantUnsupportedFileTypeException {
        if (fileType == null) {
            try {
                // Read file header to determine file type.
                SavantROFile trkFile = new SavantROFile(trackURI);
                trkFile.close();

                if (trkFile.getFileType() == null) {
                    throw new SavantFileNotFormattedException();
                }

                switch (trkFile.getFileType()) {
                    case SEQUENCE_FASTA:
                        return new OldFastaDataSource(trackURI);
                    case POINT_GENERIC:
                        return new GenericPointDataSource(trackURI);
                    case CONTINUOUS_GENERIC:
                        return new GenericContinuousDataSource(trackURI);
                    case INTERVAL_GENERIC:
                        return new GenericIntervalDataSource(trackURI);
                    case INTERVAL_GFF:
                        return new GenericIntervalDataSource(trackURI);
                    case INTERVAL_BED:
                        return new BEDDataSource(trackURI);
                    default:
                        throw new SavantUnsupportedFileTypeException("This version of Savant does not support file type " + trkFile.getFileType());
                }
            } catch (UnknownSchemeException usx) {
                // Not one of our known URI schemes, so see if any of our plugins can handle it.
                for (SavantDataSourcePlugin p: DataSourcePluginController.getInstance().getPlugins()) {
                    DataSourceAdapter ds = p.getDataSource(trackURI);
                    if (ds != null) {
                        return ds;
                    }
                }
                throw usx;
            }
        } else {
            switch (fileType) {
                case SEQUENCE_FASTA:
                    LOG.info("Opening Fasta file " + trackURI);
                    return new FastaDataSource(trackURI, l);
                case TABIX:
                    LOG.info("Opening Tabix file " + trackURI);
                    return new TabixDataSource(trackURI);
                case INTERVAL_BAM:
                    LOG.info("Opening BAM file " + trackURI);
                    return new BAMDataSource(trackURI);
                case CONTINUOUS_BIGWIG:
                    LOG.info("Opening BigWig file " + trackURI);
                    return new BigWigDataSource(trackURI);
                case CONTINUOUS_TDF:
                    LOG.info("Opening TDF file " + trackURI);
                    return new TDFDataSource(trackURI);
                default:
                    // Some other format which we can't handle.
                    throw new SavantFileNotFormattedException();
            }
        }
    }

    static class TrackCreator implements Runnable {
        private URI trackURI;
        
        /**
         * We only allow a single external listener for a given track-creator thread.
         */
        private final TrackCreationListener listener;

        /**
         *
         * @param trackURI the URI for loading the base track (URIs of additional tracks like coverage will be derived)
         * @param l the single listener for TrackCreationEvents, generally a Frame
         */
        TrackCreator(URI trackURI, TrackCreationListener l) {
            this.trackURI = trackURI;
            listener = l;
        }

        @Override
        public void run() {
            // determine default track name from filename
            String uriString = trackURI.toString();

            FileType fileType = SavantFileFormatterUtils.guessFileTypeFromPath(uriString);

            try {
                DataSourceAdapter ds = createDataSource(trackURI, fileType, listener);

                List<Track> tracks = new ArrayList<Track>(2);
                tracks.add(createTrack(ds));
                if (fileType == FileType.INTERVAL_BAM) {
                    URI coverageURI = new URI(uriString + ".cov.tdf");
                    try {
                        if (NetworkUtils.exists(coverageURI)) {
                            ds = new TDFDataSource(coverageURI);
                            tracks.add(new BAMCoverageTrack(ds));
                        } else {
                            coverageURI = new URI(uriString + ".cov.savant");
                            if (NetworkUtils.exists(coverageURI)) {
                                ds = new GenericContinuousDataSource(coverageURI);
                                tracks.add(new BAMCoverageTrack(ds));
                            }
                        }
                    } catch (Exception x) {
                        LOG.error("Unable to load coverage track for " + uriString, x);
                    }
                }
                LOG.debug("Firing trackCreationCompleted for " + tracks.get(0));
                fireTrackCreationCompleted(tracks.toArray(new Track[0]), "");
            } catch (RuntimeIOException x) {
                // Special case: SamTools I/O Exception which contains the real exception nested within.
                LOG.error("Track creation failed.", x);
                fireTrackCreationFailed(x.getCause());
            } catch (SavantFileNotFormattedException x) {
                // Special case: we don't want to pollute error log with a stack trace for this non-error.
                fireTrackCreationFailed(x);
            } catch (SavantTrackCreationCancelledException x) {
                // Another special case: we don't want a stack trace when all the user did was click cancel.
                fireTrackCreationFailed(x);
            } catch (Exception x) {
                LOG.error("Track creation failed.", x);
                fireTrackCreationFailed(x);
            }
        }

        /**
         * Fires a track-creation successful completion event.  It will be posted to the
         * AWT event-queue thread, so that UI code can function properly.
         */
        private void fireTrackCreationCompleted(final Track[] tracks, final String name) {
            MiscUtils.invokeLaterIfNecessary(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.handleEvent(new TrackCreationEvent(tracks, name));
                    }
                }
            });

            // This is a good opportunity to load our dictionary.
            for (final Track t: tracks) {
                new Thread("Dictionary Loader-" + MiscUtils.getFilenameFromPath(t.getName())) {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                t.getDataSource().loadDictionary();
                            } catch (Exception x) {
                                LOG.error("Unable to load dictionary for " + t.getName(), x);
                            }
                        }
                    }
                }.start();
            }
        }

        /**
         * Fires a track-creation error event.  It will be posted to the AWT event-queue
         * thread, so that UI code can function properly.
         *
         * Internally we handle most common error messages, so that the listener class
         * doesn't need to.  It might make more sense to move this application-specific
         * UI code out to an adapter class.
         *
         * @param x
         */
        private void fireTrackCreationFailed(final Throwable x) {
            MiscUtils.invokeLaterIfNecessary(new Runnable() {
                @Override
                public void run() {
                    if (x instanceof SavantUnsupportedFileTypeException) {
                        DialogUtils.displayMessage("Sorry", "Files of this type are not supported.");
                    } else if (x instanceof SavantFileNotFormattedException) {
                        SavantFileFormatterUtils.promptUserToFormatFile(trackURI);
                    } else if (x instanceof SavantUnsupportedVersionException) {
                        DialogUtils.displayMessage("Sorry", "This file was created using an older version of Savant. Please re-format the source.");
                    } else if (x instanceof SavantTrackCreationCancelledException) {
                        // Nothing to do.  The user already knows they cancelled, so no point in putting up a dialog.
                    } else if (x instanceof FileNotFoundException) {
                        DialogUtils.displayMessage("File not found", String.format("<html>File <i>%s</i> not found.</html>", x.getMessage()));
                    } else {
                        DialogUtils.displayException("Error opening track", String.format("There was a problem opening this file: %s.", MiscUtils.getMessage(x)), x);
                    }
                    listener.handleEvent(new TrackCreationEvent(x));
                }
            });
        }
    }
    
    /**
     * Normally we would just use Listener&lt;TrackCreationEvent&gt;, but Java's lame-ass implementation
     * of generics gets in the way.
     */
    public interface TrackCreationListener {
        public void handleEvent(TrackCreationEvent evt);
    }
}
