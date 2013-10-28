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
