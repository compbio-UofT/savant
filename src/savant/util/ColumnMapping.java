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
package savant.util;

import org.broad.tabix.TabixWriter.Conf;

import savant.file.DataFormat;


/**
 * Specifies the mapping between columns in a tab-delimited file and fields of interest
 * to Savant.  Typically used for Bed, gene, and GFF files.
 *
 * @author tarkvara
 */
public class ColumnMapping {

    public static final String INTERVAL_GENERIC_HEADER = "chrom\tstart\tend\tname";
    public static final String BED_HEADER = "chrom\tstart\tend\tname\tscore\tstrand\tthickStart\tthickEnd\titemRgb\tblockCount\tblockSizes\tblockStarts";
    public static final String KNOWNGENE_HEADER = "name\tchrom\tstrand\ttxStart\ttxEnd\tcdsStart\tcdsEnd\texonCount\texonStarts\texonEnds\tproteinID\talignID";
    public static final String REFGENE_HEADER = "bin\tname\tchrom\tstrand\ttxStart\ttxEnd\tcdsStart\tcdsEnd\texonCount\texonStarts\texonEnds\tid\tname2\tcdsStartStat\tcdsEndStat\texonFrames";
    public static final String GFF_HEADER = "seqname\tsource\tfeature\tstart\tend\tscore\tstrand\tframe\tgroup";
    public static final String PSL_HEADER = "matches\tmisMatches\trepMatches\tnCount\tqNumInsert\tqBaseInsert\ttNumInsert\ttBaseInsert\tstrand\tqName\tqSize\tqStart\tqEnd\ttName\ttSize\ttStart\ttEnd\tblockCount\tblockSizes\tqStarts\ttStarts";
    public static final String VCF_HEADER = "CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT";

    /** Official set of Bed columns from UCSC FAQ.  Columns from name onward are optional. */
    public static final ColumnMapping BED = inferMapping(BED_HEADER, false);

    /** Set of columns observed in knownGene tables from UCSC FAQ. */
    public static final ColumnMapping KNOWNGENE = inferMapping(KNOWNGENE_HEADER, false);

    /** Set of columns observed in RefSeq genes from UCSC FAQ.  Similar to knownGene, but adds a bin column, and the name2 column is different. */
    public static final ColumnMapping REFSEQ = inferMapping("bin\t" + REFGENE_HEADER, false);

    /** Set of columns for GFF and GTF files.  The only difference is that in GTF the final column is called "attributes" rather than "group". */
    public static final ColumnMapping GFF = inferMapping(GFF_HEADER, true);

    /** Set of columns for PSL files.  Not sure if this will work quite right. */
    public static final ColumnMapping PSL = inferMapping(PSL_HEADER, false);

    /** Set of columns for VCF files.  Note that there is no end column. */
    public static final ColumnMapping VCF = inferMapping(VCF_HEADER, true);

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

    public final boolean oneBased;

    private ColumnMapping(DataFormat format, int chrom, int start, int end, int name, int score, int strand, int thickStart, int thickEnd, int itemRGB, int blockStartsRelative, int blockStartsAbsolute, int blockEnds, int blockSizes, int name2, boolean oneBased) {
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
        this.oneBased = oneBased;
    }

    public Conf getTabixConf(int flags) {
        return new Conf(flags, chrom + 1, start + 1, end + 1, '#', 0);
    }

    /**
     * Factory method used to map GenericInterval formats.
     */
    public static ColumnMapping createIntervalMapping(int chrom, int start, int end, int name, boolean oneBased) {
        return new ColumnMapping(DataFormat.INTERVAL_GENERIC, chrom, start, end, name, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, oneBased);
    }

    /**
     * Factory method used to map BedInterval formats.
     */
    public static ColumnMapping createRichIntervalMapping(int chrom, int start, int end, int name, int score, int strand, int thickStart, int thickEnd, int itemRGB, int blockStartsRelative, int blockStartsAbsolute, int blockEnds, int blockSizes, int name2, boolean oneBased) {
        return new ColumnMapping(DataFormat.INTERVAL_RICH, chrom, start, end, name, score, strand, thickStart, thickEnd, itemRGB, blockStartsRelative, blockStartsAbsolute, blockEnds, blockSizes, name2, oneBased);
    }

    /**
     * Factory method used to create interval formats given a tab-delimited list of column names.
     * This is what we expect/hope to find in the first line of any Bed file.
     */
    public static ColumnMapping inferMapping(String header, boolean oneBased) {
        if (header.charAt(0) == '#') {
            header = header.substring(1);
        }
        String[] columnNames = header.split("\\t");
        return inferMapping(columnNames, oneBased);
    }

    /**
     * Factory method used to create interval formats given an array of column names.
     * As a side-effect, substitutes natural language names for column names which it recognises.
     */
    public static ColumnMapping inferMapping(String[] columnNames, boolean oneBased) {

        // It's either bed, or it's not any format we recognise.
        int chrom = -1, start = -1, end = -1;
        int name = -1, score = -1, strand = -1, thickStart = -1, thickEnd = -1, itemRGB = -1, blockStarts = -1, blockEnds = -1, blockSizes = -1, name2 = -1;
        boolean bed = false;

        for (int i = 0; i < columnNames.length; i++) {
            String colName = columnNames[i].toLowerCase();
            if (colName.equals("bin")) {
                columnNames[i] = null;
            } else if (colName.equals("chrom") || colName.equals("seqname") || colName.equals("genoname")) {
                chrom = i;
                columnNames[i] = "Reference";
            } else if (colName.equals("start") || colName.equals("txstart") || colName.equals("chromstart") || colName.equals("pos") || colName.equals("genostart")) {
                start = i;
                columnNames[i] = "Start";
            } else if (colName.equals("end") || colName.equals("txend") || colName.equals("chromend") || colName.equals("genoend")) {
                end = i;
                columnNames[i] = "End";
            } else if (colName.equals("name") || colName.equals("feature") || colName.equals("qname") || colName.equals("repname")) {
                name = i;
                columnNames[i] = "Name";
            } else if (colName.equals("score")) {
                score = i;
                columnNames[i] = "Score";
                bed = true;
            } else if (colName.equals("strand")) {
                strand = i;
                columnNames[i] = "Strand";
                bed = true;
            } else if (colName.equals("thickstart") || colName.equals("cdsstart")) {
                thickStart = i;
                columnNames[i] = "Thick start";
                bed = true;
            } else if (colName.equals("thickend") || colName.equals("cdsend")) {
                thickEnd = i;
                columnNames[i] = "Thick end";
                bed = true;
            } else if (colName.equals("itemrgb") || colName.equals("reserved")) {
                itemRGB = i;
                columnNames[i] = null;  // No point in showing colour in the table when we can show it visually.
                bed = true;
            } else if (colName.equals("blockcount") || colName.equals("exoncount")) {
                columnNames[i] = "Block count";
                bed = true;
            } else if (colName.equals("blockstarts") || colName.equals("exonstarts") || colName.equals("tstarts") || colName.equals("chromstarts")) {
                blockStarts = i;
                columnNames[i] = null;
                bed = true;
            } else if (colName.equals("blocksizes") || colName.equals("exonsizes")) {
                blockSizes = i;
                columnNames[i] = null;
                bed = true;
            } else if (colName.equals("exonends")) {
                blockEnds = i;
                columnNames[i] = null;
                bed = true;
            } else if (colName.equals("name2") || colName.equals("proteinid")) {
                name2 = i;
                columnNames[i] = "Alternate name";
                bed = true;
            }
        }
        if (bed) {
            // We have enough extra columns to justify using a Bed track.
            return ColumnMapping.createRichIntervalMapping(chrom, start, end, name, score, strand, thickStart, thickEnd, itemRGB, -1, blockStarts, blockEnds, blockSizes, name2, oneBased);
        } else {
            return ColumnMapping.createIntervalMapping(chrom, start, end, name, oneBased);
        }
    }
}
