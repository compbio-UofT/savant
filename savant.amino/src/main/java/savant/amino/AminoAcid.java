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
package savant.amino;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple class for storing relevant information about each amino acid.
 *
 * @author tarkvara
 */
public enum AminoAcid {
    ALANINE("Alanine", "Ala", 'A', new Color(200, 200, 200)),
    ARGININE("Arginine", "Arg", 'R', new Color(20, 90, 255)),
    ASPARGINE("Asparagine", "Asn", 'N', new Color(0, 220, 220)),
    ASPARTIC_ACID("Aspartic acid", "Asp", 'D', new Color(230, 10, 10)),
    CYSTEINE("Cysteine", "Cys", 'C', new Color(230, 230, 0)),
    GLUTAMIC_ACID("Glutamic acid", "Glu", 'E', new Color(230, 10, 10)),
    GLUTAMINE("Glutamine", "Gln", 'Q', new Color(0, 220, 220)),
    GLYCINE("Glycine", "Gly", 'G', new Color(235, 235, 235)),
    HISTIDINE("Histidine", "His", 'H', new Color(130, 130, 210)),
    ISOLEUCINE("Isoleucine", "Ile", 'I', new Color(15, 130, 15)),
    LEUCINE("Leucine", "Leu", 'L', new Color(15, 130, 15)),
    LYSINE("Lysine", "Lys", 'K', new Color(20, 90, 255)),
    METHIONINE("Methionine", "Met", 'M', new Color(230, 230, 0)),
    PHENYLALANINE("Phenylalanine", "Phe", 'F', new Color(50, 50, 170)),
    PROLINE("Proline", "Pro", 'P', new Color(220, 150, 130)),
//    PYRROLYSINE("Pyrrolysine", "Pyl", 'O', null),
//    SELENOCYSTEINE("Selenocysteine", "Sec", 'U', null),
    SERINE("Serine", "Ser", 'S', new Color(250, 150, 0)),
    THREONINE("Threonine", "Thr", 'T', new Color(250, 150, 0)),
    TRYPTOPHAN("Tryptophan", "Trp", 'W', new Color(180, 90, 180)),
    TYROSINE("Tyrosine", "Tyr", 'Y', new Color(50, 50, 170)),
    VALINE("Valine", "Val", 'V', new Color(15, 130, 15)),
    STOP("Stop", "***", '\u2605', new Color(255, 0, 0));

    final String name;
    final String abbreviation;
    final char code;
    final Color color;

    AminoAcid(String n, String a, char c, Color col) {
        name = n;
        abbreviation = a;
        code = c;
        color = col;
    }

    private static final Map<String, AminoAcid> LOOKUPS;

    static {
        LOOKUPS = new HashMap<String, AminoAcid>();
        LOOKUPS.put("UUU", PHENYLALANINE);
        LOOKUPS.put("UUC", PHENYLALANINE);
        LOOKUPS.put("UUA", LEUCINE);
        LOOKUPS.put("UUG", LEUCINE);
        LOOKUPS.put("CUU", LEUCINE);
        LOOKUPS.put("CUC", LEUCINE);
        LOOKUPS.put("CUA", LEUCINE);
        LOOKUPS.put("CUG", LEUCINE);
        LOOKUPS.put("AUU", ISOLEUCINE);
        LOOKUPS.put("AUC", ISOLEUCINE);
        LOOKUPS.put("AUA", ISOLEUCINE);
        LOOKUPS.put("AUG", METHIONINE);
        LOOKUPS.put("GUU", VALINE);
        LOOKUPS.put("GUC", VALINE);
        LOOKUPS.put("GUA", VALINE);
        LOOKUPS.put("GUG", VALINE);
        LOOKUPS.put("UCU", SERINE);
        LOOKUPS.put("UCC", SERINE);
        LOOKUPS.put("UCA", SERINE);
        LOOKUPS.put("UCG", SERINE);
        LOOKUPS.put("CCU", PROLINE);
        LOOKUPS.put("CCC", PROLINE);
        LOOKUPS.put("CCA", PROLINE);
        LOOKUPS.put("CCG", PROLINE);
        LOOKUPS.put("ACU", THREONINE);
        LOOKUPS.put("ACC", THREONINE);
        LOOKUPS.put("ACA", THREONINE);
        LOOKUPS.put("ACG", THREONINE);
        LOOKUPS.put("GCU", ALANINE);
        LOOKUPS.put("GCC", ALANINE);
        LOOKUPS.put("GCA", ALANINE);
        LOOKUPS.put("GCG", ALANINE);
        LOOKUPS.put("UAU", TYROSINE);
        LOOKUPS.put("UAC", TYROSINE);
        LOOKUPS.put("UAA", STOP);
        LOOKUPS.put("UAG", STOP);       // PYRROLYSINE
        LOOKUPS.put("CAU", HISTIDINE);
        LOOKUPS.put("CAC", HISTIDINE);
        LOOKUPS.put("CAA", GLUTAMINE);
        LOOKUPS.put("CAG", GLUTAMINE);
        LOOKUPS.put("AAU", ASPARGINE);
        LOOKUPS.put("AAC", ASPARGINE);
        LOOKUPS.put("AAA", LYSINE);
        LOOKUPS.put("AAG", LYSINE);
        LOOKUPS.put("GAU", ASPARTIC_ACID);
        LOOKUPS.put("GAC", ASPARTIC_ACID);
        LOOKUPS.put("GAA", GLUTAMIC_ACID);
        LOOKUPS.put("GAG", GLUTAMIC_ACID);
        LOOKUPS.put("UGU", CYSTEINE);
        LOOKUPS.put("UGC", CYSTEINE);
        LOOKUPS.put("UGA", STOP);       // SELENOCYSTEINE
        LOOKUPS.put("UGG", TRYPTOPHAN);
        LOOKUPS.put("CGU", ARGININE);
        LOOKUPS.put("CGC", ARGININE);
        LOOKUPS.put("CGA", ARGININE);
        LOOKUPS.put("CGG", ARGININE);
        LOOKUPS.put("AGU", SERINE);
        LOOKUPS.put("AGC", SERINE);
        LOOKUPS.put("AGA", ARGININE);
        LOOKUPS.put("AGG", ARGININE);
        LOOKUPS.put("GGU", GLYCINE);
        LOOKUPS.put("GGC", GLYCINE);
        LOOKUPS.put("GGA", GLYCINE);
        LOOKUPS.put("GGG", GLYCINE);
    }

    /**
     * Given a sequence of 3 bases, return the amino acid encoded.
     */
    public static AminoAcid lookup(char b0, char b1, char b2) {
        String s = new String(new char[] { b0, b1, b2 });
        return LOOKUPS.get(s.replace('T', 'U'));
    }
};

