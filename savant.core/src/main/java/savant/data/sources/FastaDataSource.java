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
package savant.data.sources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.util.SeekableBufferedStream;
import net.sf.samtools.util.SeekableStream;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.adapter.SequenceDataSourceAdapter;
import savant.api.data.DataFormat;
import savant.api.data.SequenceRecord;
import savant.api.util.Resolution;
import savant.util.FastaUtils;
import savant.util.FastaUtils.IndexEntry;
import savant.util.IndexCache;
import savant.util.NetworkUtils;
import savant.view.tracks.TrackCreationEvent;
import savant.view.tracks.TrackFactory.TrackCreationListener;

/**
 * New-style Fasta data-source which reads directly from a .fa file.
 *
 * @author tarkvara
 */
public class FastaDataSource extends DataSource<SequenceRecord> implements SequenceDataSourceAdapter {
    private URI fastaURI;
    private SeekableStream stream;
    private Map<String, FastaUtils.IndexEntry> index;

    public FastaDataSource(URI uri, TrackCreationListener l) throws IOException {
        fastaURI = uri;
        try {
            File faiFile = IndexCache.getIndexFile(uri, "fai", "fa");
            index = FastaUtils.readIndex(faiFile);
        } catch (FileNotFoundException x) {
            if (l != null) {
                l.handleEvent(new TrackCreationEvent("Generating index...", -1.0));
            }
            index = FastaUtils.createIndex(uri, new File(x.getMessage()));
        }
        stream = new SeekableBufferedStream(NetworkUtils.getSeekableStreamForURI(uri));
    }

    @Override
    public int getLength(String ref) {
        return index.get(ref).length;
    }

    @Override
    public List<SequenceRecord> getRecords(String ref, RangeAdapter r, Resolution res, RecordFilterAdapter filt) throws IOException {

        IndexEntry entry = index.get(ref);
        if (entry != null) {
            byte[] sequence = new byte[r.getLength()];
            int i = 0;
            int lineNum = (r.getFrom() - 1) / entry.lineLength;
            long offset = entry.offset + lineNum * (entry.lineLength + 1) + (r.getFrom() - 1) % entry.lineLength;
            stream.seek(offset);

            int c;
            while ((c = stream.read()) >= 0 && i < sequence.length) {
                if (c != '\n') {
                    sequence[i++] = (byte)Character.toUpperCase(c);
                }
            }

            return Arrays.asList(SequenceRecord.valueOf(ref, sequence));
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public Set<String> getReferenceNames() {
        return index.keySet();
    }

    @Override
    public URI getURI() {
        return fastaURI;
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.SEQUENCE;
    }

    @Override
    public final String[] getColumnNames() {
        return new String[] { "Sequence" };
    }
}
