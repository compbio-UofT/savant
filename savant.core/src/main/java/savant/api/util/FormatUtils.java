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
package savant.api.util;

import java.io.File;

import savant.file.FileType;
import savant.format.SavantFileFormatter;


/**
 * Utilities for formatting Savant files.  For more flexibility, it is worth considering
 * the <code>FormatTool</code> command-line utility.
 *
 * @author mfiume
 */
public class FormatUtils {

    /**
     * Format a file into a Savant-readable file.
     *
     * @param inFile input file
     * @param outFile new output file
     * @param fileType type of the input file
     * @param isInputOneBased whether or not the input file is 1-based (ignored)
     * @return whether or not the format was successful
     */
    public static boolean formatFile(File inFile, File outFile, FileType fileType, boolean isInputOneBased) {
        try {
            SavantFileFormatter.getFormatter(inFile, outFile, fileType).format();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
