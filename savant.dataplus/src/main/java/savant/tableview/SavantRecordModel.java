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
package savant.tableview;

import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import savant.api.adapter.TrackAdapter;
import savant.api.data.Block;
import savant.api.data.Record;
import savant.api.data.SequenceRecord;
import savant.data.sources.TabixDataSource;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.BEDIntervalRecord;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.GenericPointRecord;
import savant.data.types.TabixIntervalRecord;

/**
 *
 * @author mfiume
 */
public class SavantRecordModel {

    private static final Class sc = String.class;
    private static final Class ic = Integer.class;
    private static final Class lc = Long.class;
    private static final Class dc = Double.class;
    private static final Class bc = Boolean.class;

    private static Class[] sequenceColumnClasses = { sc };
    private static String[] sequenceColumnNames = { "Sequence" };

    private static Class[] pointColumnClasses = { sc, lc, sc };
    private static String[] pointColumnNames = { "Reference", "Position", "Description" };

    private static Class[] intervalColumnClasses = { sc, lc, lc, sc };
    private static String[] intervalColumnNames = { "Reference", "From", "To", "Description" };

    private static Class[] bamColumnClasses = {   sc,     sc,         ic,         ic,         bc,         bc,         ic,     sc,     sc,         ic,         bc,             ic,                   sc};
    private static String[] bamColumnNames =  {   "Name", "Sequence", "Length",   "Position", "First",    "Strand",   "MQ",   "BQs",  "CIGAR",    "MatePos",  "MateStrand",   "InferredInsertSize", "Attributes" };

    private static Class[] bedColumnClasses = { sc,         lc,     lc,     sc,     dc,      sc,        ic,         ic,         sc,         ic,             sc,         sc  };
    private static String[] bedColumnNames = {"Reference", "Start", "End",  "Name", "Score", "Strand", "ThickStart", "ThickEnd", "ItemRGB", "BlockCount", "BlockSizes", "BlockStarts"};

    private static Class[] continuousColumnClasses = { sc, lc, dc };
    private static String[] continuousColumnNames = { "Reference", "Position", "Value" };

    public static Vector getDataForTrack(TrackAdapter t) {

        Vector result = new Vector();
        Vector s;

        if (t.getDataInRange() == null) { return result; }

        int id = 1;

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                for (Record r : t.getDataInRange()) {
                    s = new Vector();
                    s.add(id++);
                    s.add(new String(((SequenceRecord) r).getSequence()));
                    result.add(s);
                }
                break;
            case INTERVAL_BAM:
                for (Record r : t.getDataInRange()) {
                    BAMIntervalRecord b = (BAMIntervalRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getSamRecord().getReadName());
                    s.add(b.getSamRecord().getReadString());
                    s.add(b.getSamRecord().getReadLength());
                    s.add(b.getSamRecord().getAlignmentStart());
                    s.add(b.getSamRecord().getFirstOfPairFlag());
                    s.add(!b.getSamRecord().getReadNegativeStrandFlag());
                    s.add(b.getSamRecord().getMappingQuality());
                    s.add(b.getSamRecord().getBaseQualityString());
                    s.add(b.getSamRecord().getCigar());
                    s.add(b.getSamRecord().getMateAlignmentStart());
                    s.add(!b.getSamRecord().getMateNegativeStrandFlag());
                    s.add(b.getSamRecord().getInferredInsertSize());
                    String atts = "";
                    if (b.getSamRecord().getAttributes().size() > 0) {
                        for (SAMTagAndValue v : b.getSamRecord().getAttributes()) {
                            atts += v.tag + ":" + v.value + "; ";
                        }
                        atts.substring(0,atts.length()-2);
                    }
                    s.add(atts);
                    result.add(s);
                }
                break;
            case POINT_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericPointRecord b = (GenericPointRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getReference());
                    s.add(b.getPoint().getPosition());
                    s.add(b.getDescription());
                    result.add(s);
                }
                break;
            case INTERVAL_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericIntervalRecord b = (GenericIntervalRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getReference());
                    s.add(b.getInterval().getStart());
                    s.add(b.getInterval().getEnd());
                    s.add(b.getDescription());
                    result.add(s);
                }
                break;
            case INTERVAL_BED:
                for (Record r : t.getDataInRange()) {
                    BEDIntervalRecord b = (BEDIntervalRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getReference());
                    s.add(b.getInterval().getStart());
                    s.add(b.getInterval().getEnd());
                    s.add(b.getName());
                    s.add(b.getScore());
                    s.add(b.getStrand());
                    s.add(b.getThickStart());
                    s.add(b.getThickEnd());
                    s.add(b.getItemRGB());
                    int numBlocks = b.getBlocks().size();
                    s.add(numBlocks);
                    String sizes = "";
                    String starts = "";
                    String[] sizearr = new String[numBlocks];
                    String[] startarr = new String[numBlocks];
                    List<Block> blocks = b.getBlocks();
                    for (int i = 0; i < numBlocks; i++) {
                        sizearr[i] = blocks.get(i).getSize() + "";
                        startarr[i] = blocks.get(i).getPosition() + "";
                    }
                    s.add(StringUtils.join(sizearr, ", "));
                    s.add(StringUtils.join(startarr, ", "));
                    result.add(s);
                }
                break;
            case CONTINUOUS_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericContinuousRecord b = (GenericContinuousRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getReference());
                    s.add(b.getPosition());
                    s.add(b.getValue());
                    result.add(s);
                }
                break;
            case TABIX:
                for (Record r : t.getDataInRange()) {
                    TabixIntervalRecord b = (TabixIntervalRecord) r;
                    s = new Vector();
                    s.add(id++);
                    s.add(b.getReference());
                    s.add(b.getInterval().getStart());
                    s.add(b.getInterval().getEnd()+1); // tabix intervals are not end-inclusive; our intervals are
                    for (String str : b.getOtherValues()) {
                        s.add(str);
                    }
                    result.add(s);
                }
                break;
            default:
                throw new UnsupportedOperationException("");
        }

        return result;
    }

    public static Vector getColumnNamesForTrack(TrackAdapter t) {
        Vector result = new Vector();
        result.add("No.");
        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.addAll(Arrays.asList(sequenceColumnNames));
                break;
            case INTERVAL_BAM:
                result.addAll(Arrays.asList(bamColumnNames));
                break;
            case INTERVAL_BED:
                result.addAll(Arrays.asList(bedColumnNames));
                break;
            case INTERVAL_GENERIC:
                result.addAll(Arrays.asList(intervalColumnNames));
                break;
            case CONTINUOUS_GENERIC:
                result.addAll(Arrays.asList(continuousColumnNames));
                break;
            case POINT_GENERIC:
                result.addAll(Arrays.asList(pointColumnNames));
                break;
            case TABIX:
                String[] tabixColumnNames = null;
                if (t.getDataInRange().size() > 0) {
                    TabixIntervalRecord r = ((TabixIntervalRecord) t.getDataInRange().get(0));
                    int numfields = ((TabixIntervalRecord) t.getDataInRange().get(0)).getOtherValues().size();
                    tabixColumnNames = new String[3+numfields];
                    for (int i = 0; i < numfields; i++) {
                        tabixColumnNames[i+3] = "Field" + (i+3+1);
                    }
                } else {
                    tabixColumnNames = new String[3];
                }
                tabixColumnNames[0] = "Reference";
                tabixColumnNames[1] = "Start";
                tabixColumnNames[2] = "End";
                result.addAll(Arrays.asList(tabixColumnNames));
                break;
            default:
                throw new UnsupportedOperationException(t.getDataSource().getDataFormat() + " is not supported");
        }
        return result;
    }

    public static Vector getColumnClassesForTrack(TrackAdapter t) {
        Vector result = new Vector();
        result.add(Integer.class);

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.addAll(Arrays.asList(sequenceColumnClasses));
                break;
            case INTERVAL_BAM:
                result.addAll(Arrays.asList(bamColumnClasses));
                break;
            case INTERVAL_BED:
                result.addAll(Arrays.asList(bedColumnClasses));
                break;
            case INTERVAL_GENERIC:
                result.addAll(Arrays.asList(intervalColumnClasses));
                break;
            case CONTINUOUS_GENERIC:
                result.addAll(Arrays.asList(continuousColumnClasses));
                break;
            case POINT_GENERIC:
                result.addAll(Arrays.asList(pointColumnClasses));
                break;
            case TABIX:
                Class[] tabixColumnClasses = null;
                if (t.getDataInRange().size() > 0) {
                    TabixIntervalRecord r = ((TabixIntervalRecord) t.getDataInRange().get(0));
                    int numfields = ((TabixIntervalRecord) t.getDataInRange().get(0)).getOtherValues().size();
                    tabixColumnClasses = new Class[3+numfields];
                    for (int i = 0; i < numfields; i++) {
                        tabixColumnClasses[i+3] = sc;
                    }
                } else {
                    tabixColumnClasses = new Class[3];
                }
                tabixColumnClasses[0] = sc;
                tabixColumnClasses[1] = lc;;
                tabixColumnClasses[2] = lc;
                result.addAll(Arrays.asList(tabixColumnClasses));
                break;
            default:
                throw new UnsupportedOperationException("");
        }
        return result;
    }
}
