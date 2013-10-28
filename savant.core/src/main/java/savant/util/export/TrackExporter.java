/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.util.export;

import java.io.File;
import java.io.IOException;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.util.TrackUtils;
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

    /** Bases exported so far (for calculating progress). */
    protected long basesSoFar;

    /** Total length to be exported (for calculating progress). */
    protected long totalBases;

    /** Destination file for output. */
    protected File destFile;

    protected TrackExporter(TrackAdapter t, File f) {
        track = t;
        destFile = f;
    }

    /**
     * Export the given range of the track.
     *
     * @param ref the chromosome containing the range be exported (or null to export the whole genome).
     * @param r the range to be exported (or null to export the entire chromosome)
     */
    public void export(String ref, RangeAdapter r) throws IOException, InterruptedException {
        LocationController lc = LocationController.getInstance();
        try {
            if (ref == null) {
                for (String ref2: lc.getReferenceNames()) {
                    totalBases += lc.getReferenceLength(ref2);
                }
                for (String ref2: lc.getReferenceNames()) {
                    exportRange(ref2, new Range(1, lc.getReferenceLength(ref2)));
                }
            } else {
                totalBases = LocationController.getInstance().getReferenceLength(ref);
                if (r == null) {
                    r = new Range(1, (int)totalBases);
                }
                exportRange(ref, r);
            }
        } finally {
            close();
        }
        fireEvent(new DownloadEvent(destFile));
    }

    /**
     * Give derived classes a chance to clean up when they've finished processing.
     */
    abstract void close() throws IOException;

    /**
     * Export the specified reference (or subrange thereof) to the given output stream.
     * This may be invoked as part of a large export (i.e. whole genome).
     * 
     * @param ref the reference containing the range be exported (must be non-null)
     * @param r the range to be exported (must be non-null)
     * @throws IOException 
     */
    abstract void exportRange(String ref, RangeAdapter r) throws IOException, InterruptedException;

    /**
     * Get a new TrackExporter appropriate for the given track.
     * @param trackURI URI of the track to be exported (currently only fasta and bam are supported)
     * @return an appropriate exporter, or null if one not found
     */
    public static TrackExporter getExporter(String trackURI, File f) throws IOException {
        TrackAdapter t = TrackUtils.getTrack(trackURI);
        switch (t.getDataFormat()) {
            case SEQUENCE:
                return new FastaExporter(t, f);
            case ALIGNMENT:
                return new BAMExporter(t, f);
            default:
                return null;
        }
    }
}
