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
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.samtools.util.AsciiLineReader;
import net.sf.samtools.util.BlockCompressedOutputStream;
import org.broad.igv.tools.sort.Parser;
import org.broad.igv.tools.sort.Sorter;
import org.broad.tabix.TabixWriter;
import org.broad.tabix.TabixWriter.Conf;

import savant.data.types.ColumnMapping;
import savant.file.FileType;


/**
 * Two-part formatter.  Bgzips the source (text) file, and creates the index and dictionary files.
 * @author tarkvara
 */
public class TabixFormatter extends SavantFileFormatter {
    /** Header-line to be written, definining the list of expected columns. */
    private String header;

    private ColumnMapping mapping;

    private Conf conf;

    private Map<String, String> dictionary = new LinkedHashMap<String, String>();

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
                header = ColumnMapping.INTERVAL_GENERIC_HEADER;
                break;
            case INTERVAL_BED:
                flags = TabixWriter.TI_FLAG_UCSC;
                header = ColumnMapping.BED_HEADER;
                break;
            case INTERVAL_BED1:
                header = "bin\t" + ColumnMapping.BED_HEADER;
                break;
            case INTERVAL_GFF:
                header = ColumnMapping.GFF_HEADER;
                break;
            case INTERVAL_KNOWNGENE:
                header = ColumnMapping.KNOWNGENE_HEADER;
                break;
            case INTERVAL_REFGENE:
                header = ColumnMapping.REFGENE_HEADER;
                break;
            case INTERVAL_PSL:
                flags = TabixWriter.TI_FLAG_UCSC;
                header = ColumnMapping.PSL_HEADER;
                break;
            case INTERVAL_VCF:
                flags = TabixWriter.TI_PRESET_VCF;
                header = ColumnMapping.VCF_HEADER;
                break;
        }
        mapping = ColumnMapping.inferMapping(header);
        conf = mapping.getTabixConf(flags);
    }

    @Override
    public void format() throws InterruptedException, IOException {
        try {
            // Sort the input file.
            setSubtaskStatus("Sorting input file...");
            File sortedFile = File.createTempFile("savant", ".sorted");
            new Sorter(inFile, sortedFile) {
                @Override
                public Parser getParser() {
                    return new Parser(mapping.chrom, mapping.start);
                }

                @Override
                public String writeHeader(AsciiLineReader reader, PrintWriter writer) {
                    boolean foundComments = false;
                    String nextLine = reader.readLine();
                    while (nextLine.startsWith("#")) {
                        writer.println(nextLine);
                        nextLine = reader.readLine();
                        foundComments = true;
                    }
                    if (!foundComments) {
                        writer.println("#" + header);
                    }
                    return nextLine;
                }
            }.run();
            setSubtaskProgress(25);

            // Compress the text file.
            setSubtaskStatus("Compressing text file...");
            AsciiLineReader input = new AsciiLineReader(new FileInputStream(sortedFile));
            PrintWriter output = new PrintWriter(new BlockCompressedOutputStream(outFile));
            String line;
            while ((line = input.readLine()) != null) {
                output.println(line);
                if ((mapping.name >= 0 || mapping.name2 >= 0) && !line.startsWith("#")) {
                    String[] columns = line.split("\\t");
                    String chrom = columns[mapping.chrom];
                    int start = Integer.valueOf(columns[mapping.start]);
                    int len = 1;
                    if (mapping.end >= 0) {
                        len = Integer.valueOf(columns[mapping.end]) - start;
                    }
                    String value = chrom + ":" + start + "+" + len;
                    if (mapping.name >= 0) {
                        String name = columns[mapping.name];
                        if (name != null && name.length() > 0) {
                            dictionary.put(name, value);
                        }
                    }
                    if (mapping.name2 >= 0) {
                        String name2 = columns[mapping.name2];
                        if (name2 != null && name2.length() > 0) {
                            dictionary.put(name2, value);
                        }
                    }
                }
            }
            output.close();
            input.close();
            setSubtaskProgress(50);

            setSubtaskStatus("Creating index file...");
            TabixWriter writer = new TabixWriter(outFile, conf);
            writer.createIndex(outFile);
            setSubtaskProgress(75);

            setSubtaskStatus("Creating dictionary file...");
            output = new PrintWriter(new BlockCompressedOutputStream(outFile.getAbsolutePath() + ".dict"));
            for (String k: dictionary.keySet()) {
                output.println(k + "\t" + dictionary.get(k));
            }
            output.close();
            setSubtaskProgress(100);
        } catch (Exception x) {
            LOG.error("Unable to create tabix index.", x);
            throw new IOException(x.getLocalizedMessage());
        }
    }
}
