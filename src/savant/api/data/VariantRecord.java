/*
 *    Copyright 2011-2012 University of Toronto
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
}
