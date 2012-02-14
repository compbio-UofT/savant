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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.tools.PreprocessingException;
import org.broad.igv.tools.Preprocessor;

import savant.util.MiscUtils;


/**
 * Special case of the TDF formatter, which uses the IGVTools "count" command to generate
 * a coverage file.
 *
 * @author tarkvara
 */
public class BAMToCoverage extends TDFFormatter {
    private static final int DEFAULT_WINDOW_SIZE = 25;
    private static final int DEFAULT_EXT_FACTOR = 0;
    private static final int DEFAULT_STRAND_OPTION = -1;

    public BAMToCoverage(File inFile, FormatProgressListener listener) {
        super(inFile, new File(inFile.getAbsolutePath() + ".cov.tdf"), listener);
    }

    @Override
    public void format() throws InterruptedException, IOException {
        Genome genome = getTDFGenome();
        setSubtaskStatus("Generating TDF file...");
        Preprocessor pp = new Preprocessor(outFile, genome, MEAN, 1000000, new TDFProgressMonitor());
        try {
            pp.count(inFile.getAbsolutePath(), DEFAULT_WINDOW_SIZE, DEFAULT_EXT_FACTOR, DEFAULT_ZOOMS, null, "", null);
            pp.finish();
        } catch (PreprocessingException x) {
            throw new IOException(x);
        }
    }

    @Override
    protected LinkedHashMap<String, Integer> inferChromosomes() throws InterruptedException, IOException {

        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

        inFileReader = openInputFile();

        setSubtaskStatus("Processing input file...");
        incrementOverallProgress();

        SAMFileReader samFileReader = new SAMFileReader(inFile, determineIndexFile());

        try {
            SAMFileHeader fileHeader = samFileReader.getFileHeader();
            SAMSequenceDictionary dictionary = fileHeader.getSequenceDictionary();
            List<SAMSequenceRecord> sequences = dictionary.getSequences();
            for (SAMSequenceRecord sequence : sequences) {
                result.put(MiscUtils.homogenizeSequence(sequence.getSequenceName()), sequence.getSequenceLength());
            }
        } finally {
            if (samFileReader != null) {
                samFileReader.close();
            }
        }

        return result;
    }

    private File determineIndexFile() throws IOException {

        // determine default track name from filename
        //int lastSlashIndex = inFile.lastIndexOf(System.getProperty("file.separator"));
        //String name = inFile.substring(lastSlashIndex+1, inFile.length());

        // infer index file name from track filename
        String path = inFile.getAbsolutePath();
        String pathWithoutExtension;
        int lastIndex = path.lastIndexOf(".bam");
        if (lastIndex == -1) {
            throw new IOException("BAM files should end with the \".bam\" file extension.");
        } else {
            pathWithoutExtension = path.substring(0, lastIndex);
        }
        //String pathWithoutExtension = inFile.substring(0, inFile.lastIndexOf(".bam"));
        File f = new File(path + ".bai");
        if (f.exists()) {
            return f;
        } else {
            f = new File(pathWithoutExtension + ".bai");
            if (f.exists()) {
                return f;
            }
        }
        throw new FileNotFoundException("Unable to find index file for " + inFile + ".");
    }
}
