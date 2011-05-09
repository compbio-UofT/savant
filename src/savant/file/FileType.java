/*
 *    Copyright 2010-2011 University of Toronto
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
package savant.file;


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
        INTERVAL_GENE(-1),
        INTERVAL_GENE1(-1);     // Gene file with a bin column inserted as column 0.  Used by UCSC for RefSeq genes.

    int magicNumber;

    FileType(int magicNumber) {
        this.magicNumber = magicNumber;   
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    static public FileType fromMagicNumber(int magicNumber) {
        FileType[] types = FileType.values();
        for (int i=0; i<types.length; i++) {
            if (types[i].getMagicNumber() == magicNumber) return types[i];
        }
        return null;
    }

}
