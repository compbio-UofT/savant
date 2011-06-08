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
import savant.controller.LocationController;
import savant.data.types.GenericContinuousRecord;
import savant.file.DataFormat;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.util.Resolution;

/**
 * Data source which uses Broad Institute's TDF format to display a continuous track.
 * Intended as a more efficient replacement for our existing GenericContinuousTrack.
 *
 * @author tarkvara
 */
public class TDFDataSource implements DataSource<GenericContinuousRecord> {

    private static final Log LOG = LogFactory.getLog(TDFDataSource.class);
    private static final double LOG2 = Math.log(2.0);
    private static final int NOTIONAL_SCREEN_WIDTH = 2000;

    final TDFReader tdf;
    final URI uri;
    int maxZoom = -1;

    public TDFDataSource(URI uri) throws IOException {
        tdf = TDFReader.getReader(uri.getPath());
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
    public List<GenericContinuousRecord> getRecords(String ref, RangeAdapter range, Resolution resolution) throws IOException {
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
                        LOG.debug("Tile " + i + " from " + datumStart + " to " + datumEnd);
                        // If there's a gap before the data starts, fill it with NaNs.
                        while (nextPos < datumStart && nextPos <= rangeEnd) {
                            result.add(GenericContinuousRecord.valueOf(ref, nextPos += usefulStep, Float.NaN));
                        }
                        float datum = t.getValue(0, i);
                        while (nextPos < datumEnd && nextPos <= rangeEnd) {
                            result.add(GenericContinuousRecord.valueOf(ref, nextPos += usefulStep, datum));
                        }
                    }
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

        if (maxZoom < 0) {
            Collection<String> dsNames = tdf.getDatasetNames();
            String prefix = String.format("/%s/z", ref);
            for (String dsName: dsNames) {
                // Dataset names will be of the form /chr1/zoom/0.  We want to find the highest value
                // stored in the file.
                if (dsName.startsWith(prefix)) {
                    dsName = dsName.substring(prefix.length());
                    if (dsName.endsWith("/mean")) {
                        int zoom = Integer.valueOf(dsName.substring(0, dsName.length() - 5));
                        if (zoom > maxZoom) {
                            maxZoom = zoom;
                        }
                    }
                }
            }
        }
        // The desired zoom is the one for which the screen is filled.
        int zoom = (int)(Math.log(refLen / rangeLen) / LOG2);
        if (zoom <= maxZoom) {
            LOG.info("Using zoomed dataset " + zoom);
            return tdf.getDataset(ref, zoom, WindowFunction.mean);
        }
        return tdf.getDataset(String.format("/%s/raw", ref));
    }

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.CONTINUOUS_GENERIC;
    }

    @Override
    public Object getExtraData() {
        return null;
    }
}
