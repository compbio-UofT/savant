/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.file;

/**
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
        POINT_GENERIC(0xFACEBE09)
    ;

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
