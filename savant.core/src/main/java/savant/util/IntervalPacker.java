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

import java.util.*;

import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;


/**
 * Utility class to do build the data structures necessary to draw packed intervals.
 * 
 * @author vwilliams
 */
public class IntervalPacker {

    private List<Record> data;

    private static final double ONE_MILLIONTH = Math.pow(10, -6);

    public IntervalPacker(List<Record> data) {
        this.data = data;
    }

    public List<List<IntervalRecord>> pack(int breathingSpace) {

        int numdata = data.size();

        // Initialize some data structures to be filled by scanning through data
        ArrayList<List<IntervalRecord>> levels = new ArrayList<List<IntervalRecord>>();

        TreeMap<Double, Integer> rightMostPositions = new TreeMap<Double, Integer>();
        PriorityQueue<Integer> availableLevels = new PriorityQueue<Integer>();

        int highestLevel = -1; // highest level currently available to use

        // scan through data, keeping track of which intervals fit in which levels
        for (int i = 0; i < numdata; i++) {
            IntervalRecord record = (IntervalRecord)data.get(i);
            Interval inter = record.getInterval();

            int intervalEnd = inter.getEnd();
            int intervalStart = inter.getStart();

            // check for bogus intervals here
            if (!(intervalEnd >= intervalStart) || intervalEnd < 0 || intervalStart < 0)  continue;

            int level; // level we're going to use for this interval
            if (!rightMostPositions.isEmpty()) {
                SortedMap<Double, Integer> headMap = rightMostPositions.headMap((double)intervalStart);
                if (headMap != null && !headMap.isEmpty()) {
                    Iterator<Double> positions = headMap.keySet().iterator();
                    while (positions.hasNext()) {
                        Double position = positions.next();
                        availableLevels.add(headMap.get(position));
                        positions.remove();
                    }
                }
            }

            Integer availableLevel = availableLevels.poll();
            if (availableLevel == null) {
                level = ++highestLevel;
                List<IntervalRecord> newLevel = new ArrayList<IntervalRecord>();
                levels.add(newLevel);
            } else {
                level = availableLevel;
            }

            levels.get(level).add(record);
            double tieBreaker = (double)(level+1) * ONE_MILLIONTH;
            rightMostPositions.put((double)intervalEnd+(double)breathingSpace-tieBreaker, level);
        }

        return levels;


    }

    private boolean intersectsOne(List<Interval> intervalList, Interval inter, int gap) {
        for (Interval i2 : intervalList) {
            if (i2.intersects(inter)) {
                return true;
            } else {
                // check for breathing room
                if ((Math.abs(inter.getStart() - i2.getEnd()) < gap) || (Math.abs(i2.getStart() - inter.getEnd()) < 5)) {
                    return true; // not enough space between the intervals

                }
            }
        }
        return false;
    }


}
