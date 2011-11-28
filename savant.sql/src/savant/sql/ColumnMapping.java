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

import savant.api.util.SettingsUtils;

/**
 * Keeps track of the mapping between Savant's own internal fields and SQL column names.
 *
 * @author tarkvara
 */
public class ColumnMapping implements SQLConstants {
    public final MappingFormat format;
    public final String chrom;
    public final String start;
    public final String end;
    public final String value;
    public final String name;
    public final String name2;
    public final String score;
    public final String strand;
    public final String thickStart;
    public final String thickEnd;
    public final String itemRGB;
    public final String blockStartsRelative;
    public final String blockStartsAbsolute;
    public final String blockEnds;
    public final String blockSizes;

    // Wig/Wib fields
    public final String span;
    public final String count;
    public final String offset;
    public final String file;
    public final String lowerLimit;
    public final String dataRange;

    private ColumnMapping(MappingFormat format, String chrom, String start, String end, String value, String name, String score, String strand, String thickStart, String thickEnd, String itemRGB, String blockStartsRelative, String blockStartsAbsolute, String blockEnds, String blockSizes, String name2, String span, String count, String offset, String file, String lowerLimit, String dataRange) {
        this.format = format;
        this.chrom = chrom;
        this.start = start;
        this.end = end;
        this.value = value;
        this.name = NO_COLUMN.equals(name) ? null : name;
        this.score = NO_COLUMN.equals(score) ? null : score;
        this.strand = NO_COLUMN.equals(strand) ? null : strand;
        this.thickStart = NO_COLUMN.equals(thickStart) ? null : thickStart;
        this.thickEnd = NO_COLUMN.equals(thickEnd) ? null : thickEnd;
        this.itemRGB = NO_COLUMN.equals(itemRGB) ? null : itemRGB;
        this.blockStartsAbsolute = NO_COLUMN.equals(blockStartsAbsolute) ? null : blockStartsAbsolute;
        this.blockStartsRelative = (this.blockStartsAbsolute != null || NO_COLUMN.equals(blockStartsRelative)) ? null : blockStartsRelative;
        this.blockEnds = NO_COLUMN.equals(blockEnds) ? null : blockEnds;
        this.blockSizes = NO_COLUMN.equals(blockSizes) ? null : blockSizes;
        this.name2 = NO_COLUMN.equals(name2) ? null : name2;
        this.span = NO_COLUMN.equals(span) ? null : span;
        this.count = NO_COLUMN.equals(count) ? null : count;
        this.offset = NO_COLUMN.equals(offset) ? null : offset;
        this.file = NO_COLUMN.equals(file) ? null : file;
        this.lowerLimit = NO_COLUMN.equals(lowerLimit) ? null : lowerLimit;
        this.dataRange = NO_COLUMN.equals(dataRange) ? null : dataRange;
    }

    /**
     * Constructor used to map GenericContinuous formats.
     */
    public static ColumnMapping getContinuousMapping(String chrom, String start, String end, String value) {
        return new ColumnMapping(MappingFormat.CONTINUOUS_VALUE_COLUMN, chrom, start, end, value, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor used to map GenericInterval formats.
     */
    public static ColumnMapping getIntervalMapping(String chrom, String start, String end, String name) {
        return new ColumnMapping(MappingFormat.INTERVAL_GENERIC, chrom, start, end, null, name, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor used to map BEDInterval formats.
     */
    public static ColumnMapping getRichIntervalMapping(String chrom, String start, String end, String name, String score, String strand, String thickStart, String thickEnd, String itemRGB, String blockStartsRelative, String blockStartsAbsolute, String blockEnds, String blockSizes, String name2) {
        return new ColumnMapping(MappingFormat.INTERVAL_RICH, chrom, start, end, null, name, score, strand, thickStart, thickEnd, itemRGB, blockStartsRelative, blockStartsAbsolute, blockEnds, blockSizes, name2, null, null, null, null, null, null);
    }

    /**
     * Constructor used to map GenericContinuous formats.
     */
    public static ColumnMapping getWigMapping(String chrom, String start, String end, String span, String count, String offset, String file, String lowerLimit, String dataRange) {
        return new ColumnMapping(MappingFormat.CONTINUOUS_WIG, chrom, start, end, null, null, null, null, null, null, null, null, null, null, null, null, span, count, offset, file, lowerLimit, dataRange);
    }

    private static String findColumn(SQLDataSourcePlugin plugin, String settingName, Column[] columns) {
        String setting = SettingsUtils.getString(plugin, settingName);
        if (setting != null) {
            String[] names = setting.split(",");
            for (Column c: columns) {
                for (int i = 0; i < names.length; i++) {
                    if (c.name.equals(names[i])) {
                        return c.name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Restore a mapping from our saved settings.
     */
    public static ColumnMapping getSavedMapping(SQLDataSourcePlugin plugin, Column[] columns, boolean skipChrom) {
        // CHROM, START, and END are common to all mappings.
        String chrom = null;
        if (!skipChrom) {
            chrom = findColumn(plugin, CHROM, columns);
        }
        String start = findColumn(plugin, START, columns);
        String end = findColumn(plugin, END, columns);
        if ((chrom != null || skipChrom) && start != null && end != null) {
            // First check for Wig columns, which are unambiguous.
            String count = findColumn(plugin, COUNT, columns);
            String offset = findColumn(plugin, OFFSET, columns);
            String file = findColumn(plugin, FILE, columns);
            String lowerLimit = findColumn(plugin, LOWER_LIMIT, columns);
            String dataRange = findColumn(plugin, DATA_RANGE, columns);
            if (count != null && offset != null && file != null && lowerLimit != null && dataRange != null) {
                return getWigMapping(chrom, start, end, findColumn(plugin, SPAN, columns), count, offset, file, lowerLimit, dataRange);
            } else {
                String name = findColumn(plugin, NAME, columns);
                if (name != null) {
                    // We have a name field, but beyond that the division between INTERVAL_GENERIC and INTERVAL_BED is a little vague.
                    // Arbitrarily, we will decide that if there is a SCORE or STRAND column, we'll call it INTERVAL_BED.
                    String score = findColumn(plugin, SCORE, columns);
                    String strand = findColumn(plugin, STRAND, columns);
                    if (score != null || strand != null) {
                        return getRichIntervalMapping(chrom, start, end, name,
                                             score, strand,
                                             findColumn(plugin, THICK_START, columns), findColumn(plugin, THICK_END, columns),
                                             findColumn(plugin, ITEM_RGB, columns),
                                             findColumn(plugin, BLOCK_STARTS_RELATIVE, columns), findColumn(plugin, BLOCK_STARTS_ABSOLUTE, columns),
                                             findColumn(plugin, BLOCK_ENDS, columns), findColumn(plugin, BLOCK_SIZES, columns),
                                             findColumn(plugin, NAME2, columns));
                    } else {
                        // No score or strand fields.  We'll fall back on generic interval.
                        return getIntervalMapping(chrom, start, end, name);
                    }
                } else {
                    // No name field.  Maybe it's generic continuous.
                    String value = findColumn(plugin, VALUE, columns);
                    if (value != null) {
                        return getContinuousMapping(chrom, start, end, value);
                    }
                }
            }
        }
        return getIntervalMapping(chrom, start, end, null);
    }

    /**
     * Mappings have been set up.  Save them to savant.settings so they will be available
     * as defaults for subsequent runs.
     */
    public void save(SQLDataSourcePlugin plugin) {
        saveValue(plugin, CHROM, chrom);
        saveValue(plugin, START, start);
        saveValue(plugin, END, end);
        saveValue(plugin, VALUE, value);
        saveValue(plugin, NAME, name);
        saveValue(plugin, SCORE, score);
        saveValue(plugin, STRAND, strand);
        saveValue(plugin, THICK_START, thickStart);
        saveValue(plugin, THICK_END, thickEnd);
        saveValue(plugin, ITEM_RGB, itemRGB);
        saveValue(plugin, BLOCK_STARTS_RELATIVE, blockStartsRelative);
        saveValue(plugin, BLOCK_STARTS_ABSOLUTE, blockStartsAbsolute);
        saveValue(plugin, BLOCK_ENDS, blockEnds);
        saveValue(plugin, BLOCK_SIZES, blockSizes);
        saveValue(plugin, NAME2, name2);
        saveValue(plugin, SPAN, span);
        saveValue(plugin, COUNT, count);
        saveValue(plugin, OFFSET, offset);
        saveValue(plugin, FILE, file);
        saveValue(plugin, LOWER_LIMIT, lowerLimit);
        saveValue(plugin, DATA_RANGE, dataRange);
        SettingsUtils.store();
    }

    private void saveValue(SQLDataSourcePlugin plugin, String settingName, String mappedName) {
        if (mappedName != null) {
            String existingValue = SettingsUtils.getString(plugin, settingName);
            if (existingValue != null) {
                // If we have multiple columns corresponding to this field, store them as a comma-separated list.
                String[] existingValues = existingValue.split(",");
                for (int i = 0; i < existingValues.length; i++) {
                    if (existingValues[i].equals(mappedName)) {
                        // Already stored.  We're done.
                        return;
                    }
                }
                SettingsUtils.setString(plugin, settingName, existingValue + "," + mappedName);
            } else {
                SettingsUtils.setString(plugin, settingName, mappedName);
            }
        }
    }
}
