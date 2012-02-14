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

package savant.api.util;

import java.io.File;
import savant.file.FileType;
import savant.format.DataFormatter;
import savant.format.FormatProgressListener;


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
     * @param isInputOneBased whether or not the input file is 1-based
     * @return whether or not the format was successful
     */
    public static boolean formatFile(File inFile, File outFile, FileType fileType, boolean isInputOneBased) {
        try {
            new DataFormatter(inFile, outFile, fileType, isInputOneBased).format(new FormatProgressListener() {

                @Override
                public void taskProgressUpdate(Integer progress, String status) {}

                @Override
                public void incrementOverallProgress() {}
            });
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
