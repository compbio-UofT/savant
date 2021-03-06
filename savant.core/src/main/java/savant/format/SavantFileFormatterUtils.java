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
package savant.format;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import savant.api.data.Interval;
import savant.api.data.Block;
import savant.api.data.IntervalRecord;
import savant.api.util.DialogUtils;
import savant.api.data.Strand;
import savant.data.types.*;
import savant.file.FieldType;
import savant.file.FileType;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.util.Range;
import savant.view.dialog.DataFormatForm;


/**
 * Utilities for manipulating data files.
 * @author vwilliams
 */
public class SavantFileFormatterUtils {

    // Constants to use for size calculations on output fields.
    public static final int LONG_FIELD_SIZE     = 8;
    public static final int INT_FIELD_SIZE      = 4;
    public static final int SHORT_FIELD_SIZE    = 2;
    public static final int CHAR_FIELD_SIZE     = 2;
    public static final int BYTE_FIELD_SIZE     = 1;
    public static final int BOOLEAN_FIELD_SIZE  = 1;
    public static final int DOUBLE_FIELD_SIZE   = 8;
    public static final int FLOAT_FIELD_SIZE    = 4;

    /**
     * Given an unformatted file, try to guess what it should be formatted as.
     *
     * @param path    path to an unformatted file
     * @return a guess at the file type (or null if we have no good guess)
     */
    public static FileType guessFileTypeFromPath(String path) {

        // Get the file extension
        String extension = MiscUtils.getExtension(path).toLowerCase();

        if (extension.equals("bam")) {
            return FileType.INTERVAL_BAM;
        }
        if (extension.equals("bed")) {
            return FileType.INTERVAL_BED;
        }
        if (extension.equals("gff")) {
            return FileType.INTERVAL_GFF;
        }
        if (extension.equals("gtf")) {
            return FileType.INTERVAL_GTF;
        }
        if (extension.equals("wig") || extension.equals("bedgraph")) {
            return FileType.CONTINUOUS_WIG;
        }
        if (extension.equals("fa") || extension.equals("fasta")) {
            return FileType.SEQUENCE_FASTA;
        }
        if (extension.equals("gz")) {
            return FileType.TABIX;
        }
        if (extension.equals("bw") || extension.equals("bigwig")) {
            return FileType.CONTINUOUS_BIGWIG;
        }
        if (extension.equals("bb") || extension.equals("bigbed")) {
            return FileType.INTERVAL_BIGBED;
        }
        if (extension.equals("tdf")) {
            return FileType.CONTINUOUS_TDF;
        }
        if (extension.equals("psl")) {
            return FileType.INTERVAL_PSL;
        }
        if (extension.equals("vcf")) {
            return FileType.INTERVAL_VCF;
        }
        if (extension.equals("gene") || extension.equals("knowngene")) {
            return FileType.INTERVAL_KNOWNGENE;
        }
        if (extension.equals("refgene")) {
            return FileType.INTERVAL_REFGENE;
        }

        // None of the generic formats have any kind of standard extension.
        return null;
    }
    
    public static File getFormattedFile(String inputPath, FileType ft) {
        String outputPath = inputPath;
        switch (ft) {
            case CONTINUOUS_WIG:
                outputPath += ".tdf";
                break;
            case INTERVAL_BED:
            case INTERVAL_BED1:
            case INTERVAL_GENERIC:
            case INTERVAL_GFF:
            case INTERVAL_GTF:
            case INTERVAL_PSL:
            case INTERVAL_VCF:
            case INTERVAL_KNOWNGENE:
            case INTERVAL_REFGENE:
                outputPath += ".gz";
                break;
            default:
                break;
        }
        return new File(outputPath);
    }

    public static List<Object> readBinaryRecord(RandomAccessFile in, List<FieldType> fields) throws IOException {

        List<Object> record = new ArrayList<Object>(fields.size());

        for (FieldType ft : fields) {
            if (ft == FieldType.IGNORE) { continue; }

            switch(ft) {
                case INTEGER:
                    record.add(in.readInt());
                    break;
                case ITEMRGB:
                    int r = in.readInt();
                    int g = in.readInt();
                    int b = in.readInt();
                    record.add(ItemRGB.valueOf(r,g,b));
                    break;
                case DOUBLE:
                    record.add(in.readDouble());
                    break;
                case FLOAT:
                    record.add(in.readFloat());
                    break;
                case BLOCKS:
                    int numBlocks = in.readInt();
                    List<Block> blocks = new ArrayList<Block>(numBlocks);
                    for (int i = 0; i < numBlocks; i++) {
                        int start = in.readInt();
                        int size = in.readInt();
                        blocks.add(Block.valueOf(start,size));
                    }
                    record.add(blocks);
                    break;
                case STRING:
                    int len = in.readInt();
                    if (len > 10000) {
                        throw new IOException("Tried to read binary string of length " + len + " characters");
                    }
                    String s = "";
                    for (int i = 0; i < len; i++) {
                        s += (char) in.readByte();
                    }
                    record.add(s);
                    break;
                case CHAR:
                    record.add((char) in.readByte());
                    break;
                case RANGE:
                    int start = in.readInt();
                    int end = in.readInt();
                    record.add(new Range(start,end));
                    break;
                case LONG:
                    record.add(in.readLong());
                    break;
                case IGNORE:
                    break;
                default:
                    break;
            }
        }

        return record;
    }

    /** IO **/

    public static int getRecordSize(List<Object> record, List<FieldType> fields) {

        int recordSize = 0;

        int recIndex = 0;
        for (FieldType ft : fields) {
            if  (ft == FieldType.IGNORE) { continue; }

            switch(ft) {
                case STRING:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE + ((String) record.get(recIndex)).length()* SavantFileFormatterUtils.BYTE_FIELD_SIZE;
                    break;
                case INTEGER:
                    recordSize +=  SavantFileFormatterUtils.INT_FIELD_SIZE;
                    break;
                case ITEMRGB:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE*3;
                    break;
                case BLOCKS:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE*((List<Block>) record.get(9)).size();
                    break;
                case CHAR:
                    recordSize +=  SavantFileFormatterUtils.BYTE_FIELD_SIZE;
                    break;
                case DOUBLE:
                    recordSize += SavantFileFormatterUtils.DOUBLE_FIELD_SIZE;
                    break;
                case FLOAT:
                    recordSize += SavantFileFormatterUtils.FLOAT_FIELD_SIZE;
                    break;
                case BOOLEAN:
                    // TODO: change?!
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE;
                    break;
                default:
                    throw new UnsupportedOperationException("Data Utils.getRecordSize: Not implemented yet!");
            }

            recIndex++;
        }

        return recordSize;
    }

    public static IntervalRecord convertRecordToInterval(List<Object> record, FileType fileType, List<FieldType> fields) {
        IntervalRecord ir = null;
        switch(fileType) {
            case INTERVAL_GENERIC:
                ir = convertRecordToGenericInterval(record,fields);
                break;
            case INTERVAL_BED:
                ir = convertRecordToBEDInterval(record,fields);
                break;
            case INTERVAL_GFF:
                //TODO: fix... this won't work unless indexes correspond to start / end indicies
                ir = convertRecordToGenericInterval(record,fields);
                break;
            default:
                break;
        }

        return ir;
    }

    private static IntervalRecord convertRecordToGenericInterval(List<Object> record, List<FieldType> fields) {
        GenericIntervalRecord ir;
        
        //TODO: Is this the best place for this?
        if(record.size() > 3){
            ir = GenericIntervalRecord.valueOf((String)record.get(0), Interval.valueOf((Integer) record.get(1),(Integer) record.get(2)), (String) record.get(3));
        } else {
            ir = GenericIntervalRecord.valueOf((String)record.get(0), Interval.valueOf((Integer) record.get(1),(Integer) record.get(2)), "");
        }
        
        return (IntervalRecord) ir;
    }

//    // TODO: make it actually return a GFF IntervalRecord
//    private static IntervalRecord convertRecordToGFFInterval(List<Object> record, List<FieldType> fields) {
//        GenericIntervalRecord ir = GenericIntervalRecord.valueOf(Interval.valueOf((Integer) record.get(3),(Integer) record.get(4)), (String) record.get(1));
//        return (IntervalRecord) ir;
//    }

    private static IntervalRecord convertRecordToBEDInterval(List<Object> record, List<FieldType> fields) {

        int start = (Integer) record.get(1);
        int end = (Integer) record.get(2);
        
        String ref = (String) record.get(0);
        
        int numFields = record.size();
        
        String name = "";
        Float score = 0.0f;
        Strand strand = SavantFileFormatterUtils.getStrand("+");
        int thickStart = 0;
        int thickEnd = 0;
        ItemRGB rgb = ItemRGB.valueOf(-1,-1,-1);
        List<Block> blocks = null;
        
        if (numFields > 3) { name = (String) record.get(3); }
        if (numFields > 4) {
            Object o = record.get(4);
            if (o instanceof Integer) {
                score = ((Integer)o).floatValue();
            } else if (o instanceof Float) {
                score = (Float) record.get(4);
            }
        }

        int intervallength = Interval.valueOf(start,end).getLength();
        if (numFields > 5) { strand = SavantFileFormatterUtils.getStrand((String) record.get(5)); }
        if (numFields > 6) { thickStart = (Integer)record.get(6); }
        if (numFields > 7) { thickEnd = (Integer)record.get(7); } else { thickEnd = intervallength; }
        if (numFields > 8) { rgb = (ItemRGB) record.get(8); }
        if (numFields > 9) { blocks = (List<Block>) record.get(9); } else {
            blocks = new ArrayList<Block>();
            blocks.add(Block.valueOf(0,intervallength));
        }

        BEDIntervalRecord ir = BEDIntervalRecord.valueOf(
                ref,
                start,
                end,
                name,
                score,
                strand,
                thickStart,
                thickEnd,
                rgb,
                blocks
                );
        
       return ir;
    }

    public static Strand getStrand(String strand) {
        char c = strand.charAt(0);
        if (c == '-') {
            return Strand.REVERSE;
        } else {
            return Strand.FORWARD;
        }
    }

    /**
     * The user has tried to open an unformatted file.  Prompt them to format it.
     *
     * @param uri the file URI which the user has tried to open.
     */
    public static void promptUserToFormatFile(URI uri) {
        if (DialogUtils.askYesNo("Unformatted File", String.format("<html><i>%s</i> does not appear to be formatted. Format now?</html>", NetworkUtils.getFileName(uri))) == DialogUtils.YES) {
            new DataFormatForm(DialogUtils.getMainWindow(), uri, true).setVisible(true);
        }
    }
}
