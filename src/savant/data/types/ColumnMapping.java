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
package savant.data.types;

import savant.file.DataFormat;

/**
 * Specifies the mapping between columns in a tab-delimited file and fields of interest
 * to Savant.  Typically used for Bed, gene, and GFF files.
 *
 * @author tarkvara
 */
public class ColumnMapping {

    /** Official set of Bed columns from UCSC FAQ.  Columns from name onward are optional. */
    public static final ColumnMapping BED = ColumnMapping.getBedMapping(0, 1, 2, 3, 4, 5, 6, 7, 8, -1, 11, -1, 10, -1);

    /** Set of columns observed in KnownGene genes from UCSC FAQ. */
    public static final ColumnMapping GENE = ColumnMapping.getBedMapping(1, 3, 4, 0, 10, 2, 5, 6, -1, -1, 8, 9, -1, 11);

    /** Set of columns observed in RefSeq genes from UCSC FAQ.  Like KnownGene, but adds an extra bin column. */
    public static final ColumnMapping REFSEQ = ColumnMapping.getBedMapping(2, 4, 5, 1, 11, 3, 6, 7, -1, -1, 9, 10, -1, 12);

    /** Set of columns for GFF and GTF files.  The only difference is that in GTF the final column is called "attributes" rather than "group". */
    public static final ColumnMapping GFF = ColumnMapping.getBedMapping(0, 3, 4, 2, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1);

    /** Set of columns for PSL files.  Not sure if this will work quite right. */
    public static final ColumnMapping PSL = ColumnMapping.getBedMapping(13, 15, 16, 9, -1, 8, -1, -1, -1, -1, 20, -1, 18, -1);

    /** Set of columns for VCF files.  Note that there is no end column. */
    public static final ColumnMapping VCF = ColumnMapping.getIntervalMapping(0, 1, -1, 2);

    /** One of our own generic interval files. */
    public static final ColumnMapping GENERIC_INTERVAL = ColumnMapping.getIntervalMapping(0, 1, 2, 3);

    public final DataFormat format;

    public final int chrom;
    public final int start;
    public final int end;
    public final int name;
    public final int score;
    public final int strand;
    public final int thickStart;
    public final int thickEnd;
    public final int itemRGB;
    public final int blockStartsRelative;
    public final int blockStartsAbsolute;
    public final int blockEnds;
    public final int blockSizes;
    public final int name2;

    private ColumnMapping(DataFormat format, int chrom, int start, int end, int name, int score, int strand, int thickStart, int thickEnd, int itemRGB, int blockStartsRelative, int blockStartsAbsolute, int blockEnds, int blockSizes, int name2) {
        this.format = format;
        this.chrom = chrom;
        this.start = start;
        this.end = end;
        this.name = name;
        this.score = score;
        this.strand = strand;
        this.thickStart = thickStart;
        this.thickEnd = thickEnd;
        this.itemRGB = itemRGB;
        this.blockStartsAbsolute = blockStartsAbsolute;
        this.blockStartsRelative = blockStartsRelative;
        this.blockEnds = blockEnds;
        this.blockSizes = blockSizes;
        this.name2 = name2;
    }

    /**
     * Factory method used to map GenericInterval formats.
     */
    public static ColumnMapping getIntervalMapping(int chrom, int start, int end, int name) {
        return new ColumnMapping(DataFormat.INTERVAL_GENERIC, chrom, start, end, name, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
    }

    /**
     * Factory method used to map BedInterval formats.
     */
    public static ColumnMapping getBedMapping(int chrom, int start, int end, int name, int score, int strand, int thickStart, int thickEnd, int itemRGB, int blockStartsRelative, int blockStartsAbsolute, int blockEnds, int blockSizes, int name2) {
        return new ColumnMapping(DataFormat.INTERVAL_BED, chrom, start, end, name, score, strand, thickStart, thickEnd, itemRGB, blockStartsRelative, blockStartsAbsolute, blockEnds, blockSizes, name2);
    }
}
