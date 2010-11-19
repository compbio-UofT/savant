/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api;

import java.io.File;
import savant.file.FileType;
import savant.format.DataFormatter;

/**
 * Utilities for formatting Savant files
 *
 * @author mfiume
 */
public class FormatUtils {

    /**
     * Format a file into a Savant-readable file
     * @param inFile input file
     * @param outFile new output file (should end in .savant)
     * @param fileType type of the input file
     * @param isInputOneBased whether or not the input file is 1-based
     * @return whether or not the format was successful
     */
    public static boolean formatFile(File inFile, File outFile, FileType fileType, boolean isInputOneBased) {
        try {
            new DataFormatter(inFile, outFile, fileType, isInputOneBased).format();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
