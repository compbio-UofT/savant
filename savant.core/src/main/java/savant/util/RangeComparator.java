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

import savant.util.Range;

import java.util.Comparator;

/**
 *
 * @author mfiume
 */
public class RangeComparator implements Comparator{
    public int compare(Object o1, Object o2){

        Range r1 = (Range) o1;
        Range r2 = (Range) o2;

        if (r1.getFrom() < r2.getFrom()) {
            return -1;
        } else if (r1.getFrom() > r2.getFrom()) {
            return 1;
        } else {
            return 0;
        }
    }
}