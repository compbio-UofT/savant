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
 * Interface which is shared by all record types which represent variant data.  Currently,
 * the only one is VCFVariantRecord.
 *
 * @author tarkvara
 */
public interface VariantRecord extends PointRecord {
    /**
     * Type of variant represented by this record.
     *
     * @return the type (cannot be NONE)
     */
    public VariantType getVariantType();
    
    /**
     * Reference bases corresponding to this variant.
     *
     * @return a non-empty string containing reference bases
     */
    public String getRefBases();
    
    /**
     * Array alternate non-reference alleles called on at least one of the samples.
     *
     * @return an array containing the non-reference alleles
     */
    public String[] getAltAlleles();

    /**
     * Get the number of participants represented by this record (probably the same for
     * all records from a given DataSource).
     *
     * @return the number of participants in this record
     */
    public int getParticipantCount();

    /**
     * Given a participant, determine what type of variant (if any) they have for this record.
     * @param index the participant's index (0-based)
     * @return one or two variants for this participant
     */
    public VariantType[] getVariantsForParticipant(int index);

    /**
     * Given a participant, determine which of the alleles from <code>getAltAlleles()</code> they have for this record.
     * @param index the participant's index (0-based)
     * @return one or two 1-based indices of the participants allele within <code>getAltAlleles()</code>; 0 indicates reference
     */
    public int[] getAllelesForParticipant(int index);
    
    /**
     * Are the alleles in this record phased or not?
     */
    public boolean isPhased();
}
