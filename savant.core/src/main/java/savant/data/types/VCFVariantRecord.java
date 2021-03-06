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
package savant.data.types;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.util.ColumnMapping;

/**
 * Record type which holds variant data stored in a VCF record.
 *
 * @author tarkvara
 */
public class VCFVariantRecord extends TabixIntervalRecord implements VariantRecord {
    private static final Log LOG = LogFactory.getLog(VCFVariantRecord.class);
    private static final int NAME_COLUMN = 2;
    private static final int REF_COLUMN = 3;
    private static final int ALT_COLUMN = 4;
    private static final int FIRST_PARTICIPANT_COLUMN = 9;
    private static final byte MISSING = -1;
    
    private final String reference;
    private final String name;
    private final String refBases;
    private final String[] altBases;
    private final byte[] participants0;
    private final byte[] participants1;
    private boolean phased = true;

    /**
     * Constructor not be be called directly, but rather through TabixIntervalRecord.valueOf.
     */
    protected VCFVariantRecord(String line, ColumnMapping mapping) {
        super(line, mapping);
        
        // Storing all the string information is grossly inefficient, so we just parse what we
        // need and set the values to null.
        reference = super.getReference().intern();
        name = values[NAME_COLUMN].equals(".") ? null : values[NAME_COLUMN];    // VCF uses "." for missing value
        refBases = values[REF_COLUMN].intern();
        
        String altValue = values[ALT_COLUMN];
        if (altValue.startsWith("<")) {
            altBases = new String[] { altValue.intern() };
        } else {
            altBases = altValue.split(",");
            for (int i = 0; i < altBases.length; i++) {
                altBases[i] = altBases[i].intern();
            }
        }

        if (values.length > FIRST_PARTICIPANT_COLUMN) {
            participants0 = new byte[values.length - FIRST_PARTICIPANT_COLUMN];
            participants1 = new byte[values.length - FIRST_PARTICIPANT_COLUMN];
            for (int i = 0; i < participants0.length; i++) {
                String info = values[FIRST_PARTICIPANT_COLUMN + i];
                participants0[i] = MISSING;
                participants1[i] = MISSING;
                if (!info.equals(".")) {
                    int colonPos = info.indexOf(':');
                    if (colonPos >= 0) {
                        info = info.substring(0, colonPos);
                    }
                    
                    phased &= info.indexOf('/') < 0;
                    String[] alleleIndices = info.split("[|/]");
                    if (!alleleIndices[0].equals(".")) {
                        participants0[i] =  Byte.parseByte(alleleIndices[0]);
                    }
                    if (alleleIndices.length > 1 && !alleleIndices[1].equals(".")) {
                        participants1[i] = Byte.parseByte(alleleIndices[1]);
                    }
                }
            }
        } else {
            // A defective VCF with no participants.
            participants0 = new byte[0];
            participants1 = new byte[0];
        }

        values = null;
    }

    /**
     * For debug purposes, provides a succinct summary of the variant.
     */
    @Override
    public String toString() {
        return getVariantType().toString() + "@" + getInterval().getStart();
    }

    @Override
    public int compareTo(Object t) {
        VariantRecord that = (VariantRecord)t;
        return new CompareToBuilder().append(getReference(), that.getReference()).
                                      append(getPosition(), that.getPosition()).
                                      append(getRefBases(), that.getRefBases()).
                                      append(getAltAlleles(), that.getAltAlleles()).toComparison();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof VariantRecord) {
            VariantRecord that = (VariantRecord)t;
            return new EqualsBuilder().append(getReference(), that.getReference()).
                                       append(getPosition(), that.getPosition()).
                                       append(getRefBases(), that.getRefBases()).
                                       append(getAltAlleles(), that.getAltAlleles()).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getReference()).
                                     append(getPosition()).
                                     append(getRefBases()).
                                     append(getAltAlleles()).toHashCode();
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public int getPosition() {
        return interval.getStart();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRefBases() {
        return refBases;
    }

    @Override
    public String[] getAltAlleles() {
        return altBases;
    }

    @Override
    public int getParticipantCount() {
        return participants0.length;
    }

    /**
     * Get the variant for the given individual.
     *
     * @param index between 0 and getParticipantCount() - 1
     * @return the variant for this participant (possibly NONE)
     */
    @Override
    public VariantType[] getVariantsForParticipant(int index) {
        int[] alleles = getAllelesForParticipant(index);
        if (alleles == null) {
            // Missing value for this participant.
            return null;
        } else if (alleles.length == 1) {
            // Either haploid or homozygous.
            return new VariantType[] { getVariantType(alleles[0]) };
        } else {
            return new VariantType[] { getVariantType(alleles[0]), getVariantType(alleles[1]) };
        }
    }

    /**
     * Get the indices of the allele for the given individual.
     *
     * @param index between 0 and getParticipantCount() - 1
     * @return one or two alleles for this participant (0 for reference)
     */
    @Override
    public int[] getAllelesForParticipant(int index) {
        int allele0 = participants0[index];
        if (allele0 == MISSING) {
            return null;
        }
        int allele1 = participants1[index];
        if (allele0 == allele1 || allele1 == MISSING) {
            // Either haploid or homozygous.
            return new int[] { allele0 };
        }
        // Heterozygous.
        return new int[] { allele0, allele1 };
    }
    
    /**
     * Return the type of this record without reference to any participant.  When there
     * are multiple ALT values, we return the first one.
     *
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * @return the type of variant represented by this record
     */
    @Override
    public VariantType getVariantType() {
        return getVariantType(altBases[0]);
    }

    @Override
    public boolean isPhased() {
        return phased;
    }

    /**
     * Get the variant type for the given allele.
     * @param index 0-based allele index where 0 = reference
     */
    private VariantType getVariantType(int index) {
        return index > 0 ? getVariantType(altBases[index - 1]) : VariantType.NONE;
    }

    /**
     * TODO: Interpret ALT strings which contain descriptive information rather than bases.
     * @return the type of variant represented by this record
     */
    private VariantType getVariantType(String alt) {
        if (refBases.length() == 1 && alt.length() == 1) {
            switch (alt.charAt(0)) {
                case 'A':
                    return VariantType.SNP_A;
                case 'C':
                    return VariantType.SNP_C;
                case 'G':
                    return VariantType.SNP_G;
                case 'T':
                    return VariantType.SNP_T;
                default:
                    LOG.info("Unrecognised base " + alt + " in VCFVariantRecord.");
                    return VariantType.OTHER;
            }
        }
        switch (alt.charAt(0)) {
            case '<':
                // Funky informative string.  We only recognize <INS and <DEL.
                if (alt.startsWith("<INS")) {
                    return VariantType.INSERTION;
                } else if (alt.startsWith("<DEL")) {
                    return VariantType.DELETION;
                } else {
                    return VariantType.OTHER;
                }
            case 'I':
                // VCF 3.3 insertion.
                return VariantType.INSERTION;
            case 'D':
                // VCF 3.3 deletion.
                return VariantType.DELETION;
            default:
                if (alt.length() > refBases.length()) {
                    return VariantType.INSERTION;
                } else if (alt.length() < refBases.length()) {
                    return VariantType.DELETION;
                } else {
                    // When merging a SNP and a deletion, vcf-merge produces some SNPs with long ALT strings.
                    int numDiffs = 0;
                    VariantType result = VariantType.NONE;
                    for (int i = 0; i < alt.length() && numDiffs <= 1; i++) {
                        if (alt.charAt(i) != refBases.charAt(i)) {
                            numDiffs++;
                            result = VariantType.fromChar(alt.charAt(i));
                        }
                    }
                    if (numDiffs <= 1) {
                        return result;
                    }
                }
                return VariantType.OTHER;
        }
    }
}
