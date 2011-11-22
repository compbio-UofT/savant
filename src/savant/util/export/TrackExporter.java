/*
 *    Copyright 2011 University of Toronto
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

package savant.util.export;

import java.io.File;
import java.io.IOException;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.controller.LocationController;
import savant.util.Controller;
import savant.util.DownloadEvent;
import savant.util.Range;


/**
 * Base class for track exporters which are used to convert a Savant track into a local
 * file suitable for use as an input to command-line tools like GATK.
 * 
 * @author tarkvara
 */
public abstract class TrackExporter extends Controller<DownloadEvent> {
    protected final TrackAdapter track;

    protected TrackExporter(TrackAdapter t) {
        track = t;
    }

    /**
     * Export the given range of the track.
     *
     * @param ref the chromosome containing the range be exported
     * @param r the range to be exported (or null to export the entire chromosome)
     * @param destFile the local file to be created
     */
    public abstract void exportRange(String ref, RangeAdapter r, File destFile) throws IOException;

    /**
     * Get a new TrackExporter appropriate for the given track.
     * @param t track to be exported (currently only fasta and bam are supported)
     * @return an appropriate exporter, or null if one not found
     */
    public static TrackExporter getExporter(TrackAdapter t) {
        switch (t.getDataFormat()) {
            case SEQUENCE_FASTA:
                return new FastaExporter(t);
            case INTERVAL_BAM:
                return new BamExporter(t);
            default:
                return null;
        }
    }
}
