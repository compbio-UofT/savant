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
package savant.sql;

/**
 * Defines one of several ways of mapping between Savant fields and SQL database columns.
 * The MappingFormat determines which of the columns in the ColumnMapping are actually
 * relevant.
 *
 * @author tarkvara
 */
public enum MappingFormat {
    CONTINUOUS_VALUE_COLUMN,   // Data values stored in a database column.
    CONTINUOUS_WIG,            // Data values stored in an external Wig (or Wib) file.
    INTERVAL_GENERIC,
    INTERVAL_RICH,
    EXTERNAL_FILE              // Table just contains path to BAM or BigWig file.
}
