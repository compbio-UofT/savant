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
import java.io.PrintWriter;
import net.sf.samtools.util.AsciiLineReader;
import org.broad.igv.tools.sort.Parser;
import org.broad.igv.tools.sort.Sorter;
import org.broad.tabix.TabixWriter;
import org.broad.tabix.TabixWriter.Conf;
import net.sf.samtools.util.BlockCompressedOutputStream;

import savant.file.FileType;
import savant.util.FileUtils;

/**
 * Two-part formatter.  Bgzips the source (text) file, and creates the index file.
 * @author tarkvara
 */
public class TabixFormatter extends SavantFileFormatter {
    /** Tabix configuration. */
    private Conf conf;

    /** Header-line to be written, definining the list of expected columns. */
    private String header;

    /**
     * Convert a text-based interval file into a usable format.
     *
     * @param inFile input text file
     * @param outFile output .gz file (index will append .tbi to the name)
     */
    public TabixFormatter(File inFile, File outFile, FileType inputFileType) {
        super(inFile, outFile, FileType.TABIX);
        int flags = 0;
        switch (inputFileType) {
            case INTERVAL_GENERIC:
                conf = TabixWriter.BED_CONF;
                header = "#chrom\tstart\tend\tname";
                break;
            case INTERVAL_BED:
                conf = TabixWriter.BED_CONF;
                header = "#chrom\tstart\tend\tname\tscore\tstrand\tthickStart\tthickEnd\titemRgb\tblockCount\tblockSizes\tblockStarts";
                break;
            case INTERVAL_GFF:
                conf = TabixWriter.GFF_CONF;
                header = "#seqname\tsource\tfeature\tstart\tend\tscore\tstrand\tframe\tgroup";
                break;
            case INTERVAL_GENE:
                conf = new Conf(0, 2, 4, 5, '#', 0);
                header = "## Savant 1.4.5 gene\n#name\tchrom\tstrand\ttxStart\ttxEnd\tcdsStart\tcdsEnd\texonCount\texonStarts\texonEnds\tid\tname2\tcdsStartStat\tcdsEndStat\texonFrames";
                break;
            case INTERVAL_REFGENE:
                conf = new Conf(0, 3, 5, 6, '#', 0);
                header = "## Savant 1.4.5 refGene\n#bin\tname\tchrom\tstrand\ttxStart\ttxEnd\tcdsStart\tcdsEnd\texonCount\texonStarts\texonEnds\tid\tname2\tcdsStartStat\tcdsEndStat\texonFrames";
                break;
            case INTERVAL_PSL:
                conf = TabixWriter.PSLTBL_CONF;
                header = "## Savant 1.4.5 PSL\n#matches\tmisMatches\trepMatches\tnCount\tqNumInsert\tqBaseInsert\ttNumInsert\ttBaseInsert\tstrand\tqName\tqSize\tqStart\tqEnd\ttName\ttSize\ttStart\ttEnd\tblockCount\tblockSizes\tqStarts\ttStarts";
                break;
            case INTERVAL_VCF:
                conf = TabixWriter.VCF_CONF;
                header = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT";
                break;
        }
    }

    @Override
    public void format() throws InterruptedException, IOException, SavantFileFormattingException {
        try {
            // Sort the input file.
            setSubtaskStatus("Sorting input file...");
            File sortedFile = File.createTempFile("savant", "sorted");
            new Sorter(inFile, sortedFile) {
                @Override
                public Parser getParser() {
                    return new Parser(conf.chrColumn - 1, conf.startColumn - 1);
                }

                @Override
                public String writeHeader(AsciiLineReader reader, PrintWriter writer) {
                    boolean foundComments = false;
                    String nextLine = reader.readLine();
                    while (nextLine.startsWith(String.valueOf(conf.commentChar))) {
                        writer.println(nextLine);
                        nextLine = reader.readLine();
                        foundComments = true;
                    }
                    if (!foundComments) {
                        writer.println(header);
                    }
                    return nextLine;
                }
            }.run();
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
