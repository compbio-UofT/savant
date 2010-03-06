/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.format.util.data.interval;

import savant.format.DebutFile;
import savant.model.IntervalRecord;
import savant.util.DataUtils;
import savant.util.Range;
import savant.util.comparator.IntervalRecordComparator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class IntervalRecordGetter {

    public static List<IntervalRecord> getData(DebutFile dFile, Range r, IntervalTreeNode n) throws IOException {
        List<IntervalRecord> result = new ArrayList<IntervalRecord>();
        getData(dFile, result, r, n);
        Collections.sort(result, new IntervalRecordComparator());
        return result;
    }

    private static void getData(DebutFile dFile, List<IntervalRecord> data, Range r, IntervalTreeNode n) throws IOException {

        if (intersects(r, n.range)) {
            //System.out.println("\tBin : " + n.range + " overlaps range " + r);
            data.addAll(getIntersectingIntervals(dFile, r,n));
            for (IntervalTreeNode child : n.children) {
                if (child != null && child.subtreeSize > 0 && intersects(child.range,r)) {
                    getData(dFile, data, r,child);
                }
            }
        }
    }

    private static boolean intersects(Range r1, Range r2) {
        //System.out.println("Does " + r1  + " intersect " + r2 + "? " + (r1.getFrom() <= r2.getTo() && r1.getTo() >= r2.getFrom()));
        return (r1.getFrom() <= r2.getTo() && r1.getTo() >= r2.getFrom());
    }

    private static List<IntervalRecord> getIntersectingIntervals(DebutFile dFile, Range r, IntervalTreeNode n) throws IOException {

        //System.out.println("\t\tGetting intersecting intervals");
        //System.out.println("Node range: " + n.range + " size: " + n.size);

        List<IntervalRecord> data = new ArrayList<IntervalRecord>();

        if (n.size > 0) {
            dFile.seekSuper(n.startByte);
            //System.out.println("\tStart byte: " + n.startByte);
            //System.out.println("\tSeeking to : " + dFile.getFilePointer());
            for (int i = 0; i < n.size; i++) {


                List<Object> record = DataUtils.readBinaryRecord(dFile, dFile.getFields());
                IntervalRecord ir = DataUtils.convertRecordToInterval(record, dFile.getFileType(), dFile.getFields());

                if (intersects(ir.getInterval().getRange(),r)) {
                    data.add(ir);
                }
            }
        }
        return data;
    }

    public static List<IntervalRecord> getRecordsInBin(DebutFile dFile, IntervalTreeNode n) throws IOException {

        List<IntervalRecord> recs = new ArrayList<IntervalRecord>(n.size);
        if (n.size > 0) {
            dFile.seekSuper(n.startByte);
            for (int i = 0; i < n.size; i++) {
                List<Object> record = DataUtils.readBinaryRecord(dFile, dFile.getFields());
                IntervalRecord ir = DataUtils.convertRecordToInterval(record, dFile.getFileType(), dFile.getFields());
                recs.add(ir);
            }
        }
        
        return recs;
    }
}
