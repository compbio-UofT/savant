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

package savant.data.sources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broad.igv.tdf.TDFDataset;
import org.broad.igv.tdf.TDFReader;
import org.broad.igv.tdf.TDFTile;
import org.broad.igv.track.WindowFunction;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.data.types.GenericContinuousRecord;
import savant.util.MiscUtils;
import savant.util.Range;

/**
 * Data source which uses Broad Institute's TDF format to display a continuous track.
 * Intended as a more efficient replacement for our existing GenericContinuousTrack.
 *
 * @author tarkvara
 */
public class TDFDataSource extends DataSource<GenericContinuousRecord> {

    private static final Log LOG = LogFactory.getLog(TDFDataSource.class);
    private static final double LOG2 = Math.log(2.0);
    private static final int NOTIONAL_SCREEN_WIDTH = 2000;

    private final TDFReader tdf;
    private final URI uri;
    private int maxZoom = -1;
    private String rawUnhomogenised;

    public TDFDataSource(URI uri) throws IOException {
        tdf = TDFReader.getReader(uri);
        this.uri = uri;
    }

    @Override
    public Set<String> getReferenceNames() {
        Set<String> result = new HashSet<String>();
        for (String key: tdf.getDatasetNames()) {
            int rawPos = key.indexOf("/raw");
            if (rawPos > 1) {
                // Keys will have the form "/chr1/raw".
                result.add(key.substring(1, rawPos));
            }
        }
        return result;
    }

    @Override
    public List<GenericContinuousRecord> getRecords(String ref, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException, InterruptedException {
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        TDFDataset ds = getTDFDataset(ref, (Range)range);
        if (ds != null) {
            int nextPos = range.getFrom();
            int rangeEnd = range.getTo();
            int usefulStep = Math.max(1,range.getLength() / NOTIONAL_SCREEN_WIDTH);     // No need for more points than we have pixels.
            List<TDFTile> tiles = ds.getTiles(range.getFrom(), range.getTo());
            for (TDFTile t : tiles) {
                for (int i = 0; i < t.getSize() && nextPos <= rangeEnd; i++) {
                    int datumEnd = t.getEndPosition(i);
                    if (nextPos < datumEnd) {
                        int datumStart = t.getStartPosition(i);
                        // If there's a gap before the data starts, fill it with NaNs.
                        if (datumStart == nextPos + 1 && usefulStep > 2) {
                            // Special case.  TDF formatter occasionally leaves a gap of one base between tiles.  This isn't a real NaN.
                            LOG.debug("Skipping NaN hole at " + nextPos);
                        } else {
                            while (nextPos < datumStart && nextPos <= rangeEnd) {
                                result.add(GenericContinuousRecord.valueOf(ref, nextPos, Float.NaN));
                                nextPos += usefulStep;
                            }
                        }
                        float datum = t.getValue(0, i);
                        LOG.debug("Tile " + i + " from " + datumStart + " to " + datumEnd + "=" + datum);
                        while (nextPos < datumEnd && nextPos <= rangeEnd) {
                            result.add(GenericContinuousRecord.valueOf(ref, nextPos, datum));
                            nextPos += usefulStep;
                        }
                    }
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        }
        return result;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public void close() {
        tdf.close();
    }

    /**
     * Given a range, determine the TDF zoom-factor which would be appropriate for that range.
     *
     * @param range
     * @return
     */
    private TDFDataset getTDFDataset(String ref, Range range) {
        int refLen = LocationController.getInstance().getReferenceLength(ref);
        ref = MiscUtils.homogenizeSequence(ref);
        int rangeLen = range.getLength();

        // Only do this calculation the first time through.
        if (maxZoom < 0) {
            Collection<String> dsNames = tdf.getDatasetNames();
            for (String dsName: dsNames) {
                // Dataset names expected to be of the form /1/z0/mean.  We want to find the highest zoom
                // stored in the file.  We have to be on the lookout for references with unhomogenised names.
                String[] split = dsName.split("/");
                if (split.length == 4 && MiscUtils.homogenizeSequence(split[1]).equals(ref) && split[2].startsWith("z") && split[3].equals("mean")) {
                    int zoom = Integer.valueOf(split[2].substring(1));
                    if (zoom > maxZoom) {
                        maxZoom = zoom;
                    }
                } else if (split.length == 3 && MiscUtils.homogenizeSequence(split[1]).equals(ref) && split[2].equals("raw")) {
                    rawUnhomogenised = dsName;
                }
            }
        }
        // The desired zoom is the one for which the screen is filled.
        TDFDataset result = null;
        int zoom = (int)(Math.log(refLen / rangeLen) / LOG2);
        if (zoom <= maxZoom) {
            LOG.info("Using zoomed dataset " + zoom);
            result = tdf.getDataset(ref, zoom, WindowFunction.mean);
            if (result == null) {
                result = tdf.getDataset("chr" + ref, zoom, WindowFunction.mean);
            }
        }
        if (result == null) {
            result = tdf.getDataset(rawUnhomogenised);
        }
        return result;
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.CONTINUOUS;
    }

    @Override
    public final String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }
}
