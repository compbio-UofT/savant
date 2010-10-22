/*
 *    Copyright 2010 University of Toronto
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

/*
 * IntervalPacker.java
 * Created on Feb 1, 2010
 */

package savant.util;

import savant.data.types.Interval;
import savant.data.types.IntervalRecord;

import java.util.*;

/**
 * Utility class to do build the data structures necessary to draw packed intervals.
 * @author vwilliams
 */
public class IntervalPacker {

    private List<Object> data;

    private static final double ONE_MILLIONTH = Math.pow(10, -6);

    public IntervalPacker(List<Object> data) {
        this.data = data;
    }

//    public Map<Integer, ArrayList<IntervalRecord>> pack(int breathingSpace) {
    public ArrayList<List<IntervalRecord>> pack(int breathingSpace) {

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

            long intervalEnd = inter.getEnd();
            long intervalStart = inter.getStart();

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

    public boolean intersects(Interval i1, Interval i2) {
        // FIXME: i think this should be i1.getStart() < i2.getEnd()-1 && i2.getStart() < i1.getEnd()-1;
        return i1.getStart() < i2.getEnd() && i2.getStart() < i1.getEnd();
    }

    private boolean intersectsOne(List<Interval> intervalList, Interval inter, int gap) {
        for (Interval i1 : intervalList) {
            if (intersects(i1,inter)) {
                return true;
            }
            else {
                // check for breathing room
                if ((Math.abs(inter.getStart() - i1.getEnd()) < gap) || (Math.abs(i1.getStart() - inter.getEnd()) < 5)) {
                    return true; // not enough space between the intervals

                }
            }
        }
        return false;
    }


}
