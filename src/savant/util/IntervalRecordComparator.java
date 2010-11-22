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

package savant.util;

import savant.data.types.IntervalRecord;
import savant.util.Range;

import java.util.Comparator;

/**
 * Comparator to sort intervals by their left position.
 * 
 * @author mfiume
 */
public class IntervalRecordComparator implements Comparator{

    public int compare(Object o1, Object o2){

        Range r1 = ((IntervalRecord) o1).getInterval().getRange();
        Range r2 = ((IntervalRecord) o2).getInterval().getRange();

        if (r1.getFrom() < r2.getFrom()) {
            return -1;
        } else if (r1.getFrom() > r2.getFrom()) {
            return 1;
        } else {
            return 0;
        }
    }
}
