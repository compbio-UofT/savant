/*
 *    Copyright 2010-2011 University of Toronto
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

import java.util.Comparator;
import savant.api.data.Interval;

import savant.api.data.IntervalRecord;


/**
 * Comparator to sort intervals by their left position.
 * 
 * @author mfiume
 */
public class IntervalRecordComparator implements Comparator<IntervalRecord> {

    @Override
    public int compare(IntervalRecord o1, IntervalRecord o2) {

        Interval i1 = o1.getInterval();
        Interval i2 = o2.getInterval();

        if (i1.getStart() < i2.getStart()) {
            return -1;
        } else if (i1.getStart() > i2.getStart()) {
            return 1;
        } else {
            return 0;
        }
    }
}
