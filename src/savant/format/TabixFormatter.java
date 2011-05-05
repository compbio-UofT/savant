/*
 *    Copyright 2011 University of Toronto
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

package savant.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.broad.tabix.TabixWriter;
import org.broad.tabix.TabixWriter.Conf;
import net.sf.samtools.util.BlockCompressedOutputStream;
import org.broad.igv.tools.sort.Sorter;

import savant.file.FileType;
import savant.util.FileUtils;

/**
 * Two-part formatter.  Bgzips the source (text) file, and creates the index file.
 * @author tarkvara
 */
public class TabixFormatter extends SavantFileFormatter {
    /** Tabix configuration. */
    private Conf conf;

    /**
     * Convert a text-based interval file into a usable format.
     *
     * @param inFile input text file
     * @param outFile output .gz file (index will append .tbi to the name)
     */
    public TabixFormatter(File inFile, File outFile, boolean isOneBased, FileType inputType, int refColumn, int beginColumn, int endColumn, char commentChar) {
        super(inFile, outFile, FileType.TABIX);
        int flags = 0;
        if (!isOneBased) {
            flags |= TabixWriter.TI_FLAG_UCSC;
        }
        // Our column numbers are zero-based; tabix' are one-based.
        this.conf = new Conf(flags, refColumn + 1, beginColumn + 1, endColumn + 1, commentChar, 0);
    }

    @Override
    public void format() throws InterruptedException, IOException, SavantFileFormattingException {
        try {
            // Sort the input file.
            setSubtaskStatus("Sorting input file...");
            File sortedFile = File.createTempFile("savant", "sorted");
            Sorter.getSorter(inFile, sortedFile).run();
            setSubtaskProgress(33);

            // Compress the text file.
            setSubtaskStatus("Compressing text file...");
            FileUtils.copyStream(new FileInputStream(sortedFile), new BlockCompressedOutputStream(outFile));
            setSubtaskProgress(67);
        
            setSubtaskStatus("Creating index file...");
            TabixWriter writer = new TabixWriter(outFile, conf);
            writer.createIndex(outFile);
            setSubtaskProgress(100);
        } catch (Exception x) {
            LOG.error("Unable to create tabix index.", x);
            throw new SavantFileFormattingException(x.getLocalizedMessage());
        }
    }
}
