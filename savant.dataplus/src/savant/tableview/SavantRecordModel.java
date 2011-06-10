/*
 *    Copyright 2009-2011 University of Toronto
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

package savant.tableview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import net.sf.samtools.SAMRecord.SAMTagAndValue;
import org.apache.commons.lang.StringUtils;

import savant.api.adapter.TrackAdapter;
import savant.data.types.BAMIntervalRecord;
import savant.data.types.BedRecord;
import savant.data.types.Block;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.GenericPointRecord;
import savant.data.types.Record;
import savant.data.types.SequenceRecord;
import savant.data.types.TabixIntervalRecord;

/**
 *
 * @author mfiume
 */
public class SavantRecordModel {

    private static final Class SC = String.class;
    private static final Class IC = Integer.class;
    private static final Class DC = Double.class;
    private static final Class BC = Boolean.class;

    private static final Class[] SEQUENCE_COLUMN_CLASSES = { SC };
    private static final String[] SEQUENCE_COLUMN_NAMES = { "Sequence" };

    private static final Class[] POINT_COLUMN_CLASSES = { SC, IC, SC };
    private static final String[] POINT_COLUMN_NAMES = { "Reference", "Position", "Description" };

    private static final Class[] INTERVAL_COLUMN_CLASSES = { SC, IC, IC, SC };
    private static final String[] INTERVAL_COLUMN_NAMES = { "Reference", "From", "To", "Description" };

    private static final Class[] BAM_COLUMN_CLASSES = {   SC,     SC,         IC,         IC,         BC,         BC,         IC,     SC,     SC,         IC,         BC,             IC,                   SC};
    private static final String[] BAM_COLUMN_NAMES =  {   "Name", "Sequence", "Length",   "Position", "First",    "Strand",   "MQ",   "BQs",  "CIGAR",    "MatePos",  "MateStrand",   "InferredInsertSize", "Attributes" };

    private static final Class[] BED_COLUMN_CLASSES = { SC,         IC,     IC,     SC,     DC,      SC,        IC,         IC,         SC,         IC,             SC,         SC  };
    private static final String[] BED_COLUMN_NAMES = {"Reference", "Start", "End",  "Name", "Score", "Strand", "ThickStart", "ThickEnd", "ItemRGB", "BlockCount", "BlockSizes", "BlockStarts"};

    private static final Class[] CONTINUOUS_COLUMN_CLASSES = { SC, IC, DC };
    private static final String[] CONTINUOUS_COLUMN_NAMES = { "Reference", "Position", "Value" };

    public static List<Object[]> getDataForTrack(TrackAdapter t) {

        List<Object[]> result = new ArrayList<Object[]>();

        if (t.getDataInRange() == null) { return result; }

        int id = 1;

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                for (Record r : t.getDataInRange()) {
                    result.add(new Object[] { id++, ((SequenceRecord)r).getSequence() });
                }
                break;
            case INTERVAL_BAM:
                for (Record r : t.getDataInRange()) {
                    BAMIntervalRecord b = (BAMIntervalRecord) r;
                    List<Object> s = new ArrayList<Object>();
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
                    result.add(s.toArray());
                }
                break;
            case POINT_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericPointRecord b = (GenericPointRecord) r;
                    result.add(new Object[] { id++, b.getReference(), b.getPoint().getPosition(), b.getDescription() });
                }
                break;
            case INTERVAL_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericIntervalRecord b = (GenericIntervalRecord) r;
                    result.add(new Object[] { id++, b.getReference(), b.getInterval().getStart(), b.getInterval().getEnd(), b.getDescription() });
                }
                break;
            case INTERVAL_BED:
                for (Record r : t.getDataInRange()) {
                    BedRecord b = (BedRecord) r;
                    List s = new ArrayList();
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
                    String[] sizearr = new String[numBlocks];
                    String[] startarr = new String[numBlocks];
                    List<Block> blocks = b.getBlocks();
                    for (int i = 0; i < numBlocks; i++) {
                        sizearr[i] = blocks.get(i).getSize() + "";
                        startarr[i] = blocks.get(i).getPosition() + "";
                    }
                    s.add(StringUtils.join(sizearr, ", "));
                    s.add(StringUtils.join(startarr, ", "));
                    result.add(s.toArray());
                }
                break;
            case CONTINUOUS_GENERIC:
                for (Record r : t.getDataInRange()) {
                    GenericContinuousRecord b = (GenericContinuousRecord) r;
                    result.add(new Object[] { id++, b.getReference(), b.getPosition(), b.getValue() });
                }
                break;
            default:
                throw new UnsupportedOperationException("");
        }

        return result;
    }

    public static List<String> getColumnNamesForTrack(TrackAdapter t) {
        List<String> result = new ArrayList<String>();
        result.add("No.");
        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.addAll(Arrays.asList(SEQUENCE_COLUMN_NAMES));
                break;
            case INTERVAL_BAM:
                result.addAll(Arrays.asList(BAM_COLUMN_NAMES));
                break;
            case INTERVAL_BED:
                result.addAll(Arrays.asList(BED_COLUMN_NAMES));
                break;
            case INTERVAL_GENERIC:
                result.addAll(Arrays.asList(INTERVAL_COLUMN_NAMES));
                break;
            case CONTINUOUS_GENERIC:
                result.addAll(Arrays.asList(CONTINUOUS_COLUMN_NAMES));
                break;
            case POINT_GENERIC:
                result.addAll(Arrays.asList(POINT_COLUMN_NAMES));
                break;
            default:
                throw new UnsupportedOperationException(t.getDataSource().getDataFormat() + " is not supported");
        }
        return result;
    }

    public static List<Class> getColumnClassesForTrack(TrackAdapter t) {
        List<Class> result = new ArrayList<Class>();
        result.add(Integer.class);

        switch(t.getDataSource().getDataFormat()) {
            case SEQUENCE_FASTA:
                result.addAll(Arrays.asList(SEQUENCE_COLUMN_CLASSES));
                break;
            case INTERVAL_BAM:
                result.addAll(Arrays.asList(BAM_COLUMN_CLASSES));
                break;
            case INTERVAL_BED:
                result.addAll(Arrays.asList(BED_COLUMN_CLASSES));
                break;
            case INTERVAL_GENERIC:
                result.addAll(Arrays.asList(INTERVAL_COLUMN_CLASSES));
                break;
            case CONTINUOUS_GENERIC:
                result.addAll(Arrays.asList(CONTINUOUS_COLUMN_CLASSES));
                break;
            case POINT_GENERIC:
                result.addAll(Arrays.asList(POINT_COLUMN_CLASSES));
                break;
            default:
                throw new UnsupportedOperationException("");
        }
        return result;
    }
}
