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

package savant.diff;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.ContinuousRecord;
import savant.api.data.DataFormat;
import savant.api.util.TrackUtils;
import savant.data.types.GenericContinuousRecord;
import savant.util.MiscUtils;
import savant.api.util.Resolution;

/**
 * The actual data-source implemented by the Diff plugin.  Takes two existing continuous data-sources
 * and creates a third data-source based on the difference between them.
 *
 * @author tarkvara
 */
public class DiffDataSource implements DataSourceAdapter<ContinuousRecord> {
    private static final Log LOG = LogFactory.getLog(DiffDataSource.class);

    /** Our two input data-sources. */
    DataSourceAdapter<? extends ContinuousRecord> inputA, inputB;
    
    private URI uri;

    /**
     * Construct a data-source for the given diff:// URI.
     */
    public DiffDataSource(URI uri) throws URISyntaxException {
        this.uri = uri;
    }

    /**
     * Set of references is whatever's common to both input tracks.
     */
    @Override
    public Set<String> getReferenceNames() {
        Set<String> result = new HashSet<String>();
        if (inputsAttached()) {
            // In a perfect world, we could just intersect the two sets and be done.
            // In practice, some tracks will store chromosome names like "chr1", "chr2", etc.,
            // while others will use "1", "2".  We reconcile these as best we can.
            Set<String> aRefs = inputA.getReferenceNames();
            Set<String> bRefs = inputB.getReferenceNames();
            for (String a: aRefs) {
                String goodA = MiscUtils.homogenizeSequence(a);     // Reduce "chr1" to "1".
                String match = null;
                for (String b: bRefs) {
                    String goodB = MiscUtils.homogenizeSequence(b); // Reduce to "1" before comparing.
                    if (goodA.equals(goodB)) {
                        match = b;
                        break;
                    }
                }
                if (match != null) {
                    result.add(goodA);
                    bRefs.remove(match);
                }
            }
        }
        return result;
    }

    /**
     * The number of records we return will be based on the first input.  In some cases,
     * the second input may not happen to have a point at the expected location, so
     * we may have to interpolate.
     * 
     * The data-sources are expected to return records in order, so we can count on that
     * to simplify our task.
     */
    @Override
    public List<ContinuousRecord> getRecords(String ref, RangeAdapter range, Resolution res) throws IOException {
        List<ContinuousRecord> result = null;
        if (inputsAttached()) {
            List<? extends ContinuousRecord> aRecords = inputA.getRecords(ref, range, res);
            List<? extends ContinuousRecord> bRecords = inputB.getRecords(ref, range, res);

            result = new ArrayList<ContinuousRecord>(aRecords.size());
            int j = 0;
            ContinuousRecord recB = bRecords.get(j);
            for (int i = 0; i < aRecords.size(); i++) {
                ContinuousRecord recA = aRecords.get(i);
                int pos = recA.getPosition();

                // Figure out which record in B corresponds to A.
                while (recB.getPosition() < pos && j + 1 < bRecords.size()) {
                    j++;
                    recB = bRecords.get(j);
                }
                
                // For the purposes of this demonstration, we'll treat NaNs as zero.
                float value = recA.getValue();
                if (Float.isNaN(value)) {
                    value = 0.0f;
                }
                value -= interpolate(bRecords, j, pos);
                result.add(GenericContinuousRecord.valueOf(ref, pos, value));
            }
        }
        return result;
    }

    /**
     * Because the two input data-sources may be returning differing numbers of records,
     * we may have to interpolate.  Arbitrarily, we decide that inputA provides the
     * bench-mark and inputB gets interpolated.
     */
    private float interpolate(List<? extends ContinuousRecord> bRecords, int j, int pos) {
        float result = 0.0f;
        if (j < bRecords.size()) {
            ContinuousRecord recB = bRecords.get(j);

            if (recB.getPosition() == pos || j == 0) {
                // Simple case.  We have a data-point at the exact position.
                result = recB.getValue();
            } else if (recB.getPosition() > pos) {
                // If we got here, recB is further on in the chromosome than pos, so we need to interpolate with the preceding data-point.
                ContinuousRecord prevRecB = bRecords.get(j - 1);
                float weight = (float)(pos - prevRecB.getPosition()) / (recB.getPosition() - prevRecB.getPosition());
                result = prevRecB.getValue() * weight + recB.getValue() * (1.0f - weight);
            }
        }
        return Float.isNaN(result) ? 0.0f : result;
    }

    /**
     * Get the URI corresponding to the difference between our input tracks.
     * @return
     */
    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return uri.toString();
    }

    /**
     * No extra clean-up to be done.
     */
    @Override
    public void close() {
    }

    /**
     * @return <code>CONTINUOUS_GENERIC</code>
     */
    @Override
    public DataFormat getDataFormat() {
        return DataFormat.CONTINUOUS_GENERIC;
    }

    /**
     * @return "Reference", "Position", "Value"
     */
    @Override
    public String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }

    /**
     * A do-nothing stub because continuous tracks don't have any strings to look up.
     */
    @Override
    public void loadDictionary() {
    }

    /**
     * A do-nothing stub because continuous tracks don't have any strings to look up.
     */
    @Override
    public List<BookmarkAdapter> lookup(String string) {
        return null;
    }

    /**
     * Utility method to calculate the URI which will be used to specify the difference between the two given tracks.
     */
    public static URI getDiffURI(TrackAdapter trackA, TrackAdapter trackB) throws URISyntaxException {
        return new URI("diff://(" + trackA.getDataSource().getURI() + ";" + trackB.getDataSource().getURI() + ")");
    }

    /**
     * Search through our existing tracks to find the ones which match our input URIs.
     * Because we can't rely on Savant loading the tracks in any known order, we may have
     * to call this method repeatedly before the inputs get hooked up.
     */
    private boolean inputsAttached() {
        if (inputA != null && inputB != null) {
            return true;
        }
        try {
            String uriString = uri.getRawSchemeSpecificPart().substring(3);     // Trim off the initial "diff://("
            uriString = uriString.substring(0, uriString.length() - 1);         // Trim off the final ")"
            int delimiterPos = uriString.indexOf(';');
            if (delimiterPos > 0) {
                URI uriA = new URI(uriString.substring(0, delimiterPos));
                URI uriB = new URI(uriString.substring(delimiterPos + 1));

                TrackAdapter[] availableTracks = TrackUtils.getTracks(DataFormat.CONTINUOUS_GENERIC);
                for (TrackAdapter t: availableTracks) {
                    URI u = t.getDataSource().getURI();
                    if (u.equals(uriA)) {
                        inputA = t.getDataSource();
                    } else if (u.equals(uriB)) {
                        inputB = t.getDataSource();
                    }
                }
            }
        } catch (URISyntaxException x) {
            LOG.error(String.format("Unable to parse %s as a valid URI.", x));
        }
        return inputA != null && inputB != null;
    }
}
