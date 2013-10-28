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
 * Constants shared between various classes in the Savant SQL data-source plugin.
 *
 * @author tarkvara
 */
public interface SQLConstants {
    public static final String CHROM = "CHROM";
    public static final String START = "START";
    public static final String END = "END";
    public static final String VALUE = "VALUE";
    public static final String NAME = "NAME";
    public static final String NAME2 = "NAME2";
    public static final String SCORE = "SCORE";
    public static final String STRAND = "STRAND";
    public static final String THICK_START = "THICK_START";
    public static final String THICK_END = "THICK_END";
    public static final String ITEM_RGB = "ITEM_RGB";
    public static final String BLOCK_STARTS_RELATIVE = "BLOCK_STARTS_RELATIVE";
    public static final String BLOCK_STARTS_ABSOLUTE = "BLOCK_STARTS_ABSOLUTE";
    public static final String BLOCK_ENDS = "BLOCK_ENDS";
    public static final String BLOCK_SIZES = "BLOCK_SIZES";
    public static final String SPAN = "SPAN";
    public static final String COUNT = "COUNT";
    public static final String OFFSET = "OFFSET";
    public static final String FILE = "FILE";
    public static final String LOWER_LIMIT = "LOWER_LIMIT";
    public static final String DATA_RANGE = "DATA_RANGE";

    public static final String NO_COLUMN = "(none)";
}
