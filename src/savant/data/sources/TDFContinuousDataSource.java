/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.data.sources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
import savant.data.types.GenericContinuousRecord;
import savant.util.Resolution;

/**
 *
 * @author tarkvara
 */
public class TDFContinuousDataSource extends GenericContinuousDataSource {

    private static final Log LOG = LogFactory.getLog(TDFContinuousDataSource.class);
    final TDFReader tdf;
    final URI uri;

    public TDFContinuousDataSource(URI uri) throws IOException {
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
    public List<GenericContinuousRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        TDFDataset ds = getTDFDataset(reference, range);
        if (ds != null) {
            List<TDFTile> tiles = ds.getTiles(range.getFromAsInt(), range.getToAsInt());
            for (TDFTile t : tiles) {
                for (int i = 0; i < t.getSize(); i++) {
                    int pos = t.getStartPosition(i) + 1;
                    if (pos >= range.getFromAsInt()) {
                        if (pos <= range.getToAsInt()) {
                            result.add(GenericContinuousRecord.valueOf(reference, pos, t.getValue(0, i)));
                        } else {
                            break;
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
    private TDFDataset getTDFDataset(String reference, RangeAdapter range) {
        return tdf.getDataset(String.format("/%s/raw", reference));
    }
}
