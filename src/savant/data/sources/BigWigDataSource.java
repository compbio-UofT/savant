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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.data.types.GenericContinuousRecord;
import savant.util.NetworkUtils;


/**
 * Data source which uses UCSC's BigWig format to display a continuous track.
 * An alternative to TDF.
 *
 * @author tarkvara
 */
public class BigWigDataSource extends DataSource<GenericContinuousRecord> {

    private static final Log LOG = LogFactory.getLog(BigWigDataSource.class);

    private BBFileReader bbReader;

    public BigWigDataSource(URI uri) throws IOException {
        bbReader = new BBFileReader(NetworkUtils.getNeatPathFromURI(uri), new TribbleStream(uri));
        if (!bbReader.isBigWigFile()) {
            throw new IOException("Input is not a BigWig file.");
        }
    }

    @Override
    public Set<String> getReferenceNames() {
        Set<String> result = new HashSet<String>();
        for (String chr: bbReader.getChromosomeNames()) {
            result.add(chr);
        }
        return result;
    }

    @Override
    public List<GenericContinuousRecord> getRecords(String ref, RangeAdapter range, Resolution resolution, RecordFilterAdapter filt) throws IOException, InterruptedException {
        List<GenericContinuousRecord> result = new ArrayList<GenericContinuousRecord>();
        try {
            BigWigIterator wigIterator = bbReader.getBigWigIterator(ref, range.getFrom(), ref, range.getTo(), false);
            int nextPos = range.getFrom();
            int rangeEnd = range.getTo();
            while (wigIterator.hasNext()) {
                WigItem wig = wigIterator.next();

                int datumStart = wig.getStartBase();
                int datumEnd = wig.getEndBase();
                float value = wig.getWigValue();

                while (nextPos < datumStart && nextPos <= rangeEnd) {
                    result.add(GenericContinuousRecord.valueOf(ref, nextPos, Float.NaN));
                    nextPos++;
                }
                while (nextPos < datumEnd && nextPos <= rangeEnd) {
                    result.add(GenericContinuousRecord.valueOf(ref, nextPos, value));
                    nextPos++;
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (RuntimeException ignored) {
            // If BigWig reader has no data in the given range, it throws a RuntimeException.  Really?  Who does that?
        }
        return result;
    }

    @Override
    public URI getURI() {
        return new File(bbReader.getBBFilePath()).toURI();
    }

    @Override
    public void close() {
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.CONTINUOUS;
    }

    @Override
    public final String[] getColumnNames() {
        return GenericContinuousRecord.COLUMN_NAMES;
    }
    
    private static class TribbleStream extends org.broad.tribble.util.SeekableStream {

        SeekableStream samStream;
        long pos;
        
        TribbleStream(URI uri) throws IOException {
            samStream = NetworkUtils.getSeekableStreamForURI(uri, true);
            pos = 0;
        }

        @Override
        public void seek(long position) throws IOException {
            samStream.seek(position);
            pos = position;
        }

        @Override
        public long position() throws IOException {
            return pos;
        }

        @Override
        public boolean eof() throws IOException {
            return samStream.eof();
        }


        @Override
        public long length() {
            return samStream.length();
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("TribbleStream.read() not implemented.");
        }
        
        @Override
        public void readFully(byte[] buf) throws IOException {
            int numRead = samStream.read(buf, 0, buf.length);
            pos += numRead;
        }
    }
}
