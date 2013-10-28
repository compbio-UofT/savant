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
package savant.format;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.samtools.util.BlockCompressedOutputStream;
import org.broad.igv.tools.sort.Parser;
import org.broad.igv.tools.sort.Sorter;
import org.broad.tabix.TabixWriter;
import org.broad.tabix.TabixWriter.Conf;
import org.broad.tribble.readers.AsciiLineReader;

import savant.file.FileType;
import savant.util.ColumnMapping;


/**
 * Two-part formatter.  Bgzips the source (text) file, and creates the index and dictionary files.
 * @author tarkvara
 */
public class TabixFormatter extends SavantFileFormatter {
    /** Header-line to be written, defining the list of expected columns. */
    private String header;

    private ColumnMapping mapping;

    private Conf conf;

    private List<String> dictionary = new ArrayList<String>();
    
    private boolean needsTabHack = false;

    /** Keeps track of progress during sorting. */
    private long fileLength, bytesWritten, bytesRead;
    private int sortProgress;

    /**
     * Convert a text-based interval file into a usable format.
     *
     * @param inFile input text file
     * @param outFile output .gz file (index will append .tbi to the name)
     * @param needsTabs if true, file needs extra processing to convert whitespace to tabs
     */
    public TabixFormatter(File inFile, File outFile, FileType inputFileType, boolean needsTabs) throws IOException, SavantFileFormattingException {
        super(inFile, outFile);
        needsTabHack = needsTabs;

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
                flags = TabixWriter.TI_FLAG_UCSC;
                header = "bin\t" + ColumnMapping.BED_HEADER;
                break;
            case INTERVAL_GFF:
                header = ColumnMapping.GFF_HEADER;
                break;
            case INTERVAL_GTF:
                header = ColumnMapping.GTF_HEADER;
                break;
            case INTERVAL_KNOWNGENE:
                flags = TabixWriter.TI_FLAG_UCSC;
                header = ColumnMapping.KNOWNGENE_HEADER;
                break;
            case INTERVAL_REFGENE:
                flags = TabixWriter.TI_FLAG_UCSC;
                header = ColumnMapping.REFGENE_HEADER;
                break;
            case INTERVAL_PSL:
                flags = TabixWriter.TI_FLAG_UCSC;
                header = ColumnMapping.PSL_HEADER;
                break;
            case INTERVAL_VCF:
                flags = TabixWriter.TI_PRESET_VCF;
                readHeaderLine();
                break;
            case INTERVAL_UNKNOWN:
                readHeaderLine();
                break;
        }
        mapping = ColumnMapping.inferMapping(header, (flags & TabixWriter.TI_FLAG_UCSC) == 0);
        conf = mapping.getTabixConf(flags);
        if (mapping.chrom < 0 || mapping.start < 0) {
            throw new SavantFileFormattingException("Unable to determine columns.  Does this file have a proper header line?");
        }
    }

    /**
     * Look for a comment line which gives us the names of fields in the file.
     */
    private void readHeaderLine() throws IOException {
        // If we're lucky, the file starts with a comment line with the field-names in it.
        // That's what UCSC puts there, as does Savant.  In some files (e.g. VCF), this
        // magical comment line may be preceded by a ton of metadata comment lines.
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInput()));
        String line = reader.readLine();
        while (line.charAt(0) == '#') {
            header = line;
            line = reader.readLine();
        }
        reader.close();
        if (header != null) {
            while (header.length() > 0 && header.charAt(0) == '#') {
                header = header.substring(1);
            }
        }
    }

    @Override
    public void format() throws InterruptedException, IOException {
        try {
            // Sort the input file.
            setProgress(0.0, "Sorting input file...");
            File sortedFile = File.createTempFile("savant", ".sorted");
            new TabixSorter(inFile, sortedFile).run();

            // Compress the text file.
            setProgress(0.25, "Compressing text file...");
            AsciiLineReader input = new AsciiLineReader(new FileInputStream(sortedFile));
            PrintWriter output = new PrintWriter(new BlockCompressedOutputStream(outFile));
            String line;
            while ((line = input.readLine()) != null) {
                if (!line.isEmpty()) {
                    output.print(line + "\n");
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
                                dictionary.add(name + "\t" + value);
                            }
                        }
                        if (mapping.name2 >= 0) {
                            String name2 = columns[mapping.name2];
                            if (name2 != null && name2.length() > 0) {
                                dictionary.add(name2 + "\t" + value);
                            }
                        }
                    }
                }
            }
            output.close();
            input.close();
            setProgress(0.5, "Creating index file...");
            TabixWriter writer = new TabixWriter(outFile, conf);
            writer.createIndex(outFile);

            if (dictionary.size() > 0) {
                setProgress(0.75, "Creating dictionary file...");
                output = new PrintWriter(new BlockCompressedOutputStream(outFile.getAbsolutePath() + ".dict"));
                Collections.sort(dictionary);
                for (String l: dictionary) {
                    output.print(l + "\n");
                }
                output.close();
            }
            setProgress(1.0, null);
        } catch (Exception x) {
            throw new IOException(x);
        }
    }
    
    /**
     * Called by ProgressiveInputStream and ProgressiveWriter to update our progress while sorting.
     */
    private void updateSortProgress() {
        int newProg = (int)((bytesRead + bytesWritten) * 12.5 / fileLength);
        if (newProg != sortProgress) {
            sortProgress = newProg;
            setProgress(sortProgress * 0.01, null);
        }
    }

    /**
     * Used by TabixSorter and by readHeaderLine() so that they both have the same treatment of whitespace.
     */
    public InputStream getInput() throws FileNotFoundException {
        return needsTabHack ? new TabFixingInputStream(inFile) : new ProgressiveInputStream(inFile);
    }

    private class TabixSorter extends Sorter {
        TabixSorter(File inFile, File sortedFile) {
            super(inFile, sortedFile);
        }

        @Override
        public Parser getParser() throws IOException {
            return new Parser(mapping.chrom, mapping.start);
        }

        @Override
        public String writeHeader(AsciiLineReader reader, PrintWriter writer) throws IOException {
            String nextLine = reader.readLine();
            while (nextLine.startsWith("#")) {
                writer.print(nextLine + "\n");
                nextLine = reader.readLine();
            }
            // We may have to truncate the header to remove optional columns (usually for Bed).
            int numColumns = nextLine.split("\\t").length;
            String[] headerColumns = header.split("\\t");
            if (headerColumns.length > numColumns) {
                header = headerColumns[0];
                for (int i = 1; i < numColumns; i++) {
                    header += "\t" + headerColumns[i];
                }
            }
            writer.print("#" + header + "\n");
            // Readjust our mapping now that we know the actual number of columns.
            mapping = ColumnMapping.inferMapping(header, mapping.oneBased);
            return nextLine;
        }
        
        @Override
        public InputStream getInput() throws FileNotFoundException {
            return TabixFormatter.this.getInput();
        }
        
        @Override
        public PrintWriter getOutput() throws IOException {
            return new PrintWriter(new BufferedWriter(new ProgressiveWriter(outputFile)));
        }
    }

    /**
     * InputStream class which allows us to update progress during the sorting process.
     */
    private class ProgressiveInputStream extends FilterInputStream {

        private ProgressiveInputStream(File f) throws FileNotFoundException {
            super(new FileInputStream(f));
            fileLength = f.length();
            bytesRead = 0;
        }
        
        @Override
        public int read() throws IOException {
            bytesRead++;
            return super.read();
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            bytesRead += n;
            updateSortProgress();
            return n;
        }
    }
    
    /**
     * InputStream class which allows us to update progress during the sorting process.
     */
    private class TabFixingInputStream extends ProgressiveInputStream {
        private boolean trailingSpace;
        private byte[] tempBuf = new byte[0];

        private TabFixingInputStream(File f) throws FileNotFoundException {
            super(f);
        }
        
        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (tempBuf.length < b.length) {
                tempBuf = new byte[b.length];
            }
            int n = super.read(tempBuf, off, len);
            int j = off;
            for (int i = 0; i < n; i++) {
                byte c = tempBuf[i + off];
                if (c == ' ') {
                    if (!trailingSpace) {
                        // First space in a possible run.
                        trailingSpace = true;
                        b[j++] = '\t';
                    }
                } else {
                    trailingSpace = false;
                    b[j++] = c;
                }
            }
            LOG.info("read(" + b.length + ", " + off + ", " + len +") returning " + (j - off) + " instead of " + n);
            return j - off;
        }
    }
    
    /**
     * Writer class which allows us to update progress during the sorting process.
     */
    private class ProgressiveWriter extends FileWriter {
        ProgressiveWriter(File f) throws IOException {
            super(f);
            bytesWritten = 0;
        }
        
        void write(char c) throws IOException {
            bytesWritten++;
            super.write(c);
        }
        
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            bytesWritten += len;
            updateSortProgress();
            super.write(cbuf, off, len);
        }
        
        @Override
        public void write(String str, int off, int len) throws IOException {
            bytesWritten += len;
            updateSortProgress();
            super.write(str, off, len);
        }
    }
}
