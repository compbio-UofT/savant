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
package savant.file;

import savant.api.data.DataFormat;


/**
 * Enumeration defining the file-types recognised by Savant.  For Savant's native
 * file-types, the magic number associated with the file-type is store in the file's header.
 *
 * @author mfiume
 */
public enum FileType {
    // DO NOT CHANGE THE ORDER OF THINGS HERE!!
    INTERVAL_BAM(0xFACEBE01),
    //SAVANT(0xFACEBE02),
        INTERVAL_GENERIC(0xFACEBE03),
        INTERVAL_BED(0xFACEBE04),
        INTERVAL_GFF(0xFACEBE05),
        CONTINUOUS_WIG(0xFACEBE06),
        CONTINUOUS_GENERIC(0xFACEBE07),
        SEQUENCE_FASTA(0xFACEBE08),
        POINT_GENERIC(0xFACEBE09),
        TABIX(-1),
        INTERVAL_BIGBED(-1),
        CONTINUOUS_BIGWIG(-1),
        CONTINUOUS_TDF(-1),
        INTERVAL_BED1(-1),      // BED file with a bin column inserted as column 0.
        INTERVAL_PSL(-1),
        INTERVAL_VCF(-1),
        INTERVAL_KNOWNGENE(-1),
        INTERVAL_REFGENE(-1),     // Gene file with a bin column inserted as column 0.  Used by UCSC for RefSeq genes.
        INTERVAL_UNKNOWN(-1),     // Some unknown interval format.  Columns must be identified by comment-line at start of file.
        INTERVAL_GTF(-1);     

    int magicNumber;

    FileType(int magicNumber) {
        this.magicNumber = magicNumber;   
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public DataFormat toDataFormat() {
        switch (this) {
            case INTERVAL_BAM:
                return DataFormat.ALIGNMENT;
            case INTERVAL_GENERIC:
                return DataFormat.GENERIC_INTERVAL;
            case INTERVAL_BED:
            case INTERVAL_GFF:
            case INTERVAL_GTF:
            case TABIX:
            case INTERVAL_BIGBED:
            case INTERVAL_BED1:      // BED file with a bin column inserted as column 0.
            case INTERVAL_KNOWNGENE:
            case INTERVAL_REFGENE:     // Gene file with a bin column inserted as column 0.  Used by UCSC for RefSeq genes.
            case INTERVAL_UNKNOWN:     // Some unknown interval format.  Columns must be identified by comment-line at start of file.
                return DataFormat.RICH_INTERVAL;
            case INTERVAL_PSL:
                // TODO: PSL files really contain alignments, not intervals.
                return DataFormat.RICH_INTERVAL;
            case CONTINUOUS_WIG:
            case CONTINUOUS_GENERIC:
            case CONTINUOUS_BIGWIG:
            case CONTINUOUS_TDF:
                return DataFormat.CONTINUOUS;
            case SEQUENCE_FASTA:
                return DataFormat.SEQUENCE;
            case POINT_GENERIC:
                return DataFormat.POINT;
            case INTERVAL_VCF:
                return DataFormat.VARIANT;
        }
        return null;
    }

    static public FileType fromMagicNumber(int magicNumber) {
        FileType[] types = FileType.values();
        for (int i=0; i<types.length; i++) {
            if (types[i].getMagicNumber() == magicNumber) return types[i];
        }
        return null;
    }
}
