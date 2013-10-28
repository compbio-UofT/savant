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


/**
 * Comparator class for sorting references in a human-friendly fashion.
 * <ol>
 * <li>Chromosomes with numeric suffixes ("chr1" .. "chr22")</li>
 * <li>Chromosomes with roman numeral suffixes ("chrI" .. "chrXXII")</li>
 * <li>Chromosomes with alphabetic suffixes ("chrX" .. "chrY")</li>
 * <li>Mitochondria ("chrM" or "MT")</li>
 * <li>Other material (contigs and chrUn entries), basically anything with an underscore in the name</li>
 * </ol>
 *
 * @author tarkvara
 */
public class ReferenceComparator implements Comparator<String> {
    @Override
    public int compare(String ref1, String ref2) {
        if (ref1.contains("_")) {
            if (ref2.contains("_")) {
                // Two non-chromosomal references.  Fall back on string comparator.
                return ref1.compareTo(ref2);
            } else {
                // First one is non-chromosomal, second one isn't.
                return 1;
            }
        }
        if (ref2.contains("_")) {
            // Second one is non-chromosomal, first one isn't.
            return -1;
        }

        // Put mitochondria at the end.
        if (ref1.equals("chrM") || ref1.equals("MT")) {
            return 1;
        }
        if (ref2.equals("chrM") || ref2.equals("MT")) {
            return -1;
        }

        // Find the first digit
        int pos1 = findFirstArabicDigit(ref1);
        int pos2 = findFirstArabicDigit(ref2);

        if (pos1 == pos2) {
            if (pos1 != -1) {
                // Two strings have prefixes of the same length.
                int prefixComp = ref1.substring(0, pos1).compareTo(ref2.substring(0, pos2));
                if (prefixComp != 0) {
                    return prefixComp;
                }
                return Integer.parseInt(ref1.substring(pos1)) - Integer.parseInt(ref2.substring(pos2));
            } else {
                // No arabic numerals found.  Try roman numerals.
                pos1 = findFirstRomanDigit(ref1);
                if (pos1 != -1) {
                    pos2 = findFirstRomanDigit(ref2);
                    if (pos1 == pos2) {
                        // Found two valid roman numerals.  First compare the prefixes.
                        int prefixComp = ref1.substring(0, pos1).compareTo(ref2.substring(0, pos2));
                        if (prefixComp != 0) {
                            return prefixComp;
                        }
                        int result = getSortableRoman(ref1.substring(pos1)).compareTo(getSortableRoman(ref2.substring(pos2)));
                        return result;
                    }
                }
            }
            // No roman or arabic numerals found in either string.
            return ref1.compareTo(ref2);
        } else if (pos1 == -1) {
            // ref1 is alphabetic, but ref2 is numeric.
            return 1;
        } else if (pos2 == -1) {
            // ref1 is numeric, but ref2 is alphabetic.
            return -1;
        }
        return pos1 - pos2;
    }

    private int findFirstArabicDigit(String s) {
        int n = s.length() - 1;
        if (!Character.isDigit(s.charAt(n))) {
            return -1;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (!Character.isDigit(s.charAt(i))) {
                return i + 1;
            }
        }
        // Entire contig name happens to be numeric.
        return 0;
    }

    /**
     * Roman numerals up to 30 sort alphabetically, with two exceptions.
     * @param roman a roman numeral
     * @return a roman numeral modified to sort properly.
     */
    private String getSortableRoman(String roman) {
        if (roman.equals("IX")) {
            return "VIIII";
        } else if (roman.equals("XIX")) {
            return "XVIIII";
        }
        return roman;
    }

    private boolean isRoman(char c) {
        return c == 'I' || c == 'V' || c == 'X';
    }

    private int findFirstRomanDigit(String s) {
        int n = s.length() - 1;
        if (!isRoman(s.charAt(n))) {
            return -1;
        }
        for (int i = n - 1; i > 0; i--) {
            if (!isRoman(s.charAt(i))) {
                return i + 1;
            }
        }
        return 0;
    }
}
