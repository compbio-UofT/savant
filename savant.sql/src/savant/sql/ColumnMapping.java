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
import savant.file.DataFormat;


/**
 * Keeps track of the mapping between Savant's own internal fields and SQL column names.
 *
 * @author tarkvara
 */
public class ColumnMapping implements SQLConstants {
    public final DataFormat format;
    public final String chrom;
    public final String start;
    public final String end;
    public final String value;
    public final String name;
    public final String score;
    public final String strand;
    public final String thickStart;
    public final String thickEnd;
    public final String itemRGB;
    public final String blockStarts;
    public final String blockEnds;
    public final String blockSizes;

    private ColumnMapping(DataFormat format, String chrom, String start, String end, String value, String name, String score, String strand, String thickStart, String thickEnd, String itemRGB, String blockStarts, String blockEnds, String blockSizes) {
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
        this.blockStarts = NO_COLUMN.equals(blockStarts) ? null : blockStarts;
        this.blockEnds = NO_COLUMN.equals(blockEnds) ? null : blockEnds;
        this.blockSizes = NO_COLUMN.equals(blockSizes) ? null : blockSizes;
    }

    /**
     * Constructor used to map GenericContinuous formats.
     */
    public static ColumnMapping getContinuousMapping(String chrom, String start, String end, String value) {
        return new ColumnMapping(DataFormat.CONTINUOUS_GENERIC, chrom, start, end, value, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor used to map GenericInterval formats.
     */
    public static ColumnMapping getIntervalMapping(String chrom, String start, String end, String name) {
        return new ColumnMapping(DataFormat.INTERVAL_GENERIC, chrom, start, end, null, name, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor used to map BEDInterval formats.
     */
    public static ColumnMapping getBEDMapping(String chrom, String start, String end, String name, String score, String strand, String thickStart, String thickEnd, String itemRGB, String blockStarts, String blockEnds, String blockSizes) {
        return new ColumnMapping(DataFormat.INTERVAL_BED, chrom, start, end, null, name, score, strand, thickStart, thickEnd, itemRGB, blockStarts, blockEnds, blockSizes);
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
    public static ColumnMapping getSavedMapping(SQLDataSourcePlugin plugin, Column[] columns) {
        // CHROM, START, and END are common to all mappings.
        String chrom = findColumn(plugin, CHROM, columns);
        String start = findColumn(plugin, START, columns);
        String end = findColumn(plugin, END, columns);
        if (chrom != null && start != null && end != null) {
            String name = findColumn(plugin, NAME, columns);
            if (name != null) {
                // We have a name field, but beyond that the division between INTERVAL_GENERIC and INTERVAL_BED is a little vague.
                // Arbitrarily, we will decide that if there is a SCORE or STRAND column, we'll call it INTERVAL_BED.
                String score = findColumn(plugin, SCORE, columns);
                String strand = findColumn(plugin, STRAND, columns);
                if (score != null || strand != null) {
                    return getBEDMapping(chrom, start, end, name, score, strand,
                                         findColumn(plugin, THICK_START, columns), findColumn(plugin, THICK_END, columns),
                                         findColumn(plugin, ITEM_RGB, columns),
                                         findColumn(plugin, BLOCK_STARTS, columns), findColumn(plugin, BLOCK_ENDS, columns), findColumn(plugin, BLOCK_ENDS, columns));
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
        return new ColumnMapping(null, chrom, start, end, null, null, null, null, null, null, null, null, null, null);
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
        saveValue(plugin, BLOCK_STARTS, blockStarts);
        saveValue(plugin, BLOCK_ENDS, blockEnds);
        saveValue(plugin, BLOCK_SIZES, blockSizes);
        SettingsUtils.store();
    }

    private void saveValue(SQLDataSourcePlugin plugin, String settingName, String mappedName) {
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
