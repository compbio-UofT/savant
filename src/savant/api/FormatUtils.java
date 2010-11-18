/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api;

import savant.file.FileType;
import savant.format.DataFormatter;

/**
 * Utilities for formatting Savant files
 * @author mfiume
 */
public class FormatUtils {

    /**
     * Format a file into a Savant-readable file
     * @param inPath Path to input file
     * @param outPath Path to new output file (should end in .savant)
     * @param fileType Type of the input file
     * @param isInputOneBased Whether or not the input file is 1-based
     * @return Whether or not the format was successful
     */
    public static boolean formatFile(String inPath, String outPath, FileType fileType, boolean isInputOneBased) {
        try {
            (new DataFormatter(inPath, outPath, fileType, isInputOneBased)).format();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }


}
