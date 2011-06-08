/*
 *    Copyright 2011 University of Toronto
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
