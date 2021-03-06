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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.util.BlockCompressedInputStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broad.tabix.TabixReader;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.adapter.VariantDataSourceAdapter;
import savant.api.data.DataFormat;
import savant.api.util.Resolution;
import savant.data.types.GFFIntervalRecord;
import savant.data.types.TabixIntervalRecord;
import savant.util.IndexCache;
import savant.util.ColumnMapping;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;

/**
 * DataSource for reading records from a Tabix file.  These can be either a plain Interval
 * records, or full-fledged Bed records.
 *
 * @author mfiume, tarkvara
 */
public class TabixDataSource extends DataSource<TabixIntervalRecord> implements VariantDataSourceAdapter {
    private static final Log LOG = LogFactory.getLog(TabixDataSource.class);

    TabixReader reader;

    /** Defines mapping between column indices and the data-fields we're interested in. */
    ColumnMapping mapping;

    /** Names of the columns, in the same order they appear in the file. */
    String[] columnNames;
    
    /**
     * Extra columns after the columns which are used in the mapping.  For VCF files, this
     * is where we find the information about the participants.
     */
    String[] extraColumns;

    private URI uri;

    public TabixDataSource(URI uri) throws IOException {

        File indexFile = IndexCache.getIndexFile(uri, "tbi", "gz");
        SeekableStream baseStream = NetworkUtils.getSeekableStreamForURI(uri);
        this.uri = uri.normalize();
        this.reader = new TabixReader(baseStream, indexFile);

        // Check to see how many columns we actually have, and try to initialise a mapping.
        inferMapping();
    }

    /**
     * Check our source file to see how many columns we have.  If possible, figure
     * out their names.
     *
     * This is only intended as a temporary hack until we get a more flexible DataFormatForm
     * which lets you set up column->field mappings.
     */
    private void inferMapping() throws IOException {
        BlockCompressedInputStream input = new BlockCompressedInputStream(NetworkUtils.getSeekableStreamForURI(uri));
        String line = TabixReader.readLine(input);
        if (line == null) {
            throw new EOFException("End of file");
        }

        // If we're lucky, the file starts with a comment line with the field-names in it.
        // That's what UCSC puts there, as does Savant.  In some files (e.g. VCF), this
        // magical comment line may be preceded by a ton of metadata comment lines.
        String lastCommentLine = null;
        String commentChar = Character.toString(reader.getCommentChar());
        while (line.startsWith(commentChar)) {
            lastCommentLine = line;
            line = TabixReader.readLine(input);
        }
        input.close();

        int numCols = 1;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                numCols++;
            }
        }

        // The chrom, start, and end fields are generally enough to uniquely determine which of the well-known formats we have.
        if (matchesMapping(ColumnMapping.BED)) {
            // It's a Bed file, but we can't set the mapping, because it may have a variable number of actual columns.
            columnNames = new String[] { "chrom", "start", "end", "name", "score", "strand", "thickStart", "thickEnd", "itemRgb", "blockCount", "blockStarts", "blockSizes" };
        } else if (matchesMapping(ColumnMapping.KNOWNGENE)) {
            columnNames = new String[] { "Name", "Reference", "Strand", "Transcription start", "Transcription end", "Coding start", "Coding end", null, null, null, "Unique ID", "Alternate name", null, null, null };
            mapping = ColumnMapping.KNOWNGENE;
        } else if (matchesMapping(ColumnMapping.REFSEQ) && numCols == 16) {
            columnNames = new String[] { null, "Transcript name", "Reference", "Strand", "Transcription start", "Transcription end", "Coding start", "Coding end", null, null, null, "Unique ID", "Gene name", null, null, null };
            mapping = ColumnMapping.REFSEQ;
        } else if (matchesMapping(ColumnMapping.GFF)) {
            // Based on chrom/start/end fields, it's impossible to distinguish between GFF and GTF files.
            // We have to look at column 8, which will have special values for GTF.
            String attributes = line.substring(line.lastIndexOf('\t') + 1);
            if (attributes.contains("gene_id") && attributes.contains("transcript_id")) {
                columnNames = new String[] { "Reference", "Source", "Feature", "Start", "End", "Score", "Strand", "Frame", "Attributes" };
                mapping = ColumnMapping.GTF;
            } else {
                columnNames = new String[] { "Reference", "Source", "Feature", "Start", "End", "Score", "Strand", "Frame", "Group" };
                mapping = ColumnMapping.GFF;
            }
        } else if (matchesMapping(ColumnMapping.PSL)) {
            columnNames = new String[] { "Matches", "Mismatches", "Matches that are part of repeats", "Number of 'N' bases", "Number of inserts in query", "Number of bases inserted in query", "Number of inserts in target", "Number of bases inserted in target", "Strand", "Query sequence name", "Query sequence size", "Alignment start in query", "Alignment end in query", "Target sequence name", "Target sequence size", "Alignment start in target", "Alignment end in target", null, null, null };
            mapping = ColumnMapping.PSL;
        } else if (matchesMapping(ColumnMapping.VCF)) {
            columnNames = new String[] { "Reference", "Position", "ID", "Reference base(s)", "Alternate non-reference alleles", "Quality", "Filter", "Additional information", "Format" };
            mapping = ColumnMapping.VCF;
        }

        if (lastCommentLine != null) {
            if (mapping == null) {
                columnNames = lastCommentLine.substring(1).split("\\t");
                // If user has screwed up the comment line in a bed file, make sure it doesn't lead us astray.
                columnNames[reader.getChromColumn()] = "chrom";
                columnNames[reader.getStartColumn()] = "start";
                if (reader.getEndColumn() >= 0) {
                    columnNames[reader.getEndColumn()] = "end";
                }
                mapping = ColumnMapping.inferMapping(columnNames, false);
            } else if (mapping == ColumnMapping.VCF) {
                // For VCF files, save off the participant IDs stored in the extra columns.
                String[] allColumns = lastCommentLine.substring(1).split("\\t");
                if (allColumns.length > columnNames.length) {
                    extraColumns = new String[allColumns.length - columnNames.length];
                    for (int i = columnNames.length; i < allColumns.length; i++) {
                        extraColumns[i - columnNames.length] = allColumns[i];
                    }
                } else {
                    // A defective VCF file with no participants.
                    extraColumns = new String[0];
                }
            }
        }
    }

    private boolean matchesMapping(ColumnMapping mapping) {
        return reader.getChromColumn() == mapping.chrom && reader.getStartColumn() == mapping.start && reader.getEndColumn() == mapping.end;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TabixIntervalRecord> getRecords(String ref, RangeAdapter r, Resolution res, RecordFilterAdapter filt) throws IOException, InterruptedException {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            TabixReader.Iterator i = reader.query(MiscUtils.homogenizeSequence(ref) + ":" + r.getFrom() + "-" + (r.getTo()+1));

            if (i != null) {
                String line = null;
                int start = -1;
                int end = -1;
                Map<Integer, Integer> ends = new HashMap<Integer, Integer>();
                while ((line = i.next()) != null) {
                    // Note: count is used to uniquely identify records in same location
                    // Assumption is that iterator will always give records in same order
                    TabixIntervalRecord rec = TabixIntervalRecord.valueOf(line, mapping);
                    if (filt == null || filt.accept(rec)) {
                        RangeAdapter r2 = rec.getExpandedRange(r);
                        if (r2 != null) {
                            return getRecords(ref, r2, res, filt);
                        }
                        if (rec.getInterval().getStart() == start) {
                            end = rec.getInterval().getEnd();
                            if (ends.get(end) == null) {
                                ends.put(end, 0);
                            } else {
                                int count = ends.get(end)+1;
                                ends.put(end, count);
                                rec.setCount(count);
                            }
                        } else {
                            start = rec.getInterval().getStart();
                            end = rec.getInterval().getEnd();
                            ends.clear();
                            ends.put(end, 0);
                            rec.setCount(0);
                        }
                        if (!absorbGFFRecord(rec, result)) {
                            result.add(rec);
                        }
                    }
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException x) {
            // If the chromosome isn't found, the Tabix library manifests it by throwing an ArrayIndexOutOfBoundsException.
            LOG.info(String.format("Reference \"%s\" not found.", ref));
        }
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {}

    @Override
    public Set<String> getReferenceNames() {
        return reader.getReferenceNames();
    }

    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * Tabix can hold data which is actually INTERVAL_GENERIC or INTERVAL_TABIX.
     */
    @Override
    public final DataFormat getDataFormat() {
        return mapping.format;
    }

    @Override
    public final String[] getColumnNames() {
        return columnNames;
    }
    
    /**
     * For VCF files, the extra columns contain participant IDs.
     * @return extra columns after the columns which were mapped by the format
     */
    @Override
    public String[] getParticipants() {
        return extraColumns;
    }

    /**
     * Hack for Mike to make RefSeq files give preference to the alternate (gene) name.
     */
    public boolean prefersAlternateName() {
        return mapping == ColumnMapping.REFSEQ;
    }
    
    private boolean absorbGFFRecord(TabixIntervalRecord rec, List<TabixIntervalRecord> recs) {
        if (rec instanceof GFFIntervalRecord) {
            if (((GFFIntervalRecord)rec).getFeatureType().equals("chromosome")) {
                // Some GFF files contain a useless top-level feature for the entire chromosome.
                return true;
            }
            // Find a plausible parent GTF.  It should be the last record, but not always.
            GFFIntervalRecord child = (GFFIntervalRecord)rec;
            for (int i = recs.size() - 1; i >= 0; i--) {
                GFFIntervalRecord parent = (GFFIntervalRecord)recs.get(i);
                if (parent.absorbRecord(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
