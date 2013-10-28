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
package savant.api.data;

/**
 * Summarises the various data formats which are used internally by Savant.
 * Intended to be more abstract than the FileType enum.
 *
 * @author tarkvara
 */
public enum DataFormat {

    SEQUENCE,
    POINT,
    CONTINUOUS,
    GENERIC_INTERVAL,
    RICH_INTERVAL,
    ALIGNMENT,
    VARIANT;

    @Override
    public String toString() {
        switch (this) {
            case SEQUENCE:
                return "Sequence";
            case POINT:
                return "Point";
            case CONTINUOUS:
                return "Continuous";
            case GENERIC_INTERVAL:
                return "Generic Interval";
            case RICH_INTERVAL:
                return "Rich Interval";
            case ALIGNMENT:
                return "Alignment";
            case VARIANT:
                return "Variant";
        }
        return null;
    }
}