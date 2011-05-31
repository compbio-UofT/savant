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

package savant.view.swing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.sf.samtools.util.RuntimeIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.PluginController;
import savant.data.event.TrackCreationEvent;
import savant.data.event.TrackCreationListener;
import savant.data.sources.BigWigDataSource;
import savant.data.sources.DataSource;
import savant.data.sources.TabixDataSource;
import savant.data.sources.TDFDataSource;
import savant.data.sources.file.BAMFileDataSource;
import savant.data.sources.file.BEDFileDataSource;
import savant.data.sources.file.FASTAFileDataSource;
import savant.data.sources.file.GenericContinuousFileDataSource;
import savant.data.sources.file.GenericIntervalFileDataSource;
import savant.data.sources.file.GenericPointFileDataSource;
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
import savant.view.swing.continuous.ContinuousTrack;
import savant.view.swing.interval.BAMCoverageTrack;
import savant.view.swing.interval.BAMTrack;
import savant.view.swing.interval.RichIntervalTrack;
import savant.view.swing.interval.IntervalTrack;
import savant.view.swing.point.PointTrack;
import savant.view.swing.sequence.SequenceTrack;

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
    public static Track createTrack(DataSource ds) throws SavantTrackCreationCancelledException {
        if (ds == null) {
            throw new IllegalArgumentException("DataSource cannot be null.");
        }

        switch (ds.getDataFormat()) {
            case SEQUENCE_FASTA:
                return new SequenceTrack(ds);
            case INTERVAL_BED:
                return new RichIntervalTrack(ds);
            case POINT_GENERIC:
                return new PointTrack(ds);
            case CONTINUOUS_GENERIC:
                return new ContinuousTrack(ds);
            case INTERVAL_BAM:
                return new BAMTrack(ds);
            case INTERVAL_GENERIC:
                return new IntervalTrack(ds);
            case TABIX:
                // Tabix data-sources can store either Bed or interval data.
                switch (((TabixDataSource)ds).getEffectiveDataFormat()) {
                    case INTERVAL_BED:
                        return new RichIntervalTrack(ds);
                    case INTERVAL_GENERIC:
                        return new IntervalTrack(ds);
                    default:
                        break;
                }
            default:
                throw new IllegalArgumentException(String.format("Unknown data format: %s.", ds.getDataFormat()));
        }
    }

    /**
     * Asynchronously create one or more tracks from the given file name.
     *
     * @param trackURI URI of the tracks being created
     * @param l the listener which is invoked when the track-creation thread completes
     */
    public static void createTrack(URI trackURI, TrackCreationListener l) {
        l.trackCreationStarted(new TrackCreationEvent());
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
     * Use this method if you want to create tracks the old synchronous way.  Our own
     * code should migrate to the asynchronous createTrack, but the external API's
     * createTrack is synchronous, and we should probably keep it that way.
     * 
     * @param trackURI
     * @return the newly-created tracks corresponding to this URI
     */
    public static Track[] createTrackSync(URI trackURI) throws Throwable {
        // Just run the track-creator in the current thread (presumably the AWT thread),
        // instead of spawning a new one.
        SyncTrackCreationListener listener = new SyncTrackCreationListener();
        TrackCreator t = new TrackCreator(trackURI, listener);
        t.run();
        if (listener.error != null) {
            throw listener.error;
        }
        return listener.result;
    }


    public static DataSource createDataSource(URI trackURI) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException, SavantUnsupportedFileTypeException {

        try {
            // Read file header to determine file type.
            SavantROFile trkFile = new SavantROFile(trackURI);
            trkFile.close();

            if (trkFile.getFileType() == null) {
                throw new SavantFileNotFormattedException();
            }

            switch (trkFile.getFileType()) {
                case SEQUENCE_FASTA:
                    return new FASTAFileDataSource(trackURI);
                case POINT_GENERIC:
                    return new GenericPointFileDataSource(trackURI);
                case CONTINUOUS_GENERIC:
                    return new GenericContinuousFileDataSource(trackURI);
                case INTERVAL_GENERIC:
                    return new GenericIntervalFileDataSource(trackURI);
                case INTERVAL_GFF:
                    return new GenericIntervalFileDataSource(trackURI);
                case INTERVAL_BED:
                    return new BEDFileDataSource(trackURI);
                default:
                    throw new SavantUnsupportedFileTypeException("This version of Savant does not support file type " + trkFile.getFileType());
            }
        } catch (UnknownSchemeException usx) {
            // Not one of our known URI schemes, so see if any of our plugins can handle it.
            for (SavantDataSourcePlugin p: PluginController.getInstance().getDataSourcePlugins()) {
                DataSource ds = p.getDataSource(trackURI);
                if (ds != null) {
                    return ds;
                }
            }
            throw usx;
        }
    }

    static class TrackCreator implements Runnable {
        private URI trackURI;
        
        /** For now, we only allow a single external listener for a given track-creator thread. */
        private TrackCreationListener listener;

        TrackCreator(URI trackURI, TrackCreationListener l) {
            this.trackURI = trackURI;
            listener = l;
        }

        @Override
        public void run() {
            // determine default track name from filename
            String uriString = trackURI.toString();
            int lastSlashIndex = uriString.lastIndexOf(System.getProperty("file.separator"));
            String name = uriString.substring(lastSlashIndex + 1);

            FileType fileType = SavantFileFormatterUtils.guessFileTypeFromPath(uriString);

            DataSource ds = null;

            try {
                // A switch statement might be nice here, except for the possibility that fileType == null.
                if (fileType == FileType.TABIX) {
                    LOG.info("Opening Tabix file " + trackURI);
                    ds = TabixDataSource.fromURI(trackURI);
                    LOG.info("Tabix datasource=" + ds);
                    if (ds != null) {
                        fireTrackCreationCompleted(new Track[] { createTrack(ds) }, "");
                        LOG.trace("Tabix track created.");
                    } else {
                        throw new FileNotFoundException(String.format("Could not create Tabix track; check that index file exists and is named \"%1$s.tbi\".", name));
                    }
                } else if (fileType == FileType.INTERVAL_BAM) {
                    LOG.info("Opening BAM file " + trackURI);

                    ds = BAMFileDataSource.fromURI(trackURI);
                    LOG.info("BAM datasource=" + ds);
                    if (ds != null) {
                        List<Track> tracks = new ArrayList<Track>(2);
                        tracks.add(createTrack(ds));
                        LOG.trace("BAM Track created.");
                        try {
                            // TODO: Only resolves coverage files for local data.  Should also work for network URIs.
                            URI coverageURI = new URI(trackURI.toString() + ".cov.tdf");
                            if (NetworkUtils.exists(coverageURI)) {
                                tracks.add(new BAMCoverageTrack(new TDFDataSource(coverageURI)));
                            } else {
                                coverageURI = new URI(trackURI.toString() + ".cov.savant");
                                if (NetworkUtils.exists(coverageURI)) {
                                    tracks.add(new BAMCoverageTrack(new GenericContinuousFileDataSource(coverageURI)));
                                }
                            }
                            fireTrackCreationCompleted(tracks.toArray(new Track[0]), "");
                        } catch (URISyntaxException ignored) {
                        }
                        LOG.info("Finished trying to load coverage file.");
                    } else {
                        throw new FileNotFoundException(String.format("Could not create BAM track; check that index file exists and is named \"%1$s.bai\".", name));
                    }
                } else {
                    if (fileType == FileType.CONTINUOUS_BIGWIG) {
                        LOG.info("Opening BigWig file " + trackURI);
                        ds = new BigWigDataSource(new File(trackURI));
                    } else if (fileType == FileType.CONTINUOUS_TDF) {
                        LOG.info("Opening TDF file " + trackURI);
                        ds = new TDFDataSource(trackURI);
                    } else {
                        ds = TrackFactory.createDataSource(trackURI);
                    }
                    fireTrackCreationCompleted(new Track[] { createTrack(ds) }, "");
                }
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
                    listener.trackCreationCompleted(new TrackCreationEvent(tracks, name));
                }
            });

            // This is a good opportunity to load our dictionary.
            for (Track t: tracks) {
                URI dictionaryURI = URI.create(t.getDataSource().getURI().toString() + ".dict");
                try {
                    if (NetworkUtils.exists(dictionaryURI)) {
                        t.loadDictionary(dictionaryURI);
                    }
                } catch (IOException x) {
                    LOG.error("Unable to load dictionary for " + dictionaryURI);
                }
            }
        }

        /**
         * Fires a track-creation error event.  It will be posted to the AWT event-queue
         * thread, so that UI code can function properly.
         */
        private void fireTrackCreationFailed(final Throwable x) {
            MiscUtils.invokeLaterIfNecessary(new Runnable() {
                @Override
                public void run() {
                    trackCreationFailed(x);
                    listener.trackCreationFailed(new TrackCreationEvent(x));
                }
            });
        }

        /**
         * Internally we handle most common error messages, so that the listener class
         * doesn't need to.  It might make more sense to move this application-specific
         * UI code out to an adapter class.
         *
         * @param x
         */
        public void trackCreationFailed(Throwable x) {
            if (x instanceof SavantUnsupportedFileTypeException) {
                DialogUtils.displayMessage("Sorry", "Files of this type are not supported.");
            } else if (x instanceof SavantFileNotFormattedException) {
                Savant s = Savant.getInstance();
                s.promptUserToFormatFile(trackURI);
            } else if (x instanceof SavantUnsupportedVersionException) {
                DialogUtils.displayMessage("Sorry", "This file was created using an older version of Savant. Please re-format the source.");
            } else if (x instanceof SavantTrackCreationCancelledException) {
                // Nothing to do.  The user already knows they cancelled, so no point in putting up a dialog.
            } else if (x instanceof FileNotFoundException) {
                DialogUtils.displayMessage("File not found", String.format("<html><i>%s</i></html> not found.", x.getMessage()));
            } else {
                DialogUtils.displayException("Error opening track", "There was a problem opening this file: " + x.getLocalizedMessage(), x);
            }
        }
    }

    /**
     * Listener used to pass results back to the sync version of createTrack.
     */
    static class SyncTrackCreationListener implements TrackCreationListener {
        Track[] result;
        Throwable error;

        @Override
        public void trackCreationStarted(TrackCreationEvent evt) {
        }

        @Override
        public void trackCreationCompleted(TrackCreationEvent evt) {
            result = evt.getTracks();
        }

        @Override
        public void trackCreationFailed(TrackCreationEvent evt) {
            error = evt.getError();
        }
    }
}
