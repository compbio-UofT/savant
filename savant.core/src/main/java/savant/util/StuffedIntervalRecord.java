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
package savant.util;

import java.util.ArrayList;
import java.util.List;

import savant.api.data.Interval;
import savant.api.data.IntervalRecord;


/**
 *
 * @author mfiume
 */
public class StuffedIntervalRecord implements IntervalRecord {

    private final int start, end;
    private final IntervalRecord originalRecord;

    public StuffedIntervalRecord(IntervalRecord record, int leftStuffing, int rightStuffing) {
        this.originalRecord = record;
        if (record.getInterval().getLength() == 0) {
            // Special case, it's an insertion.  We need at least one base in each direction to draw the diamond.
            leftStuffing = Math.max(1, leftStuffing);
            rightStuffing = Math.max(1, rightStuffing);
        }
        this.start = Math.max(0, record.getInterval().getStart() - leftStuffing);
        this.end = record.getInterval().getEnd() + rightStuffing;
    }

    @Override
    public String getReference() {
        return originalRecord.getReference();
    }

    @Override
    public Interval getInterval() {
        return Interval.valueOf(start, end);
    }

    @Override
    public String getName() {
        return originalRecord.getName();
    }

    @Override
    public int compareTo(Object o) {

        StuffedIntervalRecord other = (StuffedIntervalRecord) o;

        //compare ref
        if (!getReference().equals(other.getReference())) {
            return getReference().compareTo(other.getReference());
        }

        //compare interval
        int start = getInterval().getStart();
        int otherStart = other.getInterval().getStart();
        int end = getInterval().getEnd();
        int otherEnd = other.getInterval().getEnd();

        if (start == otherStart) {
            if (otherEnd < end) {
                return -1;
            } else if (otherEnd > end) {
                // Longer intervals reported first
                return 1;
            } else {
                return 0;
            }
        } else if (start < otherStart) {
            return -1;
        } else {
            return 1;
        }
    }

    private IntervalRecord getOriginalInterval() {
        return originalRecord;
    }

    public static List<List<IntervalRecord>> getOriginalIntervals(List<List<IntervalRecord>> pack) {

        List<List<IntervalRecord>> result = new ArrayList<List<IntervalRecord>>();

        for (List<IntervalRecord> list : pack) {
            List<IntervalRecord> subResult = new ArrayList<IntervalRecord>();
            for (IntervalRecord r : list) {
                subResult.add(((StuffedIntervalRecord)r).getOriginalInterval());
            }
            result.add(subResult);
        }

        return result;
    }
}
